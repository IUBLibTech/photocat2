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
package edu.indiana.dlib.catalog.index;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.util.Version;

/**
 * <p>
 *   An analyzer that handles special fields for the purpose of
 *   supporting various search features.  These features
 *   include exact matching, stemmed matching, and some facet
 *   support.
 * </p>
 * <p>
 *   Specifically this class uses a separate analyzer for the 
 *   different types of fields, identified by their suffix.
 * </p>
 */
public class BasicSupportAnalyzer extends Analyzer {

    protected Analyzer defaultAnalyzer;
    
    protected Analyzer facetAnalyzer;
    
    protected Analyzer exactAnalyzer;
    
    protected Analyzer stemmingAnalyzer;
    
    protected List<String> untokenizedControlFields;
    
    public BasicSupportAnalyzer() {
        defaultAnalyzer = new EnglishAnalyzer(Version.LUCENE_32);
        exactAnalyzer = new KeywordAnalyzer();
        facetAnalyzer = new KeywordAnalyzer();
        stemmingAnalyzer = new EnglishAnalyzer(Version.LUCENE_32);
        untokenizedControlFields = new ArrayList<String>();
    }
    
    protected Analyzer getAnalyzerForField(String fieldName) {
        for (String untokenizedField : this.untokenizedControlFields) {
            if (fieldName.equals(untokenizedField)) {
                return this.exactAnalyzer;
            }
        }
        if (fieldName.endsWith(getExactSuffix())) {
            return this.exactAnalyzer;
        }
        if (fieldName.endsWith(getStemSuffix())) {
            return this.stemmingAnalyzer;
        }
        if (fieldName.endsWith(getFacetSuffix())) {
            return this.facetAnalyzer;
        }
        return this.defaultAnalyzer;
    }
    
    public String getExactSuffix() {
        return ".exact";
    }
    
    public String getStemSuffix() {
        return ".stem";
    }
    
    public String getFacetSuffix() {
        return ".facet";
    }
    
    public TokenStream tokenStream(String fieldName, Reader reader) {
        return getAnalyzerForField(fieldName).tokenStream(fieldName, reader);
    }
}
