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
package edu.indiana.dlib.catalog.vocabulary.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.indiana.dlib.catalog.config.SourceDefinition;
import edu.indiana.dlib.catalog.config.VocabularySourceConfiguration;
import edu.indiana.dlib.catalog.vocabulary.VocabularySource;
import edu.indiana.dlib.catalog.vocabulary.VocabularySourceManager;
import edu.indiana.dlib.catalog.vocabulary.VocabularyTerm;

public class EmbeddedListVocabularySource implements VocabularySource {

    private VocabularySourceConfiguration config;
    
    private String[] labels;
    
    private String[] values;
    
    private String displayName;
    
    public EmbeddedListVocabularySource(SourceDefinition def, VocabularySourceConfiguration config, VocabularySourceManager manager, String collectionId) throws VocabularySourceInitializationException {
        this.config = config;
        
        String delimiter = config.getVocabularySourceConfig().getProperty("delimiter");
        if (delimiter == null) {
            delimiter = ",";
        }
        
        String delimiterSeparatedList = config.getVocabularySourceConfig().getProperty("values");
        if (delimiterSeparatedList == null) {
            throw new VocabularySourceInitializationException("Required property, \"values\" was not provided for " + this.getClass().getName() + " " + def.getType() + "!");
        }
        values = delimiterSeparatedList.split(delimiter);
        
        String labelListString = config.getVocabularySourceConfig().getProperty("labels");
        if (labelListString != null) {
            labels = labelListString.split(delimiter);
            if (labels.length != values.length) {
                // TODO: log this
                labels = null;
            }
        }
        
        displayName = config.getVocabularySourceConfig().getProperty("displayName");
    }

    public boolean lookupTermByName(String displayName) {
        if (labels != null) {
            for (String value : labels) {
                if (value.equals(displayName)) {
                    return true;
                }
            }
        } else {
            for (String value : values) {
                if (value.equals(displayName)) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<VocabularyTerm> getTermsWithPrefix(String prefix, int limit, int offset) {
        List<VocabularyTerm> matches = new ArrayList<VocabularyTerm>();
        int currentOffset = 0;
        if (labels != null) {
            for (int i = 0; i < labels.length; i ++) {
                String label = labels[i];
                if (label.toLowerCase().startsWith(prefix.toLowerCase())) {
                    if (currentOffset >= offset) {
                        matches.add(new DefaultVocabularyTerm(values[i], label, config.getId()));
                        if (matches.size() == limit) {
                            return matches;
                        }
                    } 
                    currentOffset ++;
                }
            }
        } else {
            for (String term : values) {
                if (term.toLowerCase().startsWith(prefix.toLowerCase())) {
                    if (currentOffset >= offset) {
                        matches.add(new DefaultVocabularyTerm(term, term, config.getId()));
                        if (matches.size() == limit) {
                            return matches;
                        }
                    } 
                    currentOffset ++;
                }
            }
        }
        return matches;
    }

    public List<VocabularyTerm> listAllTerms(int limit, int offset) {
        List<VocabularyTerm> matches = new ArrayList<VocabularyTerm>();
        for (int i = 0; (i + offset) < values.length && i < limit; i ++) {
            matches.add(new DefaultVocabularyTerm(values[i + offset], labels != null ? labels[i + offset] : values[i + offset], this.config.getId()));
        }
        return matches;
    }


    public String getDisplayName() {
        return displayName == null ? config.getId() : displayName;
    }


    public String getId() {
        return config.getId();
    }


    public int getTermCount() {
        return values.length;
    }

    /**
     * The current implementation returns false.
     */
    public boolean allowUnlistedTerms() {
        return false;
    }

    public List<String> getSupportedProperties() {
        return Collections.emptyList();
    }

    public VocabularyTerm getTerm(String id) {
        for (int i = 0; i < values.length; i ++) {
            String value = values[i];
            if (value.equals(id)) {
                return new DefaultVocabularyTerm(value, labels != null ? labels[i] : value, this.config.getId());
            }
        }
        return null;
    }

    public boolean supportsIds() {
        return labels != null;
    }
}
