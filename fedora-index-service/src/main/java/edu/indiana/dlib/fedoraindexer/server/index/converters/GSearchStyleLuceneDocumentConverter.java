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
package edu.indiana.dlib.fedoraindexer.server.index.converters;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.indiana.dlib.fedoraindexer.server.index.LuceneDocumentConverter;

/**
 * <p>
 *   Converts a DOM representation of an XML file into
 *   the Lucene index Document it expresses.  This class
 *   intends to provide interoperability between XSLT 
 *   written for GSearch and this index service.
 * </p>
 */
public class GSearchStyleLuceneDocumentConverter implements LuceneDocumentConverter {

    public static Logger LOGGER = Logger.getLogger(GSearchStyleLuceneDocumentConverter.class);
    
    public Document convert(org.w3c.dom.Document dom) {
        // build the document from the DOM
        Document indexDoc = new Document();
        NodeList nodes = dom.getDocumentElement().getElementsByTagName("IndexField");
        for (int i = 0; i < nodes.getLength(); i ++) {
            Element indexFieldEl = (Element) nodes.item(i);
            if (indexFieldEl.getFirstChild() == null || indexFieldEl.getFirstChild().getNodeValue() == null) {
                LOGGER.info("Field: " + indexFieldEl.getAttribute("IFname") + " is empty.");
            } else {
                indexDoc.add(new Field(indexFieldEl.getAttribute("IFname"), getTextOfElement(indexFieldEl), parseFieldStore(indexFieldEl.getAttribute("store")), parseFieldIndex(indexFieldEl.getAttribute("index")), parseFieldTermVector(indexFieldEl.getAttribute("termVector"))));
            }
        }
        return indexDoc;
    }

    private static Field.Store parseFieldStore(String store) {
        if (store == null) {
            return Field.Store.NO;
        } else if (store.equalsIgnoreCase("yes")) {
            return Field.Store.YES;
        } else if (store.equalsIgnoreCase("no")) {
            return Field.Store.NO;
        } else if (store.equalsIgnoreCase("compress")) {
            return Field.Store.COMPRESS;
        } else {
            return Field.Store.NO;
        }
    }
    
    private static Field.Index parseFieldIndex(String index) {
        if (index == null) {
            return Field.Index.NO;
        } else if (index.equalsIgnoreCase("NO")) {
            return Field.Index.NO;
        } else if (index.equalsIgnoreCase("TOKENIZED")) {
            return Field.Index.TOKENIZED;
        } else if (index.equalsIgnoreCase("UN_TOKENIZED")) {
            return Field.Index.UN_TOKENIZED;
        } else if (index.equalsIgnoreCase("NO_NORMS")) {
            return Field.Index.NO_NORMS;
        } else {
            return Field.Index.NO;
        }
    }
    
    private static Field.TermVector parseFieldTermVector(String termVector) {
        if (termVector == null) {
            return Field.TermVector.NO;
        } else if (termVector.equalsIgnoreCase("YES")) {
            return Field.TermVector.YES;
        } else {
            return Field.TermVector.NO;
        }
    } 

    private String getTextOfElement(Element el) {
        StringBuffer content = new StringBuffer();
        
        NodeList nodes = el.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i ++) {
            Node child = nodes.item(i);
            if (child.getNodeType() == Node.CDATA_SECTION_NODE || child.getNodeType() == Node.TEXT_NODE) {
                content.append(child.getNodeValue());
            } else {
                LOGGER.debug("ERROR: " + child.getNodeName());
            }
        }
        return content.toString();
    }
    
}
