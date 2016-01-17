/*
   Copyright 2006 OCLC Online Computer Library Center, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
 */
/*
 * SRWDatabaseImpl.java
 *
 * Created on August 5, 2003, 4:17 PM
 */

package ORG.oclc.os.SRW;

import gov.loc.www.zing.srw.DiagnosticsType;
import gov.loc.www.zing.srw.ExtraDataType;
import gov.loc.www.zing.srw.RecordType;
import gov.loc.www.zing.srw.RecordsType;
import gov.loc.www.zing.srw.ScanRequestType;
import gov.loc.www.zing.srw.ScanResponseType;
import gov.loc.www.zing.srw.SearchRetrieveRequestType;
import gov.loc.www.zing.srw.SearchRetrieveResponseType;
import gov.loc.www.zing.srw.StringOrXmlFragment;
import gov.loc.www.zing.srw.diagnostic.DiagnosticType;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.servlet.ServletException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import org.apache.axis.message.MessageElement;
import org.apache.axis.message.Text;
import org.apache.axis.MessageContext;
import org.apache.axis.types.NonNegativeInteger;
import org.apache.axis.types.PositiveInteger;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author  levan
 */
public abstract class SRWDatabaseImpl extends SRWDatabase {
    static Log log=LogFactory.getLog(SRWDatabaseImpl.class);
    SRWDatabaseImpl db;
    Hashtable     sortTools=new Hashtable();

    public static final int MAX_RESULT_SETS = 1000;
    
    public abstract String getExtraResponseData(QueryResult result,
                        SearchRetrieveRequestType request);
    public abstract QueryResult getQueryResult(String query,
                        SearchRetrieveRequestType request)
                        throws InstantiationException;


