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

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpression;

import org.w3c.dom.Node;

/**
 *A class that encapsulates the configuration information for a
 * controlled vocabulary source.  A controlled vocabulary source is
 * a list of terms that is explicitly maintained by an authority.
 * In many cases that authority is external to the user base of the
 * cataloging application or even its hosting institution and 
 * therefore has no mechanism to add missing terms.
 *
 *
 * A class that encapsulates the configuration information for an
 * "uncontrolled" vocabulary source.  An "uncontrolled" source is
 * a list of terms that is implicitly maintained (though may be 
 * able to be explicitly maintained as well) for a field.  There's 
 * no requirement that an entered value come from the list of terms
 * but the list is used as a helpful starting point.  At its simplest
 * an "uncontrolled" source is a list of all previously entered 
 * values that can simplify the use case where common values are
 * repeated over and over again.
 * 
 * Uncontrolled lists may be subject to changes by any user under
 * any circumstances and makes no guarantees about the quality or
 * persistence of any values.  Unlike a "controlled" source there 
 * is no overarching authority.
 *
 */
public class VocabularySourceConfiguration {

    private String type;
    
    private String id;
    
    private String authorityBinding;
    
    private String valueBinding;
    
    private VocabularySourceProperties vocabularySourceConfig;
    
    public VocabularySourceConfiguration(Node node) throws DataFormatException {
        XPath xpath = XPathHelper.getInstance().getXPath();
        try {
            // Parse out the type
            XPathExpression typeXpath = xpath.compile("@type");
            if ((Boolean) typeXpath.evaluate(node, XPathConstants.BOOLEAN)) {
                this.type = (String) typeXpath.evaluate(node, XPathConstants.STRING);
            } else {
                throw new DataFormatException("Required attribute \"type\" was not found!");
            }
            
            // Parse out the id
            XPathExpression idXpath = xpath.compile("@id");
            if ((Boolean) idXpath.evaluate(node, XPathConstants.BOOLEAN)) {
                this.id = (String) idXpath.evaluate(node, XPathConstants.STRING);
            } else {
                throw new DataFormatException("Required attribute \"id\" was not found!");
            }
            
            // parse out the authorityBinding if present
            XPathExpression authorityBindingXpath = xpath.compile("u:authorityBinding");
            if ((Boolean) authorityBindingXpath.evaluate(node, XPathConstants.BOOLEAN)) {
                this.authorityBinding = (String) authorityBindingXpath.evaluate(node, XPathConstants.STRING);
            }
            
            // parse out the valueBinding
            XPathExpression valueBindingXpath = xpath.compile("u:valueBinding");
            if ((Boolean) valueBindingXpath.evaluate(node, XPathConstants.BOOLEAN)) {
                this.valueBinding = (String) valueBindingXpath.evaluate(node, XPathConstants.STRING);
            } else {
                throw new DataFormatException("Required element \"valueBinding\" was not found!");
            }
            
            // parse out the vocabularySourceConfig if present
            XPathExpression vocabularySourceConfigXpath = xpath.compile("u:sourceConfig");
            if ((Boolean) vocabularySourceConfigXpath.evaluate(node, XPathConstants.BOOLEAN)) {
                this.vocabularySourceConfig = new VocabularySourceProperties((Node) vocabularySourceConfigXpath.evaluate(node, XPathConstants.NODE));
            }
            
            
        } catch (XPathException ex) {
            // Shouldn't happen because the xpath isn't dynamicaly generated
            throw new RuntimeException(ex);
        }
    }
    
    public String getType() {
        return this.type;
    }
    
    public String getId() {
        return this.id;
    }
    
    public String getAuthorityBinding() {
        return this.authorityBinding;
    }
    
    public String getValueBinding() {
        return this.valueBinding;
    }
    
    public VocabularySourceProperties getVocabularySourceConfig() {
        return this.vocabularySourceConfig;
    }
}
