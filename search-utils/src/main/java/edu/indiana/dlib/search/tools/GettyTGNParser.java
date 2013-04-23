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
package edu.indiana.dlib.search.tools;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Pattern;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.ValidationException;
import org.exolab.castor.xml.XMLContext;

import edu.indiana.dlib.search.analyzers.CaseInsensitiveSRUSupportAnalyzer;
import edu.indiana.dlib.xml.zthes.Relation;
import edu.indiana.dlib.xml.zthes.Term;
import edu.indiana.dlib.xml.zthes.TermNote;
import edu.indiana.dlib.xml.zthes.Zthes;

/**
 * <p>
 *   A simple command-line tool that parses a directory of getty
 *   TGN XML files and creates a Lucene index suitable for 
 *   advanced SRW searching.
 * </p>
 * <p>
 *   The current implementation indexes every field listed in the
 *   flat format with the name of the XML element.  Furthermore
 *   an ".exact" and ".facet" form will be indexed as well and a
 *   simple "zthes" field containing a valid ZThes XML fragment
 *   that represents the whole record.
 * </p>
 */
public class GettyTGNParser {

    private static final Pattern FILENAME_PATTERN = Pattern.compile("(?i)^tgn(\\d+)\\.xml");
    
    private static final int STOP_AFTER = Integer.MAX_VALUE;
    
