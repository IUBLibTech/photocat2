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
package edu.indiana.dlib.catalog.dataimport.filemaker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import edu.indiana.dlib.catalog.dataimport.Metadata;
import edu.indiana.dlib.catalog.dataimport.Record;
import edu.indiana.dlib.catalog.dataimport.Records;

/**
 * A Records implementation that parses a Filemaker Pro 
 * database XML export. 
 */
public class FilemakerXML implements Records {

    private File xmlFile;
    
    private Metadata metadata;
    
    /**
     * A test program that iterates through all the records in
     * the database.
     */
    public static void main(String[] args) throws Exception {
        FilemakerXML db = new FilemakerXML(new File(args[0]));
        for (Record rec : db) {
            for (int i = 0; i < rec.getMetadata().getFieldCount(); i ++) {
                System.out.println(rec.getMetadata().getFieldName(i) + ": " + rec.getValue(i));
            }
            System.out.println();
        }
    }
    
    public FilemakerXML(File xmlFile) throws XMLStreamException, IOException {
        this.xmlFile = xmlFile;
        if (!isLikelyFilemakerProXMLExport()) {
            throw new IllegalArgumentException();
        }
        FileInputStream fis = new FileInputStream(xmlFile);
        FilemakerXMLIterator it = new FilemakerXMLIterator(fis);
        metadata = it.getMetadata();
        fis.close();
    }
    
    /**
     * Does a quick check to see if the file looks like a filemaker pro 
     * database XML export.  The current implementation attempts to read
     * the file as an XML stream and returns true if the first StartElement
     * has the name "FMPXMLRESULT" and the namespace 
     * "http://www.filemaker.com/fmpxmlresult".  This method should only
     * be called by the constructor
     * @return true if this method *thinks* the file is a filemaker pro
     * database XML export or false otherwise
     * @throws XMLStreamException 
     * @throws FileNotFoundException 
     */
    private boolean isLikelyFilemakerProXMLExport() throws FileNotFoundException, XMLStreamException {
        XMLInputFactory readerFactory = XMLInputFactory.newInstance();
        XMLEventReader reader = readerFactory.createXMLEventReader(new FileInputStream(this.xmlFile));
        try {
            while (reader.hasNext()) {
                XMLEvent event = (XMLEvent) reader.next();
                if (event.isStartElement()) {
                    if (event.asStartElement().getName().getLocalPart().equals("FMPXMLRESULT")) {
                        return true;
                    } else {
                        return false;
                    }
                }
            }
            return false;
        } finally {
            reader.close();
        }
    }
    
    public Metadata getMetadata() {
        return metadata;
    }
    
    public Iterator<Record> getRecordIterator() throws FileNotFoundException, XMLStreamException {
        return new FilemakerXMLIterator(new FileInputStream(xmlFile));
    }

    public Iterator<Record> iterator() {
        try {
            return new FilemakerXMLIterator(new FileInputStream(xmlFile));
        } catch (FileNotFoundException ex) {
            throw new RuntimeException(ex);
        } catch (XMLStreamException ex) {
            throw new RuntimeException(ex);
        }
    }
 
}
