/**
 * Copyright 2015 Trustees of Indiana University. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE TRUSTEES OF INDIANA UNIVERSITY ``AS IS'' AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE TRUSTEES OF INDIANA UNIVERSITY OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of the Trustees of Indiana University.
 */
package ORG.oclc.os.SRW.Lucene;

import edu.indiana.dlib.search.highlighter.HighlighterRequestInfo;
import edu.indiana.dlib.search.indexing.FieldConfiguration;
import gov.loc.www.zing.srw.ExtraDataType;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.axis.message.MessageElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;

import ORG.oclc.os.SRW.ExtendedQueryResult;
import ORG.oclc.os.SRW.QueryResult;
import ORG.oclc.os.SRW.Record;
import ORG.oclc.os.SRW.RecordIterator;
import ORG.oclc.os.SRW.SRWDiagnostic;
import ORG.oclc.os.SRW.shared.FacetExtensionHandler;
import ORG.oclc.os.SRW.shared.HighlighterExtensionHandler;

/**
 * 
 * LuceneSearchResult
 *
 * A wrapper around a set of (Lucene-based) results that makes them 
 * suitable for use with the SRW server. 
 *
 * @author Ryan Scherle
 */

public class LuceneSearchResult extends QueryResult implements ExtendedQueryResult {
    static Log log=LogFactory.getLog(LuceneSearchResult.class);
                
    private Hits hits;
    private String luceneQueryString;
    private Query query;
    private Analyzer analyzer;
    private Properties dbProperties;
    
    /**
     * The list of fields parsed from the higlighter request.  This will
     * be null or empty if no highlighting was requested. 
     */
    private List<HighlighterRequestInfo> highlighterFields;
    
    /**
     * The Lucene class that will do the highlighting.  Only instantiated 
     * if "highligherFields" has been set.
     */
    private Highlighter highlighter;

    private FieldConfiguration fieldConfig;
    
    
    /**
     * A mapping from index field names to a mapping from index
     * field values to the number of such values that occur in
     * this search result.  This is a cache used by the method
     * {@code getRepresentedValues()}.
     */
    private Map<String, Map<String, Integer>> indexToValueToCountMap;
        
    public LuceneSearchResult(Hits hits, Query query, Analyzer analyzer, String luceneQueryString, Properties dbProperties, List<HighlighterRequestInfo> highlighterRequestInfo, FieldConfiguration fieldConfig) {
        this.hits = hits;
        this.luceneQueryString = luceneQueryString;
        this.query = query;
        this.analyzer = analyzer;
        this.dbProperties = dbProperties;
        this.indexToValueToCountMap = new HashMap<String, Map<String, Integer>>();
        this.fieldConfig = fieldConfig;
        this.highlighterFields = highlighterRequestInfo;
        if (this.highlighterFields != null && !this.highlighterFields.isEmpty()) {
            try {
                this.highlighter = new Highlighter(new SimpleHTMLFormatter("<strong>", "</strong>"), new QueryScorer(query));
            } catch (Throwable t) {
                // sometimes this constructor fails with otherwise reasonable
                // queries... in such cases we should disable highlighting, but
                // otherwise function properly
                log.error("Unable to instantiate Highlighter!", t);
            }
        } else {
            this.highlighter = null;
        }
    }
    
    Hits getHits() {
        return this.hits;
    }
    
    Query getQuery() {
        return this.query;
    }
    
    String getLuceneQueryString() {
        return this.luceneQueryString;
    }
    
    public long getNumberOfRecords() {
        if (hits == null) {
                return 0;
        }
        return hits.length();
    }
    
    public RecordIterator newRecordIterator(long startPoint, int numRecs, String schemaName) {
        RecordIterator recIt = new GSearchIterator(hits, startPoint, numRecs, schemaName);
        return recIt;
    }

    private class GSearchIterator implements RecordIterator {

        private int currentDoc;
        private int lastDoc;
        private String schemaName;
        