    public static void main(String[] args) throws Exception {
        ShutdownThread shutdown = new ShutdownThread();
        Runtime.getRuntime().addShutdownHook(shutdown);
        
        if (args.length != 2) {
            System.out.println("TGN Importation Tool:");
            System.out.println();
            System.out.println("Required parameter: [tgn files directory] [lucene index directory]");
            System.out.println("  This directory must contain a TGN_CHARS.XML, TGN(X).xml.");
            shutdown.notifyDone();
            return;
        }
        File xmlDirectory = new File(args[0]);
        TGNCharacterConverter converter = new TGNCharacterConverter(new File(xmlDirectory, "TGN_CHARS.xml"));
        
        FileOutputStream fos = new FileOutputStream("log.txt", true);
        BufferedWriter log = new BufferedWriter(new OutputStreamWriter(fos));
        
        Analyzer analyzer = new CaseInsensitiveSRUSupportAnalyzer();

        if (new File(args[1]).exists() && new File(args[1]).list().length > 0) {
            System.err.println("The target index directory is not empty!");
            shutdown.notifyDone();
            return;
        }
        
        
        IndexWriter writer = new IndexWriter(args[1], analyzer);
        try {
            // pass through each XML file updating the index for each term
            int ptcount = 0;
            int ndcount = 0;
            int rtcount = 0;
            int lastNotice = 0;
            Map<String, List<String>> fieldToValuesMap = new HashMap<String, List<String>>();
            Map<String, List<String>> unauthorizedFieldToValuesMap = new HashMap<String, List<String>>();
            for (File potentialFile : xmlDirectory.listFiles()) {
                if (FILENAME_PATTERN.matcher(potentialFile.getName()).matches() && !shutdown.shouldStop()) {
                    System.out.println("Parsing " + potentialFile.getName());
                    FileInputStream is = new FileInputStream(potentialFile);
                    
                    XMLInputFactory factory = XMLInputFactory.newInstance();
                    
                    XMLStreamReader parser = factory.createXMLStreamReader(is);
                    Stack<String> currentPath = new Stack<String>();
                    ArrayList<String> unauthorizedTermNames = new ArrayList<String>();
                    Term term = null;
                    Term unauthorizedTerm = null;
                    boolean relatedTo = false;
                    StringBuffer sb = null;
                    while (parser.hasNext()) {
                        int eventType = parser.next();
                        switch (eventType) {
                            case XMLStreamReader.CHARACTERS:
                                if (sb != null) {
                                    sb.append(converter.convertCharacters(parser.getText()));
                                }
                                break;
                            case XMLStreamReader.START_ELEMENT:
                                currentPath.push(parser.getLocalName());
                                if (currentPath.peek().equalsIgnoreCase("Subject")) {
                                    ptcount ++;
                                    term = new Term();
                                    fieldToValuesMap.clear();
                                    term.setTermId(parser.getAttributeValue(0));
                                    term.setTermType("PT");
                                    addField(fieldToValuesMap, "id", parser.getAttributeValue(0));
                                    addField(fieldToValuesMap, "termType", "PT");
                                } else if (isPath(currentPath, "Non-Preferred_Term")) {
                                    ndcount ++;
                                    unauthorizedTerm = new Term();
                                    unauthorizedFieldToValuesMap.clear();
                                    unauthorizedTerm.setTermType("ND");
                                    addField(unauthorizedFieldToValuesMap, "termType", "ND");
                                    
                                    Relation pt = new Relation();
                                    pt.setRelationType("PT");
                                    pt.setTermId(term.getTermId());
                                    unauthorizedTerm.addRelation(pt);
                                    addField(unauthorizedFieldToValuesMap, "pt", term.getTermId());
                                } else if (term == null) {
                                    // skip remaining processing, we are in a non-term-related element
                                } else if (isPath(currentPath, "Note_Text", "Descriptive_Note")
                                        || isPath(currentPath, "Term_Text", "Preferred_Term", "Terms")
                                        || isPath(currentPath, "Place_Type_ID", "Preferred_Place_Type", "Place_Types")
                                        || isPath(currentPath, "Hierarchy")
                                        || isPath(currentPath, "Legacy_ID", "Subject")
                                        || isPath(currentPath, "Degrees", "Latitude", "Standard")
                                        || isPath(currentPath, "Minutes", "Latitude", "Standard")
                                        || isPath(currentPath, "Seconds", "Latitude", "Standard")
                                        || isPath(currentPath, "Direction", "Latitude", "Standard")
                                        || isPath(currentPath, "Decimal", "Latitude", "Standard")
                                        || isPath(currentPath, "Degrees", "Longitude", "Standard")
                                        || isPath(currentPath, "Minutes", "Longitude", "Standard")
                                        || isPath(currentPath, "Seconds", "Longitude", "Standard")
                                        || isPath(currentPath, "Direction", "Longitude", "Standard")
                                        || isPath(currentPath, "Decimal", "Longitude", "Standard")
                                        || isPath(currentPath, "Parent_Subject_ID", "Preferred_Parent")
                                        || isPath(currentPath, "Relationship_Type", "Associative_Relationship")
                                        || (relatedTo && isPath(currentPath, " VP_Subject_ID", "Related_Subject_ID","Associative_Relationship"))
                                        || isPath(currentPath, "Term_Text", "Non-Preferred_Term", "Terms")
                                        || isPath(currentPath, "Term_ID", "Non-Preferred_Term")
                                        || isPath(currentPath, "Subject")
                                        || isPath(currentPath, "Non-Preferred_Term")) {
                                    // start a buffer of all character content (at the close
                                    // tag we'll capture this field.
                                    sb = new StringBuffer();
                                }
                                break;
                            case XMLStreamReader.END_ELEMENT:
                                if (term == null) {
                                     //skip remaining processing
                                } else if (isPath(currentPath, "Note_Text", "Descriptive_Note")) {
                                    TermNote note = new TermNote();
                                    note.setLabel("descriptive note");
                                    note.setContent(sb.toString().trim());
                                    term.addTermNote(note);
                                    addField(fieldToValuesMap, "descriptiveNote", sb.toString().trim());
                                } else if (isPath(currentPath, "Term_Text", "Preferred_Term", "Terms")) {
                                    term.setTermName(sb.toString().trim());
                                    addField(fieldToValuesMap, "termName", sb.toString().trim());
                                } else if (isPath(currentPath, "Place_Type_ID", "Preferred_Place_Type", "Place_Types")) {
                                    term.addTermCategory(sb.toString().trim());
                                    addField(fieldToValuesMap, "placeType", sb.toString().trim());
                                } else if (isPath(currentPath, "Hierarchy")) {
                                    TermNote note = new TermNote();
                                    note.setLabel("hierarchy");
                                    note.setContent(sb.toString().trim());
                                    term.addTermNote(note);
                                    addField(fieldToValuesMap, "hierarchy", sb.toString().trim());
                                } else if (isPath(currentPath, "Legacy_ID", "Subject")) {
                                    addField(fieldToValuesMap, "legacyId", sb.toString().trim());
                                } else if (isPath(currentPath, "Degrees", "Latitude", "Standard")) {
                                    addField(fieldToValuesMap, "degreesLatitude", sb.toString().trim());
                                } else if (isPath(currentPath, "Minutes", "Latitude", "Standard")) {
                                    addField(fieldToValuesMap, "minutesLatitude", sb.toString().trim());
                                } else if (isPath(currentPath, "Seconds", "Latitude", "Standard")) {
                                    addField(fieldToValuesMap, "secondsLatitude", sb.toString().trim());
                                } else if (isPath(currentPath, "Direction", "Latitude", "Standard")) {
                                    addField(fieldToValuesMap, "directionLatitude", sb.toString().trim());
                                } else if (isPath(currentPath, "Decimal", "Latitude", "Standard")) {
                                    addField(fieldToValuesMap, "decimalLatitude", sb.toString().trim());
                                } else if (isPath(currentPath, "Degrees", "Longitude", "Standard")) {
                                    addField(fieldToValuesMap, "degreesLongitude", sb.toString().trim());
                                } else if (isPath(currentPath, "Minutes", "Longitude", "Standard")) {
                                    addField(fieldToValuesMap, "minutesLongitude", sb.toString().trim());
                                } else if (isPath(currentPath, "Seconds", "Longitude", "Standard")) {
                                    addField(fieldToValuesMap, "secondsLongitude", sb.toString().trim());
                                } else if (isPath(currentPath, "Direction", "Longitude", "Standard")) {
                                    addField(fieldToValuesMap, "directionLongitude", sb.toString().trim());
                                } else if (isPath(currentPath, "Decimal", "Longitude", "Standard")) {
                                    addField(fieldToValuesMap, "decimalLongitude", sb.toString().trim());
                                } else if (isPath(currentPath, "Parent_Subject_ID", "Preferred_Parent")) {
                                    Relation bt = new Relation();
                                    bt.setRelationType("BT");
                                    bt.setTermId(sb.toString().trim());
                                    term.addRelation(bt);
                                    addField(fieldToValuesMap, "bt", sb.toString().trim());
                                } else if (isPath(currentPath, "Relationship_Type", "Associative_Relationship") && sb.toString().equalsIgnoreCase("3000/related to")) {
                                    relatedTo = true;
                                } else if (relatedTo && isPath(currentPath, " VP_Subject_ID", "Related_Subject_ID","Associative_Relationship")) {
                                    Relation rt = new Relation();
                                    rt.setRelationType("RT");
                                    rt.setTermId(sb.toString().trim());
                                    term.addRelation(rt);
                                    addField(fieldToValuesMap, "rt", sb.toString().trim());
                                    rtcount ++;
                                    relatedTo = false;
                                } else if (isPath(currentPath, "Term_Text", "Non-Preferred_Term", "Terms")) {
                                    unauthorizedTerm.setTermName(sb.toString().trim());
                                    addField(unauthorizedFieldToValuesMap, "termName", sb.toString().trim());
                                } else if (isPath(currentPath, "Term_ID", "Non-Preferred_Term")) {
                                    unauthorizedTerm.setTermId(sb.toString().trim());
                                    addField(unauthorizedFieldToValuesMap, "id", sb.toString().trim());
                                } else if (isPath(currentPath, "Subject")) {
                                    indexTerm(term, fieldToValuesMap, analyzer, writer);
                                    log.flush();
                                    term = null;
                                    if (shutdown.shouldStop()) {
                                        break;
                                    }
                                } else if (isPath(currentPath, "Non-Preferred_Term")) {
                                    indexTerm(unauthorizedTerm, unauthorizedFieldToValuesMap, analyzer, writer);
                                    log.flush();
                                    
                                    Relation nd = new Relation();
                                    nd.setRelationType("ND");
                                    nd.setTermId(unauthorizedTerm.getTermId());
                                    term.addRelation(nd);
                                    addField(fieldToValuesMap, "nd", unauthorizedTerm.getTermId());
    
                                    unauthorizedTerm = null;
                                }
                                currentPath.pop();
                                sb = null;
                                break;
                            default:
                                break;
                        }
                        int total = ptcount + ndcount;
                        if (total % 5000 == 0 && lastNotice != total) {
                            System.out.println(total + " terms completed.  (" + ptcount + " preferred, " + ndcount + " unauthorized, " + rtcount + " related relationships)");
                            lastNotice = total;
                        }
                        if (ptcount + ndcount > STOP_AFTER) break;
                    }
                    if (ptcount + ndcount > STOP_AFTER) break;
                    parser.close();
                }
                if (ptcount + ndcount > STOP_AFTER) break;
            }
            IndexReader reader = IndexReader.open(args[1]);
            addNTRelationships(reader, writer, analyzer);
            reader.close();
            System.out.print("Optimizing index...");
            writer.optimize();
            System.out.println("DONE!");
        } finally {
            System.out.print("Closing index...");
            writer.close();
            System.out.println("Done!");
            shutdown.notifyDone();
        }
    }
    
