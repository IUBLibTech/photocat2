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
package edu.indiana.dlib.fedoraindexer.tools;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.LockObtainFailedException;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.ValidationException;
import org.exolab.castor.xml.XMLContext;

import edu.indiana.dlib.fedora.client.FedoraClient;
import edu.indiana.dlib.search.analyzers.CaseInsensitiveSRUSupportAnalyzer;
import edu.indiana.dlib.xml.zthes.Relation;
import edu.indiana.dlib.xml.zthes.Term;
import edu.indiana.dlib.xml.zthes.TermNote;
import edu.indiana.dlib.xml.zthes.Zthes;

/**
 * <p>
 *   A simple command-line tool that parses the TGMI XML file from 
 *   the Library of Congress and creates a Lucene index suitable 
 *   for advanced SRW searching.
 * </p>
 * <p>
 *   The current implementation indexes every field listed in the
 *   flat format with the name of the XML element.  Furthermore
 *   an ".exact" and ".facet" form will be indexed as well and a
 *   simple "zthes" field containing a valid ZThes XML fragment
 *   that represents the whole record.
 * </p>
 */
public class TGMParser {

    public static void indexTGM(InputStream xmlStream, IndexWriter writer) throws XMLStreamException, CorruptIndexException, IOException {
        Analyzer analyzer = new CaseInsensitiveSRUSupportAnalyzer();

        // Set up the reader
        XMLInputFactory readerFactory = XMLInputFactory.newInstance();
        XMLEventReader reader = readerFactory.createXMLEventReader(xmlStream);
        
        Stack<String> currentPath = new Stack<String>();
        StringBuffer characters = null;
        Map<String, List<String>> fieldToValuesMap = new HashMap<String, List<String>>();
        Term currentTerm = null;
        while (reader.hasNext()) {
            XMLEvent event = (XMLEvent) reader.next();
            if (event.getEventType() == XMLEvent.START_ELEMENT) {
                StartElement startEl = (StartElement) event;
                currentPath.push(startEl.getName().getLocalPart());
                characters = new StringBuffer();
                if ("CONCEPT".equalsIgnoreCase(startEl.getName().getLocalPart())) {
                    fieldToValuesMap.clear();
                    currentTerm = new Term();
                }
            } else if (event.getEventType() == XMLEvent.END_ELEMENT) {
                addField(fieldToValuesMap, currentPath.peek(), characters.toString().trim());
                if ("DESCRIPTOR".equalsIgnoreCase(currentPath.peek())) {
                    currentTerm.setTermName(characters.toString().trim());
                    currentTerm.setTermType("PT");
                    addField(fieldToValuesMap, "isPT", "true");
                } else if ("NON-DESCRIPTOR".equalsIgnoreCase(currentPath.peek())) {
                    currentTerm.setTermName(characters.toString().trim());
                    currentTerm.setTermType("ND");
                    addField(fieldToValuesMap, "isPT", "false");
                } else if ("TNR".equalsIgnoreCase(currentPath.peek())) {
                    currentTerm.setTermId(characters.toString().trim());
                } else if ("UF".equalsIgnoreCase(currentPath.peek()) || "BT".equalsIgnoreCase(currentPath.peek()) || "NT".equalsIgnoreCase(currentPath.peek()) || "RT".equalsIgnoreCase(currentPath.peek()) || "USE".equalsIgnoreCase(currentPath.peek())) {
                    Relation relation = new Relation();
                    relation.setRelationType(currentPath.peek());
                    relation.setTermName(characters.toString().trim());
                    currentTerm.addRelation(relation);
                } else if ("SN".equalsIgnoreCase(currentPath.peek())) {
                    TermNote note = new TermNote();
                    note.setLabel("scope note");
                    note.setContent(characters.toString().trim());
                    currentTerm.addTermNote(note);
                } else if ("FUN".equalsIgnoreCase(currentPath.peek())) {
                    TermNote note = new TermNote();
                    note.setLabel("former usage note");
                    note.setContent(characters.toString().trim());
                    currentTerm.addTermNote(note);
                } else if ("CONCEPT".equalsIgnoreCase(currentPath.peek())) {
                    Document indexDoc = new Document();
                    // add each field
                    for (String field : fieldToValuesMap.keySet()) {
                        for (String value : fieldToValuesMap.get(field)) {
                            indexDoc.add(new Field(field + ".exact", value, Field.Store.NO, Field.Index.TOKENIZED));
                            indexDoc.add(new Field(field + ".facet", value, Field.Store.YES, Field.Index.TOKENIZED));
                            indexDoc.add(new Field(field, value, Field.Store.NO, Field.Index.TOKENIZED));
                        }
                    }
                    
                    // add a field indicating if it's a top level term
                    boolean topLevelTerm = true;
                    for (int i = 0; i < currentTerm.getRelationCount(); i ++) {
                        if (currentTerm.getRelation(i).getRelationType().equals("BT")) {
                            topLevelTerm = false;
                        }
                    }
                    indexDoc.add(new Field("isTopLevelTerm.exact", String.valueOf(topLevelTerm), Field.Store.NO, Field.Index.TOKENIZED));
                    indexDoc.add(new Field("isTopLevelTerm.facet", String.valueOf(topLevelTerm), Field.Store.YES, Field.Index.TOKENIZED));
                    indexDoc.add(new Field("isTopLevelTerm", String.valueOf(topLevelTerm), Field.Store.NO, Field.Index.TOKENIZED));
                    
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
                        zthes.addTerm(currentTerm);
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
                currentPath.pop();
                
            } else if (event.getEventType() == XMLEvent.CHARACTERS) {
                characters.append(event.asCharacters().getData());
            }
        }
        reader.close();
        //addRecursiveRelationshipsToIndex(writer);
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
    
    private static void addRecursiveRelationshipsToIndex(IndexWriter writer) throws CorruptIndexException, IOException {
        IndexSearcher searcher = new IndexSearcher(writer.getDirectory(), true);
        
        // go through each document and add all NT-RECURSIVE and
        // BT-RECURSIVE values as needed.  (this is probably inefficient
        // but it's a simple algorithm.)
        for (int i = 0; i < writer.getReader().numDocs(); i ++) {
            Document doc = writer.getReader().document(i);
            if (doc.getField("DESCRIPTOR.facet") != null) {
                String descriptor = doc.getField("DESCRIPTOR.facet").stringValue();
                List<String> ntRecursiveValues = getRecursivelyRelatedFields(descriptor, "BT", searcher);
                for (String ntRecString : ntRecursiveValues) {
                    doc.add(new Field("NT-RECURSIVE", ntRecString, Field.Store.NO, Field.Index.TOKENIZED));
                    doc.add(new Field("NT-RECURSIVE.exact", ntRecString, Field.Store.NO, Field.Index.TOKENIZED));
                    doc.add(new Field("NT-RECURSIVE.facet", ntRecString, Field.Store.YES, Field.Index.TOKENIZED));
                }
                List<String> btRecursiveValues = getRecursivelyRelatedFields(descriptor, "NT", searcher);
                for (String btRecString : btRecursiveValues) {
                    doc.add(new Field("BT-RECURSIVE", btRecString, Field.Store.NO, Field.Index.TOKENIZED));
                    doc.add(new Field("BT-RECURSIVE.exact", btRecString, Field.Store.NO, Field.Index.TOKENIZED));
                    doc.add(new Field("BT-RECURSIVE.facet", btRecString, Field.Store.YES, Field.Index.TOKENIZED));
                }
                if (!ntRecursiveValues.isEmpty() || !btRecursiveValues.isEmpty()) {
                    //System.out.println(descriptor + " " + ntRecursiveValues.size() + " NT-RECURSIVE");
                    //System.out.println(descriptor + " " + btRecursiveValues.size() + " BT-RECURSIVE");
                    writer.updateDocument(new org.apache.lucene.index.Term("DESCRIPTOR.exact", descriptor), doc);
                }       
            }     
        }
    }
    
    private static List<String> getRecursivelyRelatedFields(String value, String field, Searcher searcher) throws IOException {
        List<String> results = new LinkedList<String>(); 
        Hits resultHits = searcher.search(new TermQuery(new org.apache.lucene.index.Term(field + ".facet", value)));
        for (int i = 0; i < resultHits.length(); i ++) {
            Document match = resultHits.doc(i);
            results.add(match.getField("DESCRIPTOR.facet").stringValue());
        }
        List<String> childResults = new LinkedList<String>();
        for (int i = 0; i < results.size(); i ++) {
            String nextGen = results.get(i);
            childResults.addAll(getRecursivelyRelatedFields(nextGen, field, searcher));
        }
        results.addAll(childResults);
        return results;
    }
    
}