    public SearchRetrieveResponseType doRequest(
      SearchRetrieveRequestType request) throws ServletException {
        SearchRetrieveResponseType response=new SearchRetrieveResponseType();
        response.setNumberOfRecords(new NonNegativeInteger("0"));
//        try {
            MessageContext msgContext=MessageContext.getCurrentContext();

            String recordPacking=request.getRecordPacking();
            if(recordPacking==null) {
                if(msgContext!=null && msgContext.getProperty("sru")!=null)
                    recordPacking="xml"; // default for sru
                else
                    recordPacking="string"; // default for srw
            }

            QueryResult result;
            String query=request.getQuery(), resultSetID=getResultSetId(query);
            try{
                log.info("query:\n"+Utilities.byteArrayToString(query.getBytes("UTF-8")));
            }catch(Exception e){}

            if(resultSetID!=null) { // got a cached result
                log.info("resultSetID="+resultSetID);
                result=(QueryResult)oldResultSets.get(resultSetID);
                if(result==null)
                    return diagnostic(SRWDiagnostic.ResultSetDoesNotExist,
                        resultSetID, response);
            }
            else { // Evaluate the query.
                try {
                    result = getQueryResult(query, request);
                }
                catch(InstantiationException e) {
                    log.error(e, e);
                    return diagnostic(SRWDiagnostic.GeneralSystemError,
                        e.getMessage(), response);
                }
            }

            long postingsCount=result.getNumberOfRecords();
            log.info("'" + query + "'==> " + postingsCount);
            response.setNumberOfRecords(new NonNegativeInteger(
                Long.toString(postingsCount)));

            int resultSetTTL=defaultResultSetTTL;
            NonNegativeInteger nni=request.getResultSetTTL();
            if(nni!=null)
                resultSetTTL=nni.intValue();
            result.setResultSetIdleTime(resultSetTTL);
            if(postingsCount>0) {  // we don't mess with records otherwise
            	if(resultSetID==null) {
            		resultSetID=makeResultSetID();
            	}
            	if (timers.size() < MAX_RESULT_SETS && resultSetTTL > 0) {
            		log.info("keeping resultSet '"+resultSetID+"' for "+resultSetTTL+
            				" seconds (" + ((timers.size() * 100) / + MAX_RESULT_SETS) + "% of resultSet cache used)");
            		oldResultSets.put(resultSetID, result);
            		timers.put(resultSetID, new Long(System.currentTimeMillis()+(resultSetTTL*1000)));
            	} else {
            		if (resultSetTTL == 0) {
            			log.info("discarding resultSet '" + resultSetID + "' because it was to be retained for 0 ms.");
            		} else {
            			log.info("discarding resultSet '"+resultSetID+"' because " + timers.size() + " result sets are already cached!");
            			resultSetTTL = 0;
            		}
            	}
            	response.setResultSetId(resultSetID);
            	if (resultSetTTL > 0) {
            		response.setResultSetIdleTime(new PositiveInteger(Integer.toString(resultSetTTL)));
            	}

                int numRecs=defaultNumRecs;
                NonNegativeInteger maxRecs=request.getMaximumRecords();
                if(maxRecs!=null)
                    numRecs=(int)java.lang.Math.min(maxRecs.longValue(), maximumRecords);

                long startPoint=1;
                PositiveInteger startRec=request.getStartRecord();
                if(startRec!=null)
                    startPoint=startRec.longValue();
                if(startPoint>postingsCount)
                    diagnostic(SRWDiagnostic.FirstRecordPositionOutOfRange,
                            null, response);

                if((startPoint-1+numRecs)>postingsCount)
                    numRecs=(int)(postingsCount-(startPoint-1));
                if(numRecs==0)
                    response.setNextRecordPosition(new PositiveInteger("1"));
                else if(numRecs>0) { // render some records into SGML

                    if(!recordPacking.equals("xml") &&
                      !recordPacking.equals("string")) {
                        return diagnostic(SRWDiagnostic.UnsupportedRecordPacking, recordPacking, response);
                    }

                    String schemaName=request.getRecordSchema();
                    if(schemaName==null)
                        schemaName="default";
                    String schemaID=getSchemaID(schemaName),
                           sortKeys=request.getSortKeys();
                    log.info("schemaName="+schemaName+", schemaID="+schemaID+
                        ", sortKeys="+sortKeys);
//                    if(sortKeys!=null && sortKeys.length()>0) { // do we need to sort them first?
//                        QueryResult sortedResult=result.getSortedResult(sortKeys);
//                        if(sortedResult==null) { // sigh, we've got some sorting to do
//                            log.info("sorting resultSet");
//                            boolean       ascending=true;
//                            SortTool sortTool=null;
//                            String   sortKey;
//                            if(schemaName==null)
//                                schemaName="default";
//                            log.info("recordSchema="+schemaName);
//                            Object handler=transformers.get(schemaName);
//                            if(handler==null) {
//                                log.error("no handler for schema "+schemaName);
//                                if(log.isInfoEnabled()) {
//                                    for(Enumeration enum2=transformers.keys();
//                                      enum2.hasMoreElements();)
//                                        log.info("handler name="+(String)enum2.nextElement());
//                                }
//                                return diagnostic(SRWDiagnostics.UnknownSchemaForRetrieval,
//                                    schemaName, response);
//                            }
//                            StringTokenizer keysTokenizer=new StringTokenizer(sortKeys);
//                            //while(keysTokenizer.hasMoreTokens()) {
//                            // just one key for now
//                                sortKey=keysTokenizer.nextToken();
//                                sortTool=new PearsSortTool(sortKey, transformers);
//                            //}
//                            String sortSchema=(String)nameSpaces.get(sortTool.prefix);
//                            if(sortSchema==null)
//                                sortSchema="";
//                            sortTool.setSchema(sortSchema);
//                            sortTool.makeSortElementExtractor();
//                            BerString        doc;
//                            DataDir          recDir;
//                            DocumentIterator list=(DocumentIterator)result.getDocumentIdList();
//                            int              listEntry;
//                            String           stringRecord;
//                            entries=new SortEntry[postings];
//                            for(int i=0; i<postings; i++) {
//                                listEntry=list.nextInt();
//                                log.debug("listEntry="+listEntry);
//                                doc=(BerString)pdb.getDocument(listEntry);
//                                recDir=new DataDir(doc);
//                                if(sortTool.dataType.equals("text"))
//                                    entries[i]=new SortEntry(sortTool.extract(recDir), listEntry);
//                                else {
//                                    try {
//                                        entries[i]=new SortEntry(Integer.parseInt(sortTool.extract(recDir)), listEntry);
//                                    }
//                                    catch(java.lang.NumberFormatException e) {
//                                        entries[i]=new SortEntry(0, listEntry);
//                                    }
//                                }
//                                if(entries[i].getKey()==null) { // missing value code
//                                    if(sortTool.missingValue.equals("abort"))
//                                        return diagnostic(SRWDiagnostics.SortEndedDueToMissingValue,
//                                            null, response);
//                                    else if(sortTool.missingValue.equals("highValue"))
//                                        entries[i]=new SortEntry("\ufffffe\ufffffe\ufffffe\ufffffe\ufffffe", listEntry);
//                                    else if(sortTool.missingValue.equals("lowValue"))
//                                        entries[i]=new SortEntry("\u000000", listEntry);
//                                    else { // omit
//                                        i--;
//                                        postings--;
//                                    }
//                                }
//                                if(log.isDebugEnabled())
//                                    log.debug("entries["+i+"]="+entries[i]);
//                            }
//                            Arrays.sort(entries);
//                            sortedResultSets.put(resultSetID+"/"+sortKeys, entries);
//                            sortTools.put(sortKeys, sortTool);
//                        }
//                        else {
//                            log.info("reusing old sorted resultSet");
//                        }
//                        result=sortedResult;
//                    }  // if(sortKeys!=null && sortKeys.length()>0)

                    // render some records
                    RecordIterator list=null;
                    try {
                        log.info("making RecordIterator, startPoint="+startPoint+", schemaID="+schemaID);
                        list=result.recordIterator(startPoint, numRecs, schemaID);
                    }
                    catch(InstantiationException e) {
                        diagnostic(SRWDiagnostic.GeneralSystemError,
                            e.getMessage(), response);
                    }
                    RecordsType records=new RecordsType();

                    records.setRecord(new RecordType[numRecs]);
                    Document               domDoc;
                    DocumentBuilder        db=null;
                    DocumentBuilderFactory dbf=null;
                    int                    i, listEntry=-1;
                    MessageElement         elems[];
                    Record                 rec;
                    RecordType             rt;
                    String                 recStr="";
                    StringOrXmlFragment    frag;
                    if(recordPacking.equals("xml")) {
                        dbf=DocumentBuilderFactory.newInstance();
                        dbf.setNamespaceAware(true);
                        try {
                            db=dbf.newDocumentBuilder();
                        }
                        catch(ParserConfigurationException e) {
                            log.error(e, e);
                        }
                    }

                    /**
                     * One at a time, retrieve and display the requested documents.
                     */
                    log.info("trying to get "+numRecs+
                        " records starting with record "+startPoint+
                        " from a set of "+postingsCount+" records");
                    for(i=0; list!=null && i<numRecs; i++) {
                        try {
                            rec=list.nextRecord();
                            log.debug("rec="+rec);
                            recStr=Utilities.hex07Encode(rec.getRecord());
                            if(schemaID!=null && !rec.getRecordSchemaID().equals(schemaID)) {
                                log.debug("transforming to "+schemaID);
                                // They must have specified a transformer
                                Transformer t=(Transformer)transformers.get(schemaID);
                                if(t==null) {
                                    log.info("record not available in schema "+schemaID);
                                    diagnostic(SRWDiagnostic.RecordNotAvailableInThisSchema,
                                        schemaID, response);
                                    continue;
                                }
                                StringWriter toRec=new StringWriter();
                                StreamSource fromRec=new StreamSource(new StringReader(recStr));
                                t.transform(fromRec, new StreamResult(toRec));
                                recStr=toRec.toString();
//                                if(log.isDebugEnabled())
                                    try {
                                        log.info("Transformed XML:\n"+Utilities.byteArrayToString(
                                            recStr.getBytes("UTF8")));
                                    }
                                    catch(UnsupportedEncodingException e) {} // can't happen
                            }
                            rt=new RecordType();
                            rt.setRecordPacking(recordPacking);
                            frag=new StringOrXmlFragment();
                            elems=new MessageElement[1];
                            frag.set_any(elems);
                            if(recordPacking.equals("xml")) {
                                domDoc=db.parse(new InputSource(new StringReader(recStr)));
                                Element el=domDoc.getDocumentElement();
                                log.debug("got the DocumentElement");
                                elems[0]=new MessageElement(el);
                                log.debug("put the domDoc into elems[0]");
                            }
                            else { // string
                                Text t=new Text(recStr);
                                elems[0]=new MessageElement(t);
                            }
                            rt.setRecordData(frag);

                            if(schemaID!=null)
                                rt.setRecordSchema(schemaID);
                            else
                                rt.setRecordSchema(schemaName);
                            rt.setRecordPosition(new PositiveInteger(
                                Long.toString(startPoint+i)));
                            if (result instanceof ExtendedQueryResult) {
                                rt.setExtraRecordData(((ExtendedQueryResult) result).getExtraDataForRecord(rt.getRecordPosition().intValue() - 1));
                            }
                            records.setRecord(i, rt);
                        }
                        catch(IOException e) {
                            diagnostic(SRWDiagnostic.RecordTemporarilyUnavailable,
                                null, response);
                            log.error("error transforming document "+(i+1));
                            log.error(e, e);
                            try {
                                log.error("Bad record:\n"+Utilities.byteArrayToString(
                                        recStr.getBytes("UTF8")));
                            }
                            catch(UnsupportedEncodingException e2) {} // can't happen
                        }
                        catch (NoSuchElementException e) {
                            diagnostic(SRWDiagnostic.RecordTemporarilyUnavailable,
                                null, response);
                            log.error("error getting document "+(i+1)+", postings="+postingsCount);
                            log.error(e, e);
                            break;
                        }
                        catch(SAXException e) {
                            diagnostic(SRWDiagnostic.RecordTemporarilyUnavailable,
                                null, response);
                            log.error("error transforming document "+(i+1));
                            log.error(e, e);
                            try {
                                log.error("Bad record:\n"+Utilities.byteArrayToString(
                                        recStr.getBytes("UTF8")));
                            }
                            catch(UnsupportedEncodingException e2) {} // can't happen
                        }
                        catch (TransformerException e) {
                            diagnostic(SRWDiagnostic.RecordTemporarilyUnavailable,
                                null, response);
                            log.error("error transforming document "+(i+1));
                            log.error(e, e);
                            try {
                                log.error("Bad record:\n"+Utilities.byteArrayToString(
                                        recStr.getBytes("UTF8")));
                            }
                            catch(UnsupportedEncodingException e2) {} // can't happen
                        }

                        response.setRecords(records);
                    }
                    if(startPoint+i<=postingsCount)
                        response.setNextRecordPosition(new PositiveInteger(
                            Long.toString(startPoint+i)));
                } // else if(numRecs>0)
            } // if(postingsCount>0)

            String extraResponseData=getExtraResponseData(result, request);
            if(extraResponseData!=null)
                response.setExtraResponseData(makeExtraDataType(extraResponseData, recordPacking));

            Vector diagnostics=result.getDiagnostics();
            if(diagnostics!=null && diagnostics.size()>0) {
                DiagnosticType diagArray[]=new DiagnosticType[diagnostics.size()];
                diagnostics.toArray(diagArray);
                response.setDiagnostics(new DiagnosticsType(diagArray));
            }
            log.debug("exit doRequest");
            return response;
//        }
//        catch(Exception e) {
//            //log.error(e);
//            log.error(e, e);
//            throw new ServletException(e.getMessage());
//        }
    }


