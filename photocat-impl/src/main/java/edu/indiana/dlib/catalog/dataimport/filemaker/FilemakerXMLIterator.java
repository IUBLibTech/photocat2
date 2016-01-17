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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import edu.indiana.dlib.catalog.dataimport.Metadata;
import edu.indiana.dlib.catalog.dataimport.Record;

public class FilemakerXMLIterator implements Iterator<Record> {

    private XMLEventReader reader;
    
    private Metadata metadata;

    private boolean isOnNextRow;
    
    public FilemakerXMLIterator(InputStream xmlInputStream) throws XMLStreamException {
        // Set up the reader
        XMLInputFactory readerFactory = XMLInputFactory.newInstance();
        reader = readerFactory.createXMLEventReader(xmlInputStream);
        parseMetadata();
    }
    
    private void parseMetadata() {
        ArrayList<String> fields = new ArrayList<String>();
        while (reader.hasNext()) {
            XMLEvent event = (XMLEvent) reader.next();
            if (event.isStartElement()) {
                StartElement startEl = (StartElement) event;
                if (startEl.getName().getLocalPart().equals("FIELD")) {
                    fields.add(startEl.getAttributeByName(new QName("NAME")).getValue());
                }
            } else if (event.isEndElement()) {
                EndElement endEl = (EndElement) event; 
                if (endEl.getName().getLocalPart().equals("METADATA")) {
                    break;
                }
            }
            
        }
        this.metadata = new Metadata(fields.toArray(new String[0]));
    }
    
    public Metadata getMetadata() {
        return this.metadata;
    }
    
    public boolean hasNext() {
        if (isOnNextRow) {
            return true;
        } else {
            while (reader.hasNext()) {
                XMLEvent event = (XMLEvent) reader.next();
                if (event.isStartElement()) {
                    StartElement startEl = (StartElement) event;
                    if (startEl.getName().getLocalPart().equals("ROW")) {
                        isOnNextRow = true;
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public Record next() {
        if (hasNext()) {
            isOnNextRow = false;
            int col = 0;
            StringBuffer value = null;
            String[] values = new String[metadata.getFieldCount()];
            try {
                while (reader.hasNext()) {
                    XMLEvent event = (XMLEvent) reader.nextEvent();
                    if (event.isStartElement()) {
                        StartElement startEl = (StartElement) event;
                        if (startEl.getName().getLocalPart().equals("DATA")) {
                            value = new StringBuffer();
                        }
                    } else if (event.isEndElement()) {
                        EndElement endEl = (EndElement) event; 
                        if (endEl.getName().getLocalPart().equals("ROW")) {
                            break;
                        } else if (endEl.getName().getLocalPart().equals("DATA")) {
                            if (value.toString().trim().length() > 0) {
                                values[col] = value.toString();
                            }
                        } else if (endEl.getName().getLocalPart().equals("COL")) {
                            col ++;
                        }
                    } else if (event.isCharacters()) {
                        if (value != null) {
                            value.append(((Characters) event).getData());
                        }
                    }
                }
            } catch (XMLStreamException ex) {
                throw new RuntimeException(ex);
            }
            return new Record(metadata, values);
        } else {
            throw new IllegalStateException();
        }
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

}
