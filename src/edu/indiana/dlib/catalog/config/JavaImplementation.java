/**
 * Copyright 2011, Trustees of Indiana University
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *   
 *   Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *   
 *   Neither the name of Indiana University nor the names of its
 *   contributors may be used to endorse or promote products derived from this
 *   software without specific prior written permission.
 *   
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. 
 */
package edu.indiana.dlib.catalog.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpression;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A class that encapsulates the information from the field definition
 * relating to the Java Implementation of the field.
 *
 * This class is used elsewhere to provide an actual Java class
 * instance for the field implementation. 
 */
public class JavaImplementation {

    private String javaClassName;
    
    private List<NameValuePair> javaClassProperties;

    public JavaImplementation(Node node) throws DataFormatException {
        XPath xpath = XPathHelper.getInstance().getXPath();
        
        try {
            // Parse out the type
            XPathExpression javaClassNameXpath = xpath.compile("d:class");
            if ((Boolean) javaClassNameXpath.evaluate(node, XPathConstants.BOOLEAN)) {
                this.javaClassName = (String) javaClassNameXpath.evaluate(node, XPathConstants.STRING);
            } else {
                throw new DataFormatException("Required element \"javaClassName\" was not found!");
            }
            
            this.javaClassProperties = new ArrayList<NameValuePair>();
            NodeList propertiesNl = (NodeList) xpath.evaluate("d:property", node, XPathConstants.NODESET);
            for (int i = 0; i < propertiesNl.getLength(); i ++) {
                Node propertyNode = propertiesNl.item(i);
                this.javaClassProperties.add(new NameValuePair((String) xpath.evaluate("@d:name", propertyNode, XPathConstants.STRING), (String) xpath.evaluate(".", propertyNode, XPathConstants.STRING)));
            }
            
        } catch (XPathException ex) {
            // Shouldn't happen because the xpath isn't dynamicaly generated
            throw new RuntimeException(ex);
        }
    }
    
    public String getJavaClassName() {
        return this.javaClassName;
    }
    
    public List<NameValuePair> getJavaClassProperties() {
        return Collections.unmodifiableList(this.javaClassProperties);
    }
}
