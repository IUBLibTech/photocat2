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
package edu.indiana.dlib.search.indexing;

/**
 * <p>
 *   An interface that encapsulates information about which 
 *   Lucene index fields are used for which purposes.  The
 *   basic structure is that for each logical field, there 
 *   are several representations stored in the index the 
 *   facilitate exact matching, stemmed matching, and sorting
 *   as well as the base field which is may be normalized.
 * </p>
 * <p>
 *   All but the base field name is meant to be completely 
 *   abstracted from users of the search system as they are
 *   an implementation detail.  The only caveat is that the 
 *   underlying alternate field names should be selected
 *   such that they won't collide with any of the exposed
 *   base names.
 * </p>
 */
public interface FieldConfiguration {

    public String[] resolveFieldName(String alias);
    
    public String getFieldNameExact(String baseFieldName);
    
    public String getFieldNameStemmed(String baseFieldName);
    
    public String getFieldNameIsPresent(String baseFieldName);
    
    public String getFieldNameSort(String baseFieldName);
    
    public String getFieldNameFacet(String baseFieldName);
    
    public String getFieldNameStored(String baseFieldName);
    
}
