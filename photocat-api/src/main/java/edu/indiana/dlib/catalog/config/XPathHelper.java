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

import java.util.Collections;
import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;

/**
 * A simple NamespaceContext implementation that includes
 * the relevant namespaces and schema locations with their
 * simple prefixes.
 *
 * The namespace "info:photocat/definition" is prefixed with "d".
 * The namespace "info:photocat/configuration" is prefixed with "c".
 * The namespace "info:photocat/metadata" is prefixed with "m".
 * The namespace "info:photocat/display-configuration" is prefixed with "disp"
 * The namespace "http://www.w3.org/2001/XMLSchema-instance" is prefixed with "xsi"
 */
public class XPathHelper {

    private static XPathHelper INSTANCE;
    
    /**
     * The namespace URI for the definition schema.
     */
    public static final String D_URI = "info:ico/definition";

    /**
     * The schema location for the definition schema.
     */
    public static final String D_XSD_LOC = "http://purl.dlib.indiana.edu/iudl/xml/schema/photocat/field-definition-2.xsd";
    
    /**
     * The namespace URI for the configuration schema.
     */
    public static final String C_URI = "info:photocat/configuration"; 

    /**
     * The schema location for the configuration schema.
     */
    public static final String C_XSD_LOC = "http://purl.dlib.indiana.edu/iudl/xml/schema/photocat/field-configuration.xsd";
    
    /**
     * The namespace URI for the metadata schema.
     */
    public static final String M_URI = "info:photocat/metadata";
    
    /**
     * The schema location for the metadata schema.
     */
    public static final String M_XSD_LOC = "http://purl.dlib.indiana.edu/iudl/xml/schema/photocat/field-metadata.xsd";

    /**
     * The namespace URI for item objects.
     */
    public static final String I_URI = "info:photocat/item";
    
    /**
     * The namespace URI for the display configuration schema.
     */
    public static final String DISP_URI = "info:photocat/display-configuration";
    
    /**
     * The schema location for the display configuration schema.
     */
    public static final String DISP_XSD_LOC = "http://purl.dlib.indiana.edu/iudl/xml/schema/photocat/display-configuration.xsd";

    /**
     * The namespace URI for the unified collection configuration schema.
     */
    public static final String U_URI = "info:ico/collection";
    
    /**
     * The schema location for the unified collection configuration schema.
     */
    public static final String U_XSD_LOC = "http://purl.dlib.indiana.edu/iudl/xml/schema/photocat/collection.xsd";
    
    /**
     * The namespace URI for the schema instance schema.
     */
    public static final String XSI_URI = "http://www.w3.org/2001/XMLSchema-instance";

    /**
     * Gets the instance of this XPathHelper.
     */
    public static XPathHelper getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new XPathHelper();
        }
        return INSTANCE;
    }

    /**
     * An instantiated and configured XPath instance. 
     */
    private XPath xpath;
    
    /**
     * A NamespaceContext with all the relevant namespaces configured.
     */
    private NamespaceContext nsc;

    /**
     * A constructor for an XPathHelper.  This class is meant to have
     * a single instance per application, so the constructor is not
     * public.
     */
    private XPathHelper() {
        nsc = new EmbeddedNamespaceContext();
        xpath = new net.sf.saxon.xpath.XPathFactoryImpl().newXPath();
        xpath.setNamespaceContext(nsc);
    }
    
    public synchronized XPath getXPath() {
        return xpath;
    }
    
    private static class EmbeddedNamespaceContext implements NamespaceContext {

        /**
         * Accepts "d", "c" and "m" and returns the appropriate
         * namespace URIs.
         */
        public String getNamespaceURI(String prefix) {
            if (prefix.equals("m")) {
                return M_URI;
            } else if (prefix.equals("c")) {
                return C_URI;
            } else if (prefix.equals("i")) {
                return I_URI;
            } else if (prefix.equals("d")) {
                return D_URI;
            } else if (prefix.equals("xsi")) {
                return XSI_URI;
            } else if (prefix.equals("disp")) {
                return DISP_URI;
            } else if (prefix.equals("u")) {
                return U_URI;
            } else {
                return null;
            }
        }
    
        public String getPrefix(String namespaceURI) {
            if (namespaceURI.equals(M_URI)) {
                return "m";
            } else if (namespaceURI.equals(I_URI)) {
                return "i";
            } else if (namespaceURI.equals(C_URI)) {
                return "c";
            } else if (namespaceURI.equals(D_URI)) {
                return "d";
            } else if (namespaceURI.equals(XSI_URI)) {
                return "xsi";
            } else if (namespaceURI.equals(DISP_URI)) {
               return "disp";
            } else if (namespaceURI.equals(U_URI)) {
                return "u";
            } else {
                return null;
            }
        }
    
        public Iterator getPrefixes(String namespaceURI) {
            return Collections.singletonList(this.getPrefix(namespaceURI)).iterator();
        }
        
    }

}
