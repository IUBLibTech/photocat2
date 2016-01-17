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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.DateTools;

public class DateAnalyzer extends Analyzer {

    public static final Log LOG = LogFactory.getLog(DateAnalyzer.class);
    
    public TokenStream tokenStream(String fieldName, Reader reader) {
        // only consider the first line because dates spanning
        // multiple lines aren't supported
        BufferedReader bReader = new BufferedReader(reader);
        try {
            return new DateTokenStream(bReader.readLine());
        } catch (IOException ex) {
            LOG.error(ex);
            return new DateTokenStream(null);
        }
    }
    
    /**
     * Parses each field as a date/time, and returns a standardized
     * representation of that value.  If the input cannot be parsed
     * no tokens are returned and an error is logged.
     */
    private static class DateTokenStream extends TokenStream {
        
        private static final SimpleDateFormat[] RECOGNIZED_DATE_FORMATS 
                = new SimpleDateFormat[] {
                        new SimpleDateFormat("yyyy-MM-dd"),
                        new SimpleDateFormat("yyyy-MM"),
                        new SimpleDateFormat("yyyy") };
        
        private String dateString;

        private boolean done;
        
        public DateTokenStream(String dateString) {
            this.dateString = dateString;
            this.done = false;
        }
        
        public Token next() throws IOException {
            if (this.done == true || dateString == null) {
                return null;
            } else {
                for (SimpleDateFormat format : RECOGNIZED_DATE_FORMATS) {
                    try {
                        Date date = format.parse(dateString);
                        String luceneDateString = DateTools.dateToString(date, DateTools.Resolution.DAY);
                        this.done = true;
                        return new Token(luceneDateString, 0, luceneDateString.length());
                    } catch (ParseException ex) {
                        LOG.debug("Unable to parse date, \"" + this.dateString + "\" with the format \"" + format.toPattern() + "\".");
                    }
                    
                }
            }
            LOG.info("Unable to parse \"" + this.dateString + "\" as a date.");
            return null;
        }
        
        public void reset() {
            this.done = false;
        }
    }

}
