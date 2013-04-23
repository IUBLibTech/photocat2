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
package edu.indiana.dlib.catalog.search.impl;

import java.util.List;

import edu.indiana.dlib.catalog.search.BrowseSet;
/**
 * A BrowseSet that that exposes a predetermined
 * set of field, part and value combinations, each
 * of which can be used to generate a BrowseResult.
 */
public class EnumeratedBrowseSet implements BrowseSet {

    private String displayName;
    
    private List<Entry> browseValues;
    
    public EnumeratedBrowseSet(String name, List<Entry> browseValues) {
        displayName = name;
        this.browseValues = browseValues;
    }
    
    public List<Entry> getBrowseValues() {
        return browseValues;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public static class Entry {
    
        private String displayName;
        
        private String fieldType;
        
        private String partName;
        
        private String value;
        
        private List<Entry> entries;
        
        public Entry(String displayName, String fieldType, String partName, String value) {
            this.fieldType = fieldType;
            this.partName = partName;
            this.value = value;
            this.displayName = displayName;
        }
        
        public Entry(String displayName, String fieldType, String partName, List<Entry> entries) {
            this.fieldType = fieldType;
            this.partName = partName;
            this.displayName = displayName;
            this.entries = entries;
        }
        
        public String getFieldType() {
            return fieldType;
        }
        
        public String getPartName() {
            return partName;
        }
        
        public String getValue() {
            return value;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public List<Entry> getEntries() {
            return entries;
        }
    }
}