        public GSearchIterator(Hits hits, long startPoint, int numRecs, String schemaID) {
                log.debug("Initializing iterator: " + 
                                hits.length() + ", " +
                                startPoint + ", " +
                                numRecs + ", " +
                                schemaID);
                // Lucene hits are 0-based, while SRW is 1-based, so subtract 1 from everything
                currentDoc = (int)startPoint - 1;
                lastDoc = Math.min(hits.length(), currentDoc + numRecs) - 1;
        
                // Find the short schemaName, based on the full schemaID passed in. It would be
                // simpler for SRWDatabaseImpl to pass in the schemaName.
                for (Map.Entry<Object, Object> entry : dbProperties.entrySet()) {
                        if(entry.getValue().toString().equals(schemaID)) {
                                String possibleSchemaName = entry.getKey().toString();
                        int dotIDindex = possibleSchemaName.indexOf(".identifier");
                        if(dotIDindex > 0) {
                                schemaName = possibleSchemaName.substring(0, dotIDindex);
                        }
                        }
                }
                //log.debug("schema name is " + schemaName);
        }
        
        public void close() {
                // do nothing
        }

        /** 
         * Get the next available hit from the hit list, and transform it into an SRW Record,
         * using the format specified in the properties file as a guide.
         * @throws SRWDiagnostic 
         */
        public Record nextRecord() throws NoSuchElementException {
                if(currentDoc > lastDoc) {
                        throw new NoSuchElementException("requested item " + currentDoc + ", but the last document in this iterator is " + lastDoc);
                }

                
                StringBuffer sb=new StringBuffer();
                String recordStart = dbProperties.getProperty(schemaName + ".recordStart");
                if(recordStart != null) {
                        sb.append(recordStart);
                }
                 
                try {
                        Document doc = hits.doc(currentDoc);
                        currentDoc++;

                        // If a recordItem is specified for this schema, pull the referenced 
                        // field out of Lucene, and assume it is a fully-formed response 
                        // record. Otherwise, build a response record using the recordElement properties.
                        String recordItemField = dbProperties.getProperty(schemaName + ".recordItem");  
                        if (recordItemField != null) {
                        	String fieldValue = doc.get(recordItemField);
                        	if(fieldValue == null || fieldValue.length() == 0) {
                        		log.warn("Field " + recordItemField + " doesn't exist in this hit!");
                        		// return an empty XML file that can't be transformed
                        		return new Record("<iudlAdmin></iudlAdmin>", "UNSUPPORTED_SCHEMA");
                        	} else {
                        		//parse out any XML declaration that may appear in the field
                        		if(fieldValue.startsWith("<?")) {
                        			int declEnd = fieldValue.indexOf("?>");
                        			fieldValue = fieldValue.substring(declEnd + 2);
                        		}
                        		sb.append(fieldValue.trim());
                        	}
                        } else {
                        	int elementIndex = 1;
                        	String elementInfo = dbProperties.getProperty(schemaName + ".recordElement." + elementIndex);
                        	while(elementInfo != null) {
                        		StringTokenizer st = new StringTokenizer(elementInfo, " =");
                        		String tagName = st.nextToken();
                        		String luceneField = st.nextToken();
                        		if (doc.getFields(luceneField) != null) {
                        			for (Field field : doc.getFields(luceneField)) {
                        				String fieldValue = field.stringValue();
                        				sb.append("<" + tagName + ">");
                        				// Note: the following line might include
                        				//       invalid characters
                        				//sb.append(fieldValue);
                        				sb.append(fieldValue.replaceAll("&(?!amp;)", "&amp;"));
                        				sb.append("</" + tagName + ">");
                        			}
                        		}
                        		elementIndex++;
                        		elementInfo = dbProperties.getProperty(schemaName + ".recordElement." + elementIndex);
                        	}
                        }
                } catch(Exception e) {
                	log.error("Unable to read data from search hit #" + currentDoc, e);
                }

                String recordEnd = dbProperties.getProperty(schemaName + ".recordEnd");
                if(recordEnd != null) {
                	sb.append(recordEnd);
                }
                String sourceSchemaID = dbProperties.getProperty(schemaName + ".sourceSchemaId");
                if (sourceSchemaID != null) {
                	return new Record(sb.toString(), sourceSchemaID);
                } else {
                	String returnSchemaID = dbProperties.getProperty(schemaName + ".identifier");
                	return new Record(sb.toString(), returnSchemaID);
                }
        }
        
