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
package edu.indiana.dlib.search.analyzers;

import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.ISOLatin1AccentFilter;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;

/**
 * An extension of BasicSRUSupportAnalyzer to allow "exact" and default
 * matches to be case insensitive and to ignore most diacritical markers.
 */
public class CaseInsensitiveSRUSupportAnalyzer extends BasicSRUSupportAnalyzer {

    public CaseInsensitiveSRUSupportAnalyzer() {
        super();
        this.defaultAnalyzer = new CaseAndDiacriticInsensitiveAnalyzerWrapper(super.defaultAnalyzer);
        this.exactAnalyzer = new CaseAndDiacriticInsensitiveAnalyzerWrapper(super.exactAnalyzer);
    }
    
    private static class CaseAndDiacriticInsensitiveAnalyzerWrapper extends Analyzer {

        private Analyzer parentAnalzyer;
        
        public CaseAndDiacriticInsensitiveAnalyzerWrapper(Analyzer parentAnalyzer) {
            this.parentAnalzyer = parentAnalyzer;
        }
        
        public TokenStream tokenStream(String fieldName, Reader reader) {
            return new LowerCaseFilter(new ISOLatin1AccentFilter(this.parentAnalzyer.tokenStream(fieldName, reader)));
        }
        
    }
    
}