    public static String getResultSetId(String query) {
        StringTokenizer st=new StringTokenizer(query, " =\"");
        int num=st.countTokens();
        if(num<2 || num>3)
            return null;
        String index=st.nextToken();
        if(!index.equals("cql.resultSetId"))
            return null;
        String relationOrResultSetId=st.nextToken();
        if(relationOrResultSetId.equals("exact")) {
            if(num<3)
                return null;
            return st.nextToken();
        }
        if(num==2)
            return relationOrResultSetId;
        return null;
    }


    /**
     *  This class assumes that schema information was provided in the .props
     *  file for this database.  This method provides a way for extending
     *  classes to provide the schemaName to schemaID mapping themselves.
     */
    public String getSchemaID(String schemaName) {
       return (String)schemas.get(schemaName);
    }

    
    public static ExtraDataType makeExtraDataType(String extraData, String recordPacking) {
        ExtraDataType edt=new ExtraDataType();
        MessageElement elems[]=new MessageElement[1];
        edt.set_any(elems);
        if(recordPacking.equals("xml")) {
            Document domDoc;
            DocumentBuilderFactory dbf=
                DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            try {
                DocumentBuilder db=dbf.newDocumentBuilder();
                StringReader sr=new StringReader(extraData);
                domDoc=db.parse(new InputSource(sr));
                sr.close();
                Element el=domDoc.getDocumentElement();
                elems[0]=new MessageElement(el);
                domDoc=null;
            }
            catch(IOException e) {
                log.error(e, e);
            }
            catch(ParserConfigurationException e) {
                log.error(e, e);
            }
            catch(SAXException e) {
                log.error(e, e);
                try {
                    log.error("Bad ExtraResponseData:\n"+
                        Utilities.byteArrayToString(
                        extraData.getBytes("UTF8")));
                }
                catch(UnsupportedEncodingException e2) {} // can't happen
            }
        }
        else { // string
            Text t=new Text(extraData);
            elems[0]=new MessageElement(t);
        }
        return edt;
    }

