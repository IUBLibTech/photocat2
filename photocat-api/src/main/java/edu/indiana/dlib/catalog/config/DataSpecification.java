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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpression;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * An encapsulation of the configuration data stored in the
 * "dataSpecification" portion of the field definition file.
 * 
 * This data is a loose specification of the format of the
 * data saved by a particular field.  Implementations of fields 
 * should validate this specification vs their actual implementation.
 * 
 * TODO: this class, and the underlying schema could be updated
 * to include typing of data as well as simple specification of
 * structure and meaning.
 */
public class DataSpecification {

    private List<String> nameList;
    
    private Map<String, String> nameToMeaningMap;
    
    private List<String> attributeList;
    
    private Map<String, String> attributeToMeaningMap;
    
    public DataSpecification(Node node) throws DataFormatException {
        XPath xpath = XPathHelper.getInstance().getXPath();
        
        try {
            XPathExpression nameExpression = xpath.compile("d:name");
            XPathExpression meaningExpression = xpath.compile("d:meaning");
            
            this.attributeList = new ArrayList<String>();
            this.attributeToMeaningMap = new HashMap<String, String>();
            
            NodeList attributeNl = (NodeList) xpath.evaluate("d:attribute", node, XPathConstants.NODESET);
            for (int i = 0; i < attributeNl.getLength(); i ++) {
                Node propertyNode = attributeNl.item(i);
                String name = (String) nameExpression.evaluate(propertyNode, XPathConstants.STRING);
                String meaning = (String) meaningExpression.evaluate(propertyNode, XPathConstants.STRING);
                this.attributeList.add(name);
                this.attributeToMeaningMap.put(name, meaning);
            }
            
            this.nameList = new ArrayList<String>();
            this.nameToMeaningMap = new HashMap<String, String>();
            
            NodeList partNl = (NodeList) xpath.evaluate("d:value/d:part", node, XPathConstants.NODESET);
            for (int i = 0; i < partNl.getLength(); i ++) {
                Node propertyNode = partNl.item(i);
                String name = (String) nameExpression.evaluate(propertyNode, XPathConstants.STRING);
                String meaning = (String) meaningExpression.evaluate(propertyNode, XPathConstants.STRING);
                this.nameList.add(name);
                this.nameToMeaningMap.put(name, meaning);
            }
            
        } catch (XPathException ex) {
            // Shouldn't happen because the xpath isn't dynamicaly generated
            throw new RuntimeException(ex);
        }
    }
    
    public List<String> getValidAttributeNames() {
        return Collections.unmodifiableList(this.attributeList);
    }
    
    public String getAttributeMeaning(String attributeName) {
        return this.attributeToMeaningMap.get(attributeName);
    }
    
    public List<String> getValidPartNames() {
        return Collections.unmodifiableList(this.nameList);
    }
    
    public String getPartMeaning(String partName) {
        return this.nameToMeaningMap.get(partName);
    }
    
}