        public boolean hasNext() {
                if(currentDoc > lastDoc) {
                        return false;
                } else {
                        return true;
                }
        }
        
        public Object next()  throws NoSuchElementException {
        	return nextRecord();
        }
        
        public void remove() throws UnsupportedOperationException {

            throw new UnsupportedOperationException();

        }
    }

    public ExtraDataType getExtraDataForRecord(int index) {
        if (this.highlighter != null) {
            try {
                Document hit = this.hits.doc(index);
                org.w3c.dom.Document extraDataDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().getDOMImplementation().createDocument(HighlighterExtensionHandler.SRU_HIGHLIGHTER_SEARCH_EXTENSION_SCHEMA_V1, "highlighterInformation", null);
                for (HighlighterRequestInfo hri : this.highlighterFields) {
                    for (String fieldName : fieldConfig.resolveFieldName(hri.getFieldId())) {
                        String fieldNameStored = fieldConfig.getFieldNameStored(fieldName);
                        log.debug("Considering highlighting: " + fieldName);
                        Field[] fields = hit.getFields(fieldNameStored);
                        if (fields != null) {
                            for (Field field : fields) {
                                log.debug("Looking for matches in " + field.name());
                                this.highlighter.setFragmentScorer(new QueryScorer(this.query, fieldName));
                                for (String fragment : this.highlighter.getBestFragments(this.analyzer, field.name(), field.stringValue(), 3)) {
                                    Element match = extraDataDoc.createElementNS(HighlighterExtensionHandler.SRU_HIGHLIGHTER_SEARCH_EXTENSION_SCHEMA_V1, "match");
                                    match.setAttribute("field", hri.getFieldId());
                                    match.appendChild(extraDataDoc.createTextNode(stripInvalidCharacters(fragment)));
                                    extraDataDoc.getDocumentElement().appendChild(match);
                                }
                            }
                        }
                    }
                }
                MessageElement[] me = new MessageElement[1];
                me[0] = new MessageElement(extraDataDoc.getDocumentElement());
                ExtraDataType extraData = new ExtraDataType();
                extraData.set_any(me);
                return extraData;
            } catch (CorruptIndexException ex) {
                log.error("Unable to get highlighting information!", ex);
                // fall through without highlighting
            } catch (IOException ex) {
                log.error("Unable to get highlighting information!", ex);
                // fall through without highlighting
            } catch (DOMException ex) {
                log.error("Unable to get highlighting information!", ex);
                // fall through without highlighting
            } catch (ParserConfigurationException ex) {
                log.error("Unable to get highlighting information!", ex);
                // fall through without highlighting
            } catch (Exception ex) {
                log.error("Unable to get highlighting information!", ex);
            }
        }
        return null;
    } 
    
    /** 
     * Removes any characters not valid in XML.  Note, this does not
     * remove values that need to be escaped, just values for which
     * there is no XML escape.  
     */
    public static String stripInvalidCharacters(String input) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < input.length(); i ++) {
            char c = input.charAt(i);
            if (c >= 0x20) {
                sb.append(c);
            } else if (c == 0x9) {
                sb.append(c);
            } else if (c == 0xA) {
                sb.append(c);
            } else if (c == 0xD) {
                sb.append(c);
            } else {
                sb.append(' ');
            }
        }
        return sb.toString();
    }
}

