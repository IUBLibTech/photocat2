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
package ORG.oclc.os.SRW.shared;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Note: this class doesn't properly parse the sort keys
 * because it was too lazily implemented to consider the
 * possibility of delimiters within quotes, or escaped
 * quotes. 
 */
public class SortKeyReader {

    static Log LOG = LogFactory.getLog(SortKeyReader.class);

    /**
     * The key value defined in the CQL specification to 
     * indicate that missing values should be treated as
     * high (or appear last) in the sort order.
     */
    public static String MISSING_VALUE_HIGH = "highValue";
    
    /** 
     * The key value defined in the CQL specification to 
     * indicate that missing value should be treated as
     * low (and appear first) in the sort order.
     */
    public static String MISSING_VALUE_LOW = "lowValue";
    
    public static List<SortKey> parseSortKeys(String sortKeys) {
        if (sortKeys == null) {
            return new ArrayList<SortKey>(0);
        }
        List<SortKey> keys = new ArrayList<SortKey>();
        for (String key : sortKeys.split(" ")) {
            keys.add(new SortKey(key));
        }
        return keys;
    }
    
    public static class SortKey {
        public String path;
        public String schema;
        public boolean ascending = true;
        public boolean caseSensitive = false;
        public String missingValue = MISSING_VALUE_HIGH;
        
        public SortKey(String key) {
            String[] fields = key.split(",");
            this.path = fields[0];
            if (fields.length > 1) {
                this.schema = fields[1];
            }
            if (fields.length > 2) {
                this.ascending = fields[2].equals("1");
            }
            if (fields.length > 3) {
                this.caseSensitive = fields[3].equals("1");
            }
            if (fields.length > 4) {
                this.missingValue = fields[4];
            }
        }
       
    }
}
