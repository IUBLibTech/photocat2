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
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.indiana.dlib.fedoraindexer.server.index.LuceneDocumentConverter;

/**
 * <p>
 *   A special purpose XSLT syntax that allows fields 
 *   to be specified generally, and then behind the 
 *   scenes this class creates many slightly different
 *   copies for the field to facility all of the 
 *   functionality that can be supported for CQL queries.
 * </p>
 * <p>
 *   Specifically this class stores the following 
 *   representations of a given field:
 *   <ul>
 *     <li>
 *       base - a normalized version of the field 
 *     </li>
 *     <li>
 *       .sort - a version of this field used for 
 *               sorting.  Only one value can be
 *               stored and by default, the first
 *               encountered in a document is used.
 *     </li>
 *     <li>
 *       .facet - a version of this field used to 
 *                allow faceted browsing.  (Note:
 *                for performance reasons this 
 *                isn't stored for all fields, just
 *                those that probably contain keywords.
 *     </li>
 *     <li>
 *       .exact - a version of this field used to
 *                allow exact matches
 *     </li>
 *     <li>
 *       .stemmed - a version of this field used to allow
 *                  stemmed matching.
 *     </li>
 *     <li>
 *       .stored - a version of this field used to allow
 *                 highlighting by retaining a stored 
 *                 unmodified copy of the original field
 *     </li>
 *     <li>
 *       .present - a version of this field that indicates
 *                  whether this field has a value.  It is
 *                  set to 0 if no value is set and 1 if
 *                  one or more values are present.  To 
 *                  indicate that no value is set the XML
 *                  file may contain an empty (zero-length)
 *                  value for that field.  Otherwise, there
 *                  is no way to determine which fields to
 *                  expect.
 *     </li>
 *   </ul>
 * </p>
 */
public class CQLStyleLuceneDocumentConverter implements LuceneDocumentConverter {

    public static Logger LOGGER = Logger.getLogger(CQLStyleLuceneDocumentConverter.class);
    
    public static final String SORT_SUFFIX = ".sort";
    
    public static final String FACET_SUFFIX = ".facet";
    
    public static final String EXACT_SUFFIX = ".exact";
    
    public static final String STEMMED_SUFFIX = ".stemmed";
    
    public static final String STORED_SUFFIX = ".stored";
    
    public static final String PRESENT_SUFFIX = ".present";
    
    /**
     * A field marked as a keyword is a field that is eligible for
     * sorting, exact matching, faceted browsing and other searches
     * that are meant for short, perhaps repeatable fields.
     */
    public static final String TYPE_KEYWORD = "keyword";
    
    /**
     * A field marked as a record that will not be indexed, but instead
     * stored verbatim for retrieval with the search results.
     */
    public static final String TYPE_RECORD = "record";
    
    /**
     * A field marked as "text" is a field that is expected to be
     * freeform text and is most likely too long to expect any
     * matching against the whole value.
     */
    public static final String TYPE_TEXT = "text";

    /**
     * This analyzer must properly analyze the fields given 
     * their meaningful extensions.
     */
    private Analyzer analyzer;
    
    public CQLStyleLuceneDocumentConverter() {
    }
    
    public Document convert(org.w3c.dom.Document dom) {
        Document indexDoc = new Document();
        NodeList nodes = dom.getDocumentElement().getElementsByTagName("field");
        for (int i = 0; i < nodes.getLength(); i ++) {
            Element indexFieldEl = (Element) nodes.item(i);
            String value = getTextOfElement(indexFieldEl);
            String name = indexFieldEl.getAttribute("name");
            String type = getType(indexFieldEl);

            if (type.equals(TYPE_KEYWORD)) {
                addKeywordField(name, value, indexDoc);
            } else if (type.equals(TYPE_RECORD)) {
                addRecordField(name, value, indexDoc);
            } else if (type.equals(TYPE_TEXT)) {
                addFullTextField(name, value, indexDoc);
            } else {
                LOGGER.warn("Unexpected type, \"" + type + "\", for field, \"" + name + "\": excluding from index");
            }
        }
        return indexDoc;

    }
    
    private static String normalizeForSort(String value) {
        return value.toLowerCase().replaceAll("\\W", "");
    }
    
