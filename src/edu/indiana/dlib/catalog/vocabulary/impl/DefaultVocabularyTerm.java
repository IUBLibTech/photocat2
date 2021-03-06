/*
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
package edu.indiana.dlib.catalog.vocabulary.impl;

import java.util.List;
import java.util.Map;

import edu.indiana.dlib.catalog.vocabulary.VocabularyTerm;

public class DefaultVocabularyTerm implements VocabularyTerm {

    private String id;
    private String authorityId;
    private String displayName;
    
    private List<String> propertyNames;
    
    private Map<String, List<String>> propertyValueMap;
    
    public DefaultVocabularyTerm(String id, String authorityId) {
        this(id, id, authorityId, null, null);
    }
    
    public DefaultVocabularyTerm(String id, String displayName, String authorityId, List<String> propertyNames, Map<String, List<String>> properties) {
        this.id = id;
        this.displayName = displayName;
        this.authorityId = authorityId;
        propertyValueMap = properties;
        this.propertyNames = propertyNames;
    }
    
    public DefaultVocabularyTerm(String id, String displayName, String authorityId) {
        this(id, displayName, authorityId, null, null);
    }
    
    public String getAuthorityId() {
        return this.authorityId;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public String getId() {
        return this.id;
    }

    public List<String> getPropertyNames() {
        return this.propertyNames;
    }

    public List<String> getPropertyValues(String name) {
        return this.propertyValueMap.get(name);
    }

}