    public static Hashtable parseElements(ExtraDataType extraData) {
        Hashtable extraDataTable=new Hashtable();
        if(extraData!=null) {
            MessageElement[] elems=extraData.get_any();
            NameValuePair    nvp;
            String element, extraRequestData=elems[0].toString();
            ElementParser ep=new ElementParser(extraRequestData);
            log.info("extraRequestData="+extraRequestData);
            while(ep.hasMoreElements()) {
                nvp=(NameValuePair)ep.nextElement();
                extraDataTable.put(nvp.getName(), nvp.getValue());
                log.info(nvp);
            }
        }
        return extraDataTable;
    }

    public void useConfigInfo(String configInfo) {
        log.info("configInfo="+configInfo);
        ElementParser ep=new ElementParser(configInfo);
        NameValuePair nvp, configInfoPair=(NameValuePair)ep.nextElement();
        String attribute, attributes, type;
        StringTokenizer st;
        ep=new ElementParser(configInfoPair.getValue());
        while(ep.hasMoreElements()) {
            nvp=(NameValuePair)ep.nextElement();
            log.info("nvp="+nvp);
            if(nvp.getName().equals("default")) {
                attributes=ep.getAttributes();
                st=new StringTokenizer(attributes, " =\"");
                type=null;
                while(st.hasMoreTokens()) {
                    attribute=st.nextToken();
                    if(attribute.equals("type")) {
                        type=st.nextToken();
                    }
                }
                if(type!=null) {
                    log.info("type="+type+", value="+nvp.getValue());
                    if(type.equals("retrieveSchema"))
                        schemas.put("default", nvp.getValue());
                    else if(type.equals("maximumRecords"))
                        maximumRecords=Integer.parseInt(nvp.getValue());
                    else if(type.equals("numberOfRecords"))
                        defaultNumRecs=Integer.parseInt(nvp.getValue());
                }
            }
        }
    }
    public void useSchemaInfo(String schemaInfo) {
        log.info("schemaInfo="+schemaInfo);
        ElementParser ep=new ElementParser(schemaInfo);
        NameValuePair nvp, schemaInfoPair=(NameValuePair)ep.nextElement();
        String attribute, attributes, schemaID, schemaName;
        StringTokenizer st;
        ep=new ElementParser(schemaInfoPair.getValue());
        while(ep.hasMoreElements()) {
            nvp=(NameValuePair)ep.nextElement();
            log.info("nvp="+nvp);
            if(nvp.getName().equals("schema")) {
                attributes=ep.getAttributes();
                st=new StringTokenizer(attributes, " =\"");
                schemaID=schemaName=null;
                while(st.hasMoreTokens()) {
                    attribute=st.nextToken();
                    if(attribute.equals("name")) {
                        schemaName=st.nextToken();
                    }
                    else if(attribute.equals("identifier")) {
                        schemaID=st.nextToken();
                    }
                }
                if(schemaID!=null && schemaName!=null) {
                    log.info("adding schema: "+schemaName+", schemaID="+schemaID);
                    schemas.put(schemaName, schemaID);
                    schemas.put(schemaID, schemaID);
                }
            }
        }
    }
}
