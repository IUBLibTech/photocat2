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
package edu.indiana.dlib.catalog.config.impl.fedora;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;

/**
 * <p>
 *   A general purpose NamespaceContext implementation backed
 *   by a Map of prefix Strings to URI Strings.  The {@link 
 *   #setNamespace(String, String))} method may be invoked to 
 *   add prefix/namespace mappings.
 * </p>
 */
public class MapNamespaceContext implements NamespaceContext {

    private Map<String, String> prefixToUriMap;
    
    private Map<String, String> uriToSchemaLocationMap;
    
    public MapNamespaceContext() {
        this.prefixToUriMap = new HashMap<String, String>();
        this.uriToSchemaLocationMap = new HashMap<String, String>();
    }
    
    public void setNamespace(String prefix, String namespaceURI) {
        this.prefixToUriMap.put(prefix, namespaceURI);
    }
    
    public void setNamespace(String prefix, String namespaceURI, String schemaLocation) {
        this.prefixToUriMap.put(prefix, namespaceURI);
        this.uriToSchemaLocationMap.put(namespaceURI, schemaLocation);
    }
    
    public String getNamespaceURI(String prefix) {
        return this.prefixToUriMap.get(prefix);
    }

    public String getPrefix(String namespaceURI) {
        return getPrefixes(namespaceURI).next();
    }

    public Iterator<String> getPrefixes(String namespaceURI) {
        List<String> prefixes = new ArrayList<String>();
        for (String prefix : prefixToUriMap.keySet()) {
            if (prefixToUriMap.get(prefix).equals(namespaceURI)) {
                prefixes.add(prefix);
            }
        }
        return prefixes.iterator();
    }
    
    public String getSchemaLocation(String namespaceURI) {
        return this.uriToSchemaLocationMap.get(namespaceURI);
    }

}
