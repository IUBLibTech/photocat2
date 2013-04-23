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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class VocabularySourceProperties {

    private List<String> propertyNames;
    
    private Map<String, String> properties;
    
    public VocabularySourceProperties(Node node) {
        XPath xpath = XPathHelper.getInstance().getXPath();
        try {
            // Parse out the properties
            NodeList propertiesNl = (NodeList) xpath.evaluate("u:property", node, XPathConstants.NODESET);
            properties = new HashMap<String, String>(propertiesNl.getLength());
            propertyNames = new ArrayList<String>();
            for (int i = 0; i < propertiesNl.getLength(); i ++) {
                String name = (String) xpath.evaluate("@name", propertiesNl.item(i), XPathConstants.STRING);
                propertyNames.add(name);
                properties.put(name, (String) xpath.evaluate("text()", propertiesNl.item(i), XPathConstants.STRING)); 
            }
        } catch (XPathException ex) {
            // Shouldn't happen because the xpath isn't dynamicaly generated
            throw new RuntimeException(ex);
        }     
    }
    
    public String getProperty(String name) {
        return this.properties.get(name);
    }
        
    public Collection<String> getPropertyNames() {
        return propertyNames;
    }
    
}
