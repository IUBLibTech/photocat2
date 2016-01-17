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
package ORG.oclc.os.SRW.shared;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.axis.message.MessageElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;

import edu.indiana.dlib.common.utils.DomUtils;
import edu.indiana.dlib.search.highlighter.HighlighterRequestInfo;
import gov.loc.www.zing.srw.SearchRetrieveRequestType;

public class HighlighterExtensionHandler {
    public static final String SRU_HIGHLIGHTER_SEARCH_EXTENSION_SCHEMA_V1 = "http://www.dlib.indiana.edu/xml/sruHighlighter/version1.0/";

    static Log log = LogFactory.getLog(HighlighterExtensionHandler.class);
    
    public static List<HighlighterRequestInfo> parseRequest(SearchRetrieveRequestType request) {
        if (request.getExtraRequestData() == null || request.getExtraRequestData().get_any() == null) {
            return new ArrayList<HighlighterRequestInfo>(0);
        } else {
            List<HighlighterRequestInfo> requestedFieldsToHighlight = new ArrayList<HighlighterRequestInfo>();
            for (MessageElement me : request.getExtraRequestData().get_any()) {
                try {
                    Document doc = me.getAsDocument();
                    if (doc.getDocumentElement().getNamespaceURI().equals(SRU_HIGHLIGHTER_SEARCH_EXTENSION_SCHEMA_V1) && doc.getDocumentElement().getLocalName().equals("highlightMatches")) {
                        try {
                            String fields = doc.getDocumentElement().getFirstChild().getNodeValue();
                            for (String field : fields.split(" ")) {
                                log.debug("Highlighting Field: " + field);
                                requestedFieldsToHighlight.add(new HighlighterRequestInfo(field));
                            }
                        } catch (NullPointerException ex) {
                            log.debug("Invalid highlighter request!\n" + DomUtils.getXmlFromNode(doc));
                        }
                    } else {
                        
                    }
                } catch (Throwable t) {
                    log.error("Error parsing \"requestHighlightedField\"!", t);
                }
            }
            return requestedFieldsToHighlight;
        }
    }
    
    public static MessageElement createMessageElementForHighlighterRequest(List<HighlighterRequestInfo> hris) {
        QName name = new QName(SRU_HIGHLIGHTER_SEARCH_EXTENSION_SCHEMA_V1, "highlightMatches");
        MessageElement el = new MessageElement(name);
        el.setValue(HighlighterRequestInfo.getEncodedString(hris));
        return el;
    }
}