    /**
     * <p>
     *   Gets the type of element.  This is either determined by parsing
     *   the "type" attribute, or if that attribute is missing, guessing
     *   the type based on the length of the field.  In the current 
     *   implementation any field with a length greater than 64 characters
     *   is considered text.
     * </p>
     */
    private String getType(Element el) {
        String explicitType = el.getAttribute("type");
        if (explicitType != null) {
            return explicitType;
        } else {
            String value = getTextOfElement(el);
            if (value != null && value.length() > 64) {
                return TYPE_TEXT;
            } else {
                return TYPE_KEYWORD;
            }
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

    /**
     * Adds several Lucene Field objects to the provided index Document that
     * reflect the provided name and value and are appropriate for searching
     * full-text.  This method stores multiple versions to accommodate full-text
     * searching as well as match snippet highlighting.
     * @param name the base name of the field
     * @param value the value of the field
     * @param indexDoc the document to be updated
     */
    public static void addFullTextField(String name, String value, Document indexDoc) {
        // store base
        indexDoc.add(new Field(name, value, Field.Store.NO, Field.Index.TOKENIZED, Field.TermVector.WITH_POSITIONS_OFFSETS));
        
        // store stemmed
        indexDoc.add(new Field(name + STEMMED_SUFFIX, value, Field.Store.YES, Field.Index.TOKENIZED, Field.TermVector.NO));

        // store stored
        indexDoc.add(new Field(name + STORED_SUFFIX, value, Field.Store.YES, Field.Index.UN_TOKENIZED, Field.TermVector.NO));
        
    }
    
    /**
     * Adds several Lucene Field objects to the provided index Document that
     * reflect the provided name and value and are appropriate for searching
     * shorter fields.  This method stores multiple versions to accommodate 
     * multiple search types, such as stemmed searching, faceted results, 
     * exact matching, etc.
     * @param name the base name of the field
     * @param value the value of the field
     * @param indexDoc the document to be updated
     */
    public static void addKeywordField(String name, String value, Document indexDoc) {
        if (value == null || value.trim().equals("")) {
            // Store an indicator that the field is unset.
            indexDoc.add(new Field(name + PRESENT_SUFFIX, "0", Field.Store.NO, Field.Index.TOKENIZED, Field.TermVector.NO));
        } else {
            // Store an indicator that the field is set
            indexDoc.add(new Field(name + PRESENT_SUFFIX, "1", Field.Store.NO, Field.Index.TOKENIZED, Field.TermVector.NO));
            
            // Store the base version:
            //  This version is tokenized, unstored and contains no
            //  term-vector information.  The tokenization is determined
            //  by the analyzer but should be general enough for a simple
            //  unqualified keyword search.
            indexDoc.add(new Field(name, value, Field.Store.NO, Field.Index.TOKENIZED, Field.TermVector.NO));
            
            // Store the sort version: (if not already stored)
            //   This field is normalized here and stored untokenized.
            if (indexDoc.getField(name + SORT_SUFFIX) == null) {
                indexDoc.add(new Field(name + SORT_SUFFIX, normalizeForSort(value), Field.Store.NO, Field.Index.UN_TOKENIZED, Field.TermVector.NO));
            }
            
            // Store the facet version:
            //   This field must be STORED and UN_TOKENIZED to support the
            //   various algorithms for facet generation.
            indexDoc.add(new Field(name + FACET_SUFFIX, value, Field.Store.YES, Field.Index.UN_TOKENIZED, Field.TermVector.NO));
            
            // Store the exact version:
            //   This field must be UN_TOKENIZED.
            indexDoc.add(new Field(name + EXACT_SUFFIX, value, Field.Store.YES, Field.Index.UN_TOKENIZED, Field.TermVector.NO));
            
            // Store the stemmed version:
            //   This field is tokenized, and the analyzer must be configured
            //   to stem this sort of field.
            indexDoc.add(new Field(name + STEMMED_SUFFIX, value, Field.Store.YES, Field.Index.TOKENIZED, Field.TermVector.NO));
        }

    }
    
    public static void addRecordField(String name, String value, Document indexDoc) {
        indexDoc.add(new Field(name, value, Field.Store.YES, Field.Index.NO, Field.TermVector.NO));
    }
    
}
