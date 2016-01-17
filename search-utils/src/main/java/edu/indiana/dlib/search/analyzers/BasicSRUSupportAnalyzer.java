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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.ISOLatin1AccentFilter;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.snowball.SnowballAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

/**
 * <p>
 *   An analyzer that handles special fields for the purpose of
 *   supporting various CQL syntactical features.  These features
 *   include exact matching, stemmed matching, and some facet
 *   support.
 * </p>
 * <p>
 *   Specifically this class uses a separate analyzer for the 
 *   different types of fields, identified by their suffix.
 * </p>
 */
public class BasicSRUSupportAnalyzer extends Analyzer {

    public static final Log LOG = LogFactory.getLog(BasicSRUSupportAnalyzer.class);
    
    protected Analyzer defaultAnalyzer;
    
    protected String exactSuffix;

    protected Analyzer facetAnalyzer;
    
    protected String facetSuffix;
    
    protected Analyzer exactAnalyzer;
    
    protected String stemSuffix;
    
    protected Analyzer stemmingAnalyzer;
    
    protected Analyzer dateAnalyzer;
    
    protected List<String> untokenizedControlFields;

    //protected Pattern dateFieldPattern;
    
    public BasicSRUSupportAnalyzer() {
        this.defaultAnalyzer = new KeywordFieldAnalyzer();
        this.exactSuffix = ".exact";
        this.exactAnalyzer = new KeywordAnalyzer();
        this.facetSuffix = ".facet";
        this.facetAnalyzer = new KeywordAnalyzer();
        this.stemSuffix = ".stemmed";
        this.stemmingAnalyzer = new SnowballAnalyzer("English", StopAnalyzer.ENGLISH_STOP_WORDS);
        this.untokenizedControlFields = new ArrayList<String>();
        //this.dateFieldPattern = Pattern.compile(".*date$");
        this.dateAnalyzer = new DateAnalyzer();
    }
    
    protected Analyzer getAnalyzerForField(String fieldName) {
        for (String untokenizedField : this.untokenizedControlFields) {
            if (fieldName.equals(untokenizedField)) {
                LOG.debug(fieldName + ": " + this.exactAnalyzer.getClass().getName());
                return this.exactAnalyzer;
            }
        }
        //if (this.dateFieldPattern.matcher(fieldName).matches()) {
        //    LOG.debug(fieldName + ": " + this.dateAnalyzer.getClass().getName());
        //    return this.dateAnalyzer;
        //}
        if (fieldName.endsWith(this.exactSuffix)) {
            LOG.debug(fieldName + ": " + this.exactAnalyzer.getClass().getName());
            return this.exactAnalyzer;
        }
        if (fieldName.endsWith(this.stemSuffix)) {
            LOG.debug(fieldName + ": " + this.stemmingAnalyzer.getClass().getName());
            return this.stemmingAnalyzer;
        }
        if (fieldName.endsWith(this.facetSuffix)) {
            LOG.debug(fieldName + ": " + this.facetAnalyzer.getClass().getName());
            return this.facetAnalyzer;
        }
        LOG.debug(fieldName + ": " + this.defaultAnalyzer.getClass().getName());
        return this.defaultAnalyzer;
    }
    
    public TokenStream tokenStream(String fieldName, Reader reader) {
        return this.getAnalyzerForField(fieldName).tokenStream(fieldName, reader);
    }
    
    /**
     * An extension of the {@link StandardAnalyzer} that further
     * filters using the {@link ISOLatin1AccentFilter} to remove
     * accents.
     */
    private static class KeywordFieldAnalyzer extends StandardAnalyzer {
        public TokenStream tokenStream(String fieldName, Reader reader) {
            return new ISOLatin1AccentFilter(super.tokenStream(fieldName, reader));
        }
    }

}
