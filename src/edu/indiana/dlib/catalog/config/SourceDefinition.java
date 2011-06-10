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
 * Encapsulates the definition of a source from the definition
 * configuration file.
 */
public final class SourceDefinition {
    
    private String type;
    
    private JavaImplementation javaImplementation;

    private List<ParameterSpec> configurationParameters;
    
    public SourceDefinition(Node node) throws DataFormatException {
        XPath xpath = XPathHelper.getInstance().getXPath();
        
        try {
            // Parse out the type
            XPathExpression typeXpath = xpath.compile("@d:type");
            if ((Boolean) typeXpath.evaluate(node, XPathConstants.BOOLEAN)) {
                this.type = (String) typeXpath.evaluate(node, XPathConstants.STRING);
            } else {
                throw new DataFormatException("Required attribute \"type\" was not found!");
            }

            this.javaImplementation = new JavaImplementation((Node) xpath.evaluate("d:javaImplementation", node, XPathConstants.NODE));

            this.configurationParameters = new ArrayList<ParameterSpec>();
            NodeList paramNl = (NodeList) xpath.evaluate("d:configurationParameters/d:param", node, XPathConstants.NODESET);
            for (int i = 0; i < paramNl.getLength(); i ++) {
                String name = (String) xpath.evaluate("d:name", paramNl.item(i), XPathConstants.STRING);
                String meaning = (String) xpath.evaluate("d:meaning", paramNl.item(i), XPathConstants.STRING);
                this.configurationParameters.add(new ParameterSpec(name, meaning));
            }
        } catch (XPathException ex) {
            // Shouldn't happen because the xpath isn't dynamicaly generated
            throw new RuntimeException(ex);
        }
    }
    
    public String getType() {
        return this.type;
    }
    
    public JavaImplementation getJavaImplementation() {
        return this.javaImplementation;
    }

    public List<ParameterSpec> getConfigurationParameters() {
        return Collections.unmodifiableList(this.configurationParameters);
    }
    
}