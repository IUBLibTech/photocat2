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
package edu.indiana.dlib.fedoraindexer.server.index;

import java.io.ByteArrayInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Stack;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import edu.indiana.dlib.fedoraindexer.server.FedoraObjectAdministrativeMetadata;
import edu.indiana.dlib.fedoraindexer.server.IndexInitializationException;

/**
 * <p>
 *   A general purpose (proof of concept) Index for the full text of 
 *   a paged object (book, journal, etc.).  One document is stored 
 *   containing the full text of the book in multiple "fulltext" fields.
 *   Another document exists for each page.  A typical use case would 
 *   be to search against the full text and then, in order to provide
 *   matches in context, search against the pages.
 * </p> 
 */
public class TEIPageBreakIndex extends CompoundObjectLuceneIndex {
    
    /**
     * The URL to fetch the TEI document for a given object.  The
     * string %PID% will be replaced with the actual PID.
     */
    private String teiUrlPattern = "http://fedora.dlib.indiana.edu:8080/fedora/get/%PID%/ENC_TEXT";
    
    private DocumentBuilder documentBuilder;
    
    private XMLInputFactory factory = XMLInputFactory.newInstance();
    
    public TEIPageBreakIndex(Properties config) throws IndexInitializationException {
        super(config);
        
        // set up the document builder
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            this.documentBuilder = dbf.newDocumentBuilder();
            this.documentBuilder.setEntityResolver(new EntityResolver() {
                public InputSource resolveEntity(java.lang.String publicId, java.lang.String systemId)
                       throws SAXException, java.io.IOException
                {
                  if (systemId != null && systemId.endsWith("imh_issue.dtd")) {
                    // this deactivates the imh dtd.
                    return new InputSource(new ByteArrayInputStream("<?xml version='1.0' encoding='UTF-8'?>".getBytes()));
                  } else  {
                      return null;
                  }
                }
            });
        } catch (ParserConfigurationException e1) {
            throw new IndexInitializationException("Unable to initialize a DocumentBuilder!");
        }
    }

    /**
     * <p>
     *   Fetches the TEI document for the object represented by the
     *   administrative metadata.
     * <p>
     *   Steps through all of the tags within the TEI document until 
     *   a pagebreak (&lt;pb&gt;) tag is encountered at which point it
     *   starts creating fields to be indexed for the contents of the
     *   page.  When the next page break is reached, the document for
     *   the previous page is completed and added to the list of
     *   documents to be returned and the process is repeated.
     * </p>
     */
    protected List<Document> createIndexDocuments(FedoraObjectAdministrativeMetadata adminData) throws Exception {
        // fetch the TEI document
        HttpURLConnection conn = null;
        long start = 0;
        long end = 0;
        BookSummary bs = null;

        /* DOM Implementation */
        /*
        conn = (HttpURLConnection) (new URL(this.teiUrlPattern.replace("%PID%", adminData.getPid()))).openConnection();
        bs = new BookSummary();
        start = System.currentTimeMillis();
        org.w3c.dom.Document teiDom = this.documentBuilder.parse(new InputSource(conn.getInputStream()));
        indexNextNode(teiDom.getDocumentElement(), bs);
        end = System.currentTimeMillis();
        System.out.println("DOM Implementation: " + (end - start) + "ms.");
        System.out.println("Heap Size: " + Runtime.getRuntime().totalMemory());
        */
        
        /* StAX Implementation */
        bs = new BookSummary();
        conn = (HttpURLConnection) (new URL(this.teiUrlPattern.replace("%PID%", adminData.getPid()))).openConnection();
        if (conn.getResponseCode() == 200) {
            start = System.currentTimeMillis();
            XMLStreamReader parser = this.factory.createXMLStreamReader(conn.getInputStream());
            processXML(bs, parser);
            parser.close();
            end = System.currentTimeMillis();
            System.out.println("StAX Implementation: " + (end - start) + "ms.");
            System.out.println("Heap Size: " + Runtime.getRuntime().totalMemory());
        }
        return bs.getDocuments();
    }

    private void processXML(BookSummary bs, XMLStreamReader parser) throws XMLStreamException {
        while (parser.hasNext()) {
            int eventType = parser.next();
            switch (eventType) {
                case XMLStreamReader.CHARACTERS:
                    bs.addText(parser.getText());
                    break;
                case XMLStreamReader.START_ELEMENT:
                    bs.startTag(parser);
                    break;
                case XMLStreamReader.END_ELEMENT:
                    bs.endTag(parser);
                    break;
                default:
                    break;
            }
        }
    }
    
    /**
     * A recursive method that goes through an XML document parsing
     * out all of the text and constructing lucene index Document
     * objects for the entire TEI as well as one for each page.
     * @param node
     */
    private void indexNextNode(Node node, BookSummary bs) {
        if (node instanceof Element && ((Element) node).getNodeName().equalsIgnoreCase("pb")) {
            // Page Break detected
            Element el = (Element) node;
            bs.startPage(el.getAttribute("id"));
        }
        if (node.getNodeType() == Node.TEXT_NODE) {
            bs.addText(node.getNodeValue());
        }
        String articleId = null;
        if (node instanceof Element && ((Element) node).getNodeName().equalsIgnoreCase("div") && "scholarlyArticle".equalsIgnoreCase(((Element) node).getAttribute("type"))) {
            // Article detected
            articleId = ((Element) node).getAttribute("id");
            bs.startArticle(articleId);
        }
        NodeList childNodeList = node.getChildNodes();
        for (int i = 0; i < childNodeList.getLength(); i ++) {
            indexNextNode(childNodeList.item(i), bs);
        }
        if (articleId != null) {
            bs.endArticle();
        }
    }
    
    /**
     * A(n increasingly) general parser state for a marked-up full-text
     * document.
     */
    private static class BookSummary {
        
        private static final String FULL_TEXT_INDEX_NAME = "fulltext";
        private static final String ID_INDEX_NAME = "id";
        private static final String ARTICLE_ID_INDEX_NAME = "articleId";
        private static final String AUTHOR_INDEX_NAME = "author";
        private static final String TITLE_INDEX_NAME = "title";
        private static final String TYPE_INDEX_NAME = "type";

        private static final String ARTICLE_TYPE = "article";
        private static final String PAGE_TYPE = "page";
        private static final String BOOK_TYPE = "book";
        
        private List<Document> documents;
        private Document bookDocument;
        private Document currentPageDocument;
        private Document currentArticle;
        private int articleStackDepth;

        private Stack<QName> currentXPath;
        
        private StringBuffer currentBatchOfText;
        private StringBuffer currentTitle;
        private StringBuffer currentByLine;
        
        private boolean concatenate;
        
        public BookSummary() {
            this.documents = new LinkedList<Document>();
            this.bookDocument = null;
            this.currentPageDocument = null;
            this.currentArticle = null;
            this.concatenate = true;
            this.currentBatchOfText = new StringBuffer();
            this.currentXPath = new Stack<QName>();
        }

        public void startPage(String id) {
            this.flushText();
            if (this.bookDocument == null) {
                this.bookDocument = new Document();
                this.bookDocument.add(new Field(TYPE_INDEX_NAME, BOOK_TYPE, Field.Store.YES, Field.Index.UN_TOKENIZED, Field.TermVector.NO));
                this.documents.add(this.bookDocument);
            }
            this.currentPageDocument = new Document();
            this.currentPageDocument.add(new Field(TYPE_INDEX_NAME, PAGE_TYPE, Field.Store.YES, Field.Index.UN_TOKENIZED, Field.TermVector.NO));
            this.currentPageDocument.add(new Field(ID_INDEX_NAME, id, Field.Store.YES, Field.Index.UN_TOKENIZED, Field.TermVector.NO));
            this.documents.add(this.currentPageDocument);
        }

        public void startTag(XMLStreamReader reader) {
            this.currentXPath.push(reader.getName());
            if (reader.getLocalName().equals("pb")) {
                String id = reader.getAttributeValue(null, "id");
                this.startPage(id);
            } else if (reader.getLocalName().equals("div") && "scholarlyArticle".equalsIgnoreCase(reader.getAttributeValue(null, "type"))) {
                String id = reader.getAttributeValue(null, "id");
                this.startArticle(id);
                this.articleStackDepth = this.currentXPath.size();
            } else if (this.currentArticle != null && reader.getLocalName().equals("head")) {
                this.startArticleTitle();
            } else if (this.currentArticle != null && reader.getLocalName().equals("byline")) {
                this.startByLine();
            }
        }
        
        public void endTag(XMLStreamReader reader) {
            if (this.currentArticle != null && currentXPath.size() == this.articleStackDepth) {
                this.endArticle();
            }
            QName last = this.currentXPath.pop();
            if (!last.equals(reader.getName())) {
                throw new IllegalStateException();
            }
            if (this.currentTitle != null && reader.getLocalName().equals("head")) {
                this.endArticleTitle();
            } else if (this.currentByLine != null && reader.getLocalName().equals("byline")) {
                this.endByLine();
            }
        }
        
        public void startByLine() {
            this.currentByLine = new StringBuffer();
        }
        
        public void endByLine() {
            if (this.currentArticle != null) {
                this.currentArticle.add(new Field(AUTHOR_INDEX_NAME, this.currentByLine.toString(), Field.Store.YES, Field.Index.UN_TOKENIZED, Field.TermVector.NO));
            } else {
                // parsing error
            }
            this.currentByLine = null;
        }
        
        public void startArticleTitle() {
            this.currentTitle = new StringBuffer();
        }
        
        public void endArticleTitle() {
            if (this.currentArticle != null) {
                this.currentArticle.add(new Field(TITLE_INDEX_NAME, this.currentTitle.toString(), Field.Store.YES, Field.Index.UN_TOKENIZED, Field.TermVector.NO));
            } else {
                // parsing error
            }
            this.currentTitle = null;
        }
        
        public void startArticle(String id) {
            this.flushText();
            this.currentArticle = new Document();
            this.currentArticle.add(new Field(TYPE_INDEX_NAME, ARTICLE_TYPE, Field.Store.YES, Field.Index.UN_TOKENIZED, Field.TermVector.NO));
            this.currentArticle.add(new Field(ID_INDEX_NAME, id, Field.Store.YES, Field.Index.UN_TOKENIZED, Field.TermVector.NO));
            this.documents.add(this.currentArticle);
        }
        
        public void endArticle() {
            this.flushText();
            this.currentArticle = null;
        }
        
        public void addText(String text) {
            if (this.currentTitle != null) {
                this.currentTitle.append(text);
            }
            if (this.currentByLine != null) {
                this.currentByLine.append(text);
            }
            if (this.concatenate) {
                this.currentBatchOfText.append(text);
            } else {
                this.addDocumentsForText(text);
            }
        }
        
        private void flushText() {
            if (this.concatenate && this.currentBatchOfText != null) {
                String text = this.currentBatchOfText.toString();
                this.currentBatchOfText = new StringBuffer();
                this.addDocumentsForText(text);
            } else {
                // do nothing...
            }
        }
        
        private void addDocumentsForText(String text) {
            if (this.currentPageDocument != null) {
                if (this.currentArticle != null) {
                    this.currentPageDocument.add(new Field(ARTICLE_ID_INDEX_NAME, this.currentArticle.getField(ID_INDEX_NAME).stringValue(), Field.Store.YES, Field.Index.UN_TOKENIZED, Field.TermVector.NO));
                }
                this.currentPageDocument.add(new Field(FULL_TEXT_INDEX_NAME, text, Field.Store.YES, Field.Index.TOKENIZED, Field.TermVector.WITH_POSITIONS_OFFSETS));
            }
            if (this.currentArticle != null) {
                this.currentArticle.add(new Field(FULL_TEXT_INDEX_NAME, text, Field.Store.NO, Field.Index.TOKENIZED, Field.TermVector.WITH_POSITIONS_OFFSETS));
            }
            if (this.bookDocument != null) {
                this.bookDocument.add(new Field(FULL_TEXT_INDEX_NAME, text, Field.Store.NO, Field.Index.TOKENIZED, Field.TermVector.WITH_POSITIONS_OFFSETS));
            }
        }

        public List<Document> getDocuments() {
            return this.documents;
        }
        
    }
}
