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

import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Encapsulates the definition of a field from the field definition
 * configuration file.
 */
public final class FieldDefinition {
    
    private Definitions def;
    
    private String type;
    
    private String meaningAndUsage;

    private JavaImplementation javaImplementation;

    private DataSpecification dataSpecification;
    
    private FieldConfiguration defaultConfiguration;
    
    public FieldDefinition(Definitions parent, Node node) throws DataFormatException {
        this.def = parent;
        XPath xpath = XPathHelper.getInstance().getXPath();
        
        try {
            // Parse out the type
            XPathExpression typeXpath = xpath.compile("@d:type");
            if ((Boolean) typeXpath.evaluate(node, XPathConstants.BOOLEAN)) {
                this.type = (String) typeXpath.evaluate(node, XPathConstants.STRING);
            } else {
                throw new DataFormatException("Required attribute \"type\" was not found!");
            }

            // Parse out the meaningAndUsage
            XPathExpression meaningAndUsageXpath = xpath.compile("d:meaningAndUsage");
            if ((Boolean) meaningAndUsageXpath.evaluate(node, XPathConstants.BOOLEAN)) {
                this.meaningAndUsage = (String) meaningAndUsageXpath.evaluate(node, XPathConstants.STRING);
            } else {
                throw new DataFormatException("Required element \"meaningAndUsage\" was not found for \"" + this.type + "\" type!");
            }

            this.javaImplementation = new JavaImplementation((Node) xpath.evaluate("d:javaImplementation", node, XPathConstants.NODE));

            this.dataSpecification = new DataSpecification((Node) xpath.evaluate("d:dataSpecification", node, XPathConstants.NODE));
            
            this.defaultConfiguration = new FieldConfiguration((Element) xpath.evaluate("d:defaultConfiguration", node, XPathConstants.NODE), type, def.getId(), 0, 0);
        } catch (XPathException ex) {
            // Shouldn't happen because the xpath isn't dynamicaly generated
            throw new RuntimeException(ex);
        }
    }
    
    public String getType() {
        return this.type;
    }
    
    public String getMeaningAndUsage() {
        return this.meaningAndUsage;
    }
    
    public JavaImplementation getJavaImplementation() {
        return this.javaImplementation;
    }
    
    public DataSpecification getDataSpecification() {
        return this.dataSpecification;
    }
    
    public FieldConfiguration getDefaultConfiguration() {
        return this.defaultConfiguration;
    }
    
    public Definitions getDefinitions() {
        return this.def;
    }
    
}