    /**
     * Walks through an index adding complementary narrower term index 
     * values for each term related to another term by a broader 
     * relationship.
     * @throws IOException 
     * @throws CorruptIndexException 
     */
    private static void addNTRelationships(IndexReader reader, IndexWriter writer, Analyzer analyzer) throws CorruptIndexException, IOException {
        IndexSearcher searcher = new IndexSearcher(reader);
        for (int i = 0; i < reader.numDocs(); i ++) {
            if (i % 10000 == 0) {
                System.out.println(i + " terms of " + reader.numDocs() + " completed.");
            }

            Document doc = reader.document(i);
            String id = doc.getField("id.facet").stringValue();
            org.apache.lucene.index.Term term = new org.apache.lucene.index.Term("id.facet", id);
            Hits btHits = searcher.search(new TermQuery(new org.apache.lucene.index.Term("bt.facet", id)));
            if (btHits.length() == 0) {
                // no terms for which this term is a broader term
            } else {
                for (int j = 0; j < btHits.length(); j ++) {
                    // recreate all existing fields
                    Document newDoc = new Document();
                    List fields = doc.getFields();
                    for (int k = 0; k < fields.size(); k ++) {
                        Field field = (Field) fields.get(k);
                        if (field.name().endsWith(".facet")) {
                            String fieldName = field.name().substring(0, field.name().indexOf(".facet"));
                            String fieldValue = field.stringValue();
                            newDoc.add(new Field(fieldName + ".exact", fieldValue, Field.Store.NO, Field.Index.TOKENIZED));
                            newDoc.add(new Field(fieldName + ".facet", fieldValue, Field.Store.YES, Field.Index.TOKENIZED));
                            newDoc.add(new Field(fieldName, fieldValue, Field.Store.NO, Field.Index.TOKENIZED));
                        }
                    }
                    // add the narrower field value
                    Document ntDoc = btHits.doc(j);
                    String ntId = ntDoc.getField("id.facet").stringValue();
                    newDoc.add(new Field("nt.exact", ntId, Field.Store.NO, Field.Index.TOKENIZED));
                    newDoc.add(new Field("nt.facet", ntId, Field.Store.YES, Field.Index.TOKENIZED));
                    newDoc.add(new Field("nt", ntId, Field.Store.NO, Field.Index.TOKENIZED));
                    writer.updateDocument(term, newDoc, analyzer);
                }
            }     
        }

    }
    
