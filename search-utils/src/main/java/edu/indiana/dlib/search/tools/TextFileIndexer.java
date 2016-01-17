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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.ValidationException;
import org.exolab.castor.xml.XMLContext;

import edu.indiana.dlib.search.analyzers.CaseInsensitiveSRUSupportAnalyzer;
import edu.indiana.dlib.xml.zthes.Term;
import edu.indiana.dlib.xml.zthes.TermNote;
import edu.indiana.dlib.xml.zthes.Zthes;

/**
 * <p>
 *   A simple command-line tool that parses limited vocabulary 
 *   information from a text file.
 * </p>
 * <p>
 *   The current implementation expects a term per line and treats
 *   anything in parentheses as a description.
 * </p>
 */
public class TextFileIndexer {

    //private static final String TERM_PATTERN = "^([^\\(]*)(\\(([^\\)]*)\\))?\\w*$";
    private static final String TERM_PATTERN = "(^.*$)()()";
    
    /**
     * A prefix for the URI (identifier) for terms.  The full
     * identifier will be the prefix plus a massaged version of
     * the term name.
     */
    private String uriPrefix;
    
    public static void main(String[] args) throws Exception {
        File textFile = new File(args[0]);
        IndexWriter writer = new IndexWriter(args[1], new CaseInsensitiveSRUSupportAnalyzer());
        TextFileIndexer indexer = new TextFileIndexer(args[2]);
        try {
            int count = indexer.indexTextFile(textFile, writer);
            System.out.println("Indexed " + count + " terms");
            writer.flush();
        } finally {
            writer.optimize();
            writer.close();
        }
    }
    
    public TextFileIndexer(String uriPrefix) {
        this.uriPrefix = uriPrefix;
    }
    
    public int indexTextFile(File textFile, IndexWriter writer) throws IOException {
        Analyzer analyzer = new CaseInsensitiveSRUSupportAnalyzer();
        Pattern pattern = Pattern.compile(TERM_PATTERN);
        int count = 0;
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(textFile)));
        String line = reader.readLine();
        while (line != null) {
            Matcher m = pattern.matcher(line);
            String termName = null;
            String description = null;
            if (!m.matches()) {
                throw new RuntimeException("Unparsible line: \"" + line + "\" for pattern \"" + TERM_PATTERN + "\"");
            } else {
                termName = m.group(1).trim();
                if (m.group(3) != null && m.group(3).trim().length() > 0) {
                    description = m.group(3);
                    System.out.println("Description: " + description);
                }
            }
            
            Document indexDoc = new Document();
            Term currentTerm = new Term();
            currentTerm.setTermName(termName);
            indexDoc.add(new Field("dc.title", termName, Field.Store.NO, Field.Index.TOKENIZED));
            indexDoc.add(new Field("dc.title.exact", termName, Field.Store.NO, Field.Index.TOKENIZED));
            indexDoc.add(new Field("dc.title.facet", termName, Field.Store.YES, Field.Index.TOKENIZED));

            String termId = generateTermId(termName);
            currentTerm.setTermId(termId);
            indexDoc.add(new Field("dc.identifier", termId, Field.Store.NO, Field.Index.TOKENIZED));
            indexDoc.add(new Field("dc.identifier.exact", termId, Field.Store.NO, Field.Index.TOKENIZED));
            indexDoc.add(new Field("dc.identifier.facet", termId, Field.Store.YES, Field.Index.TOKENIZED));

            if (description != null) {
                TermNote note = new TermNote();
                note.setLabel("description");
                note.setContent(description);
                currentTerm.addTermNote(note);
                    
                indexDoc.add(new Field("description", description, Field.Store.NO, Field.Index.TOKENIZED));
            }
            currentTerm.setTermType("PT");
                
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
            count ++;
            line = reader.readLine();
        }
        return count;
    }
    
    private String generateTermId(String name) throws UnsupportedEncodingException {
        return this.uriPrefix + URLEncoder.encode(name.toLowerCase(), "UTF-8");
    }
}
