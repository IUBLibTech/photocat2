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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.FieldOption;

public class DefaultFieldConfiguration implements FieldConfiguration {

    public static final Log LOG = LogFactory.getLog(DefaultFieldConfiguration.class);
    
    private static final String INDEX_SYNONYM_PREFIX = "indexSynonym.";
    
    private static final String EXACT_SUFFIX_PROPERTY_NAME = "exactSuffix";
    
    private static final String IS_SET_SUFFIX_PROPERTY_NAME = "isSetSuffix";
    
    private static final String SORT_SUFFIX_PROPERTY_NAME = "sortSuffix";
    
    private static final String STEMMED_SUFFIX_PROPERTY_NAME = "stemmedSuffix";
    
    private static final String FACET_SUFFIX_PROPERTY_NAME = "facetSuffix";
    
    private static final String STORED_SUFFIX_PROPERTY_NAME = "storedSuffix";
    
    private static final String FIELD_LISTING_PROPERTY_NAME = "fields";
    
    private Map<String, String[]> fieldNameMap;
    
    private String exactSuffix;
    
    private String isPresentSuffix;
    
    private String sortSuffix;
    
    private String stemmedSuffix;
    
    private String facetSuffix;
    
    private String storedSuffix;

    public DefaultFieldConfiguration(Properties p) {
        this.exactSuffix = p.getProperty(EXACT_SUFFIX_PROPERTY_NAME);
        this.isPresentSuffix = p.getProperty(IS_SET_SUFFIX_PROPERTY_NAME);
        this.sortSuffix = p.getProperty(SORT_SUFFIX_PROPERTY_NAME);
        this.stemmedSuffix = p.getProperty(STEMMED_SUFFIX_PROPERTY_NAME);
        this.facetSuffix = p.getProperty(FACET_SUFFIX_PROPERTY_NAME);
        this.storedSuffix = p.getProperty(STORED_SUFFIX_PROPERTY_NAME);

        this.fieldNameMap = new HashMap<String, String[]>();
        
        Set<String> allIndices = new HashSet<String>();
        String listedFields = p.getProperty(FIELD_LISTING_PROPERTY_NAME);
        if (listedFields != null) {
            for (String fieldName : listedFields.split("(\\,)?(\\s)+")) {
                allIndices.add(fieldName);
            }
        }
        
        this.fieldNameMap.put("cql.anywhere".toLowerCase(), allIndices.toArray(new String[0]));
        this.fieldNameMap.put("cql.allIndexes".toLowerCase(), this.fieldNameMap.get("cql.anywhere"));
        this.fieldNameMap.put("cql.anyIndexes".toLowerCase(), this.fieldNameMap.get("cql.anywhere"));
        this.fieldNameMap.put("cql.serverChoice".toLowerCase(), this.fieldNameMap.get("cql.anywhere"));

        for (Object keyObj : p.keySet()) {
            String key = (String) keyObj;
            if (key.startsWith(INDEX_SYNONYM_PREFIX)) {
                String alias = key.substring(INDEX_SYNONYM_PREFIX.length());
                String[] fields = p.getProperty(key).split("(\\,)?(\\s)+");
                this.fieldNameMap.put(alias.toLowerCase(), fields);
                
            }
        }
        
        LOG.debug("Field name aliases: ");
        for (String alias : this.fieldNameMap.keySet()) {
            StringBuffer sb = new StringBuffer();
            for (String field : this.fieldNameMap.get(alias)) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(field);
            }
            
            LOG.debug("  " + alias + ": " + sb.toString());
        }
    }
    
    /**
     * A constructor that accepts Lucene IndexReaders in order to 
     * retrieve each field name.
     */
    public DefaultFieldConfiguration(Properties p, List<IndexReader> readers) {
        this.exactSuffix = p.getProperty(EXACT_SUFFIX_PROPERTY_NAME);
        this.isPresentSuffix = p.getProperty(IS_SET_SUFFIX_PROPERTY_NAME);
        this.sortSuffix = p.getProperty(SORT_SUFFIX_PROPERTY_NAME);
        this.stemmedSuffix = p.getProperty(STEMMED_SUFFIX_PROPERTY_NAME);
        this.facetSuffix = p.getProperty(FACET_SUFFIX_PROPERTY_NAME);
        this.storedSuffix = p.getProperty(STORED_SUFFIX_PROPERTY_NAME);

        this.fieldNameMap = new HashMap<String, String[]>();
        
        Set<String> allIndices = new HashSet<String>();
        for (IndexReader reader : readers) {
            for (Object nameObj : reader.getFieldNames(FieldOption.INDEXED)) {
                String name = (String) nameObj;
                if (!(this.exactSuffix != null && name.endsWith(this.exactSuffix)) 
                        && !(this.isPresentSuffix != null && name.endsWith(this.isPresentSuffix)) 
                        && !(this.sortSuffix != null && name.endsWith(this.sortSuffix)) 
                        && !(this.stemmedSuffix != null && name.endsWith(this.stemmedSuffix))
                        && !(this.storedSuffix != null && name.endsWith(this.storedSuffix))) {
                    allIndices.add(name);
                }
            }
        }
        this.fieldNameMap.put("cql.anywhere".toLowerCase(), allIndices.toArray(new String[0]));
        this.fieldNameMap.put("cql.allIndexes".toLowerCase(), this.fieldNameMap.get("cql.anywhere"));
        this.fieldNameMap.put("cql.anyIndexes".toLowerCase(), this.fieldNameMap.get("cql.anywhere"));
        this.fieldNameMap.put("cql.serverChoice".toLowerCase(), this.fieldNameMap.get("cql.anywhere"));

        for (Object keyObj : p.keySet()) {
            String key = (String) keyObj;
            if (key.startsWith(INDEX_SYNONYM_PREFIX)) {
                String alias = key.substring(INDEX_SYNONYM_PREFIX.length());
                String[] fields = p.getProperty(key).split("(\\,)?(\\s)+");
                this.fieldNameMap.put(alias.toLowerCase(), fields);
                
            }
        }
        
        LOG.debug("Field name aliases: ");
        for (String alias : this.fieldNameMap.keySet()) {
            StringBuffer sb = new StringBuffer();
            for (String field : this.fieldNameMap.get(alias)) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(field);
            }
            
            LOG.debug("  " + alias + ": " + sb.toString());
        }
    }
    
    /** 
     * A helper method that removes any known extensions from 
     * the given field name.
     */
    private String getBaseFieldName(String fieldName) {
        if (this.exactSuffix != null && fieldName.endsWith(this.exactSuffix)) {
            return fieldName.substring(0, fieldName.lastIndexOf(this.exactSuffix));
        }
        if (this.facetSuffix != null && fieldName.endsWith(this.facetSuffix)) {
            return fieldName.substring(0, fieldName.lastIndexOf(this.facetSuffix));
        }
        if (this.sortSuffix != null && fieldName.endsWith(this.sortSuffix)) {
            return fieldName.substring(0, fieldName.lastIndexOf(this.sortSuffix));
        }
        if (this.stemmedSuffix != null && fieldName.endsWith(this.stemmedSuffix)) {
            return fieldName.substring(0, fieldName.lastIndexOf(this.stemmedSuffix));
        }
        if (this.storedSuffix != null && fieldName.endsWith(this.storedSuffix)) {
            return fieldName.substring(0, fieldName.lastIndexOf(this.storedSuffix));
        }
        return fieldName;
    }
    
    public String getFieldNameExact(String fieldName) {
        String baseFieldName = this.getBaseFieldName(fieldName);
        if (this.exactSuffix == null) {
            return baseFieldName;
        }
        if (baseFieldName.endsWith(this.exactSuffix)) {
            return baseFieldName;
        }
        return baseFieldName + this.exactSuffix;
    }

    public String getFieldNameIsPresent(String fieldName) {
        String baseFieldName = this.getBaseFieldName(fieldName);
        if (this.isPresentSuffix == null) {
            return baseFieldName;
        }
        if (baseFieldName.endsWith(this.isPresentSuffix)) {
            return baseFieldName;
        }
        return baseFieldName + this.isPresentSuffix;
    }

    public String getFieldNameSort(String fieldName) {
        String baseFieldName = this.getBaseFieldName(fieldName);
        if (this.sortSuffix == null) {
            return baseFieldName;
        }
        if (baseFieldName.endsWith(this.sortSuffix)) {
            return baseFieldName;
        }
        return baseFieldName + this.sortSuffix;
    }

    public String getFieldNameStemmed(String fieldName) {
        String baseFieldName = this.getBaseFieldName(fieldName);
        if (this.stemmedSuffix == null) {
            return baseFieldName;
        }
        if (baseFieldName.endsWith(this.stemmedSuffix)) {
            return baseFieldName;
        }
        return baseFieldName + this.stemmedSuffix;
    }
    
    public String getFieldNameFacet(String fieldName) {
        String baseFieldName = this.getBaseFieldName(fieldName);
        if (this.facetSuffix == null) {
            return baseFieldName;
        }
        if (baseFieldName.endsWith(this.facetSuffix)) {
            return baseFieldName;
        }
        return baseFieldName + this.facetSuffix;
    }

    public String getFieldNameStored(String fieldName) {
        String baseFieldName = this.getBaseFieldName(fieldName);
        if (this.storedSuffix == null) {
            return baseFieldName;
        }
        if (baseFieldName.endsWith(this.storedSuffix)) {
            return baseFieldName;
        }
        return baseFieldName + this.storedSuffix;
    }
    
    public String[] resolveFieldName(String alias) {
        if (this.fieldNameMap.containsKey(alias.toLowerCase())) {
            return this.fieldNameMap.get(alias.toLowerCase());
        } else {
            return new String[] { alias };
        }
    }
}
