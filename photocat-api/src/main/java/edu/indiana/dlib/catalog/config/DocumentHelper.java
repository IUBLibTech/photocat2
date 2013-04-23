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
package edu.indiana.dlib.catalog.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import edu.indiana.dlib.catalog.config.impl.SchemaCachingEntityResolver;

/**
 * A crude utility class in which messy techniques for common
 * actions associated with DOM documents can be grouped to allow
 * later improvements in both performance and readability.
 */
public class DocumentHelper {
    
    private DocumentBuilder docBuilder;

    private SchemaCachingEntityResolver schemaCache;
    
    private static DocumentHelper INSTANCE;
    
    private DocumentHelper() {
        schemaCache = null;
        docBuilder = null;
    }
    
    public synchronized void setCacheDirectory(File directory) throws IOException {
        schemaCache = new SchemaCachingEntityResolver(directory);
        if (docBuilder != null) {
            docBuilder.setEntityResolver(this.schemaCache);
        }
    }
    
    public synchronized void clearCache() throws IOException {
        if (schemaCache != null) {
            schemaCache.clearCache();
        }
    }
    
    public Document parseAndValidateDocument(InputStream is) throws IOException, DataFormatException {
        try {
            if (docBuilder == null) {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                dbf.setValidating(true);
                dbf.setNamespaceAware(true);
                dbf.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaLanguage", 
                    "http://www.w3.org/2001/XMLSchema");
                docBuilder = dbf.newDocumentBuilder();
                if (schemaCache != null) {
                    docBuilder.setEntityResolver(this.schemaCache);
                }
                docBuilder.setErrorHandler(new ErrorHandler() {
    
                    public void error(SAXParseException exception) throws SAXException {
                        exception.printStackTrace();
                        throw exception;
                    }
    
                    public void fatalError(SAXParseException exception) throws SAXException {
                        exception.printStackTrace();
                        throw exception;
                    }
    
                    public void warning(SAXParseException exception) throws SAXException {
                        // skip
                    }});
            }
            return docBuilder.parse(new InputSource(is));
        } catch (ParserConfigurationException ex) {
            throw new DataFormatException(ex);
        } catch (SAXException ex) {
            throw new DataFormatException(ex);
        }
    }
    
    public static DocumentHelper getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DocumentHelper();
        }
        return INSTANCE;
    }

    public static void writeOutDocument(OutputStream os, Document doc) throws TransformerException, IOException {
        DOMSource source = new DOMSource(doc);
        StreamResult sResult = new StreamResult(os);
        TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer t = tFactory.newTransformer();
        t.setOutputProperty(OutputKeys.ENCODING, "utf-8");
        t.setOutputProperty(OutputKeys.METHOD, "xml");
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        t.transform(source, sResult);
        sResult.getOutputStream().close();
    }
}
