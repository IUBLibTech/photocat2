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

import edu.indiana.dlib.catalog.search.BrowseSet;

/**
 * A BrowseSet that can be used to specify a browse over
 * all of the values of a given field part.
 */
public class FieldPartBrowseSet implements BrowseSet {

    private String displayName;
    
    private String fieldType;
    
    private String partName;
    
    private boolean includeEmpty;
    
    public FieldPartBrowseSet(String name, String fieldType, String partName) {
        this(name, fieldType, partName, false);
    }
    
    public FieldPartBrowseSet(String name, String fieldType, String partName, boolean includeEmpty) {
        displayName = name;
        this.fieldType = fieldType;
        this.partName = partName;
        this.includeEmpty = includeEmpty;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getFieldType() {
        return fieldType;
    }
    
    public String getPartName() { 
        return partName;
    }
    
    public boolean includeEmptyValues() {
        return includeEmpty;
    }
    
}
