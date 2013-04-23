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
package edu.indiana.dlib.catalog.vocabulary;

import java.util.List;

/**
 * An interface that exposes access to a source of
 * controlled vocabulary.  This is typically read-only
 * access that is optimized for searching, but may also
 * allow write access, as in the case of locally
 * maintained vocabularies.
 */
public interface VocabularySource {

    /**
     * Gets the unique identifier of the vocabulary source.
     */
    public String getId();

    /**
     * Gets the name of the vocabulary source that is suitable
     * for display.
     */
    public String getDisplayName();
    
    /**
     * Gets a list of extra properties supported by this vocabulary.
     * These properties may be present on returned VocabularyTerm
     * objects from the various methods in this class.
     * @return the names of properties that may be represented on
     * the VocabularyTerm objects returned by methods to this class
     */
    public List<String> getSupportedProperties();
    
    /**
     * Gets a list of terms with the given 'prefix' that has no
     * more than 'limit' elements.
     */
    public List<VocabularyTerm> getTermsWithPrefix(String prefix, int limit, int offset);

    /**
     * Gets a sublist of all terms that is no longer than 'limit' starting 
     * at the given offset.
     */
    public List<VocabularyTerm> listAllTerms(int limit, int offset);
    
    /**
     * Gets the full VocabularyTerm with the given id.
     */
    public VocabularyTerm getTerm(String id);
    
    /**
     * @return true if at least one term from this VocabularySource whose 
     * display name equals the termName parameter exists, or false if there 
     * are no matching terms.
     */
    public boolean lookupTermByName(String termName);

    /**
     * @return true if this VocabularySource has ids that are distinct from
     * the values of the terms.  Ids should only be supported for vocabularies
     * that either have term names that are subject to change or where multiple
     * distinct terms exist with the same names.
     */
    public boolean supportsIds();
    
    /**
     * Returns the total number of terms in the vocabulary.
     */
    public int getTermCount();
    
    /**
     * Returns true if this VocabularySource is configured such that use of 
     * unlisted terms is considered valid.  The mechanism for enforcing this
     * rule may be implemented elsewhere to varying degrees of strictness.
     */
    public boolean allowUnlistedTerms();
}