    private static void indexTerm(Term term, Map<String, List<String>> fields, Analyzer analyzer, IndexWriter writer) throws UnsupportedEncodingException, IOException {
        // write the term to the index
        Document indexDoc = new Document();
        // add each field
        for (String field : fields.keySet()) {
            for (String value : fields.get(field)) {
                indexDoc.add(new Field(field + ".exact", value, Field.Store.NO, Field.Index.TOKENIZED));
                indexDoc.add(new Field(field + ".facet", value, Field.Store.YES, Field.Index.TOKENIZED));
                indexDoc.add(new Field(field, value, Field.Store.NO, Field.Index.TOKENIZED));
            }
        }
        // add a zthes XML record
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();

            // create a new Marshaller
            XMLContext context = new XMLContext();
            Marshaller marshaller = context.createMarshaller();
            marshaller.setValidation(false);
            marshaller.setSupressXMLDeclaration(true);
            //marshaller.setEncoding("UTF-8");
            marshaller.setWriter(new OutputStreamWriter(os, "UTF-8"));

            // marshal the zthes record
            Zthes zthes = new Zthes();
            zthes.addTerm(term);
            marshaller.marshal(zthes);
            String zthesXml = new String(os.toByteArray(), "UTF-8");
            // The "setSuppressXMLDeclaration" doesn't seem
            // to be respected here, so we manually remove it.
            zthesXml = zthesXml.substring(zthesXml.indexOf("<Zthes"));
            Field xmlField = new Field("zthes", zthesXml, Field.Store.COMPRESS, Field.Index.NO);
            indexDoc.add(xmlField);
        } catch (MarshalException ex) {
            throw new RuntimeException(ex);
        } catch (ValidationException ex) {
            throw new RuntimeException(ex);
        }
        writer.addDocument(indexDoc, analyzer);        
    }
    
    /**
     * Checks whether the path (strings) are equal to the last 
     * entries in the given stack.  For example, if the parameters
     * are a stack ("One", "Two", "Three") and "Two", "three": this
     * method will return true.  
     */
    private static boolean isPath(Stack<String> stack, String... path) {
        int i = stack.size() - 1;
        for (String pathEl : path) {
            if (i >= 0) {
                if (!pathEl.equalsIgnoreCase(stack.get(i --))) {
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }
    
    private static void addField(Map<String, List<String>> map, String name, String value) {
        if (map.containsKey(name)) {
            map.get(name).add(value);
        } else {
            ArrayList<String> values = new ArrayList<String>();
            values.add(value);
            map.put(name, values);
        }
    }
    
    public static class ShutdownThread extends Thread {

        private boolean shutdown;
        
        private boolean hasStopped;
        
        public ShutdownThread() {
            shutdown = false;
            hasStopped = false;
        }
        
        public boolean shouldStop() {
            return this.shutdown;
        }
        
        public void notifyDone() {
            this.hasStopped = true;
        }
        
        public void run() {
            System.out.println("Shutting down...");
            shutdown = true;
            while (!this.hasStopped) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
            System.out.println("DONE shutting down!");
        }
    }
       
    
    private static class TGNCharacterConverter {

        private Map<String, String> charMap;
        
        public TGNCharacterConverter(File characterXmlFile) throws FileNotFoundException, XMLStreamException {
            this.charMap = new HashMap<String, String>();
            FileInputStream is = new FileInputStream(characterXmlFile);
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLStreamReader parser = factory.createXMLStreamReader(is);
            String currentTag = null;
            String vcsCode = null;
            while (parser.hasNext()) {
                int eventType = parser.next();
                switch (eventType) {
                    case XMLStreamReader.CHARACTERS:
                        if (currentTag == null) {
                            break;
                        } else if (currentTag.equalsIgnoreCase("VCS_CODE")) {
                            vcsCode = parser.getText();
                        } else if (currentTag.equalsIgnoreCase("UNICODE")) {
                            String unicodeList = parser.getText();
                            StringBuffer val = new StringBuffer();
                            for (String hex : unicodeList.split(" ")) {
                                val.append((char) Integer.parseInt(hex, 16));
                            }
                            charMap.put(vcsCode, "" + val);
                            vcsCode = null;
                        }
                        break;
                    case XMLStreamReader.START_ELEMENT:
                        currentTag = parser.getLocalName();
                        break;
                    case XMLStreamReader.END_ELEMENT:
                        currentTag = null;
                        break;
                    default:
                        break;
                }
            }
            parser.close();
        }
        
        public String convertCharacters(String line) {
            for (String key : this.charMap.keySet()) {
                line = line.replace(key, this.charMap.get(key));
            }
            return line;
        }
        
    }
    
}
