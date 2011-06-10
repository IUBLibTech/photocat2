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
package edu.indiana.dlib.catalog.vocabulary.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import edu.indiana.dlib.catalog.config.SourceDefinition;
import edu.indiana.dlib.catalog.config.VocabularySourceConfiguration;
import edu.indiana.dlib.catalog.vocabulary.ManagedVocabularySource;
import edu.indiana.dlib.catalog.vocabulary.VocabularySourceManager;
import edu.indiana.dlib.catalog.vocabulary.VocabularyTerm;

/**
 * A super quick and dirty implementation of a ManagedVocabularySource.
 */
public class TextFileListVocabularySource implements ManagedVocabularySource {

    private String id;
    
    private List<String> terms;
    
    private File listFile;
    
    private String displayName;
    
    public TextFileListVocabularySource(SourceDefinition def, VocabularySourceConfiguration config, VocabularySourceManager manager) throws IOException {
        File directory = manager.getLocalVocabularyDataDirectory();
        this.id = config.getId();
        this.listFile = new File(directory, id);
        this.terms = new ArrayList<String>();
        if (listFile.exists()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(listFile)));
            String line = null;
            while ((line = reader.readLine()) != null) {
                this.terms.add(line);
            }
            reader.close();
        }
        if (config.getVocabularySourceConfig() != null) {
            this.displayName = config.getVocabularySourceConfig().getProperty("displayName");
        }
    }

    public String getId() {
        return this.id;
    }

    public synchronized List<VocabularyTerm> getTermsWithPrefix(String prefix, int limit, int offset) {
        List<VocabularyTerm> matches = new ArrayList<VocabularyTerm>();
        int currentOffset = 0;
        for (String term: this.terms) {
            if (term.toLowerCase().startsWith(prefix.toLowerCase())) {
                if (currentOffset >= offset) {
                    matches.add(new DefaultVocabularyTerm(null, term, this.getId()));
                    if (matches.size() == limit) {
                        return matches;
                    }
                }
                currentOffset ++;
            }
        }
        return matches;
        
    }

    public boolean lookupTermByName(String displayName) {
        return this.terms.contains(displayName);
    }
    

    public synchronized VocabularyTerm getTerm(String id) {
        for (String term : this.terms) {
            if (term.equals(id)) {
                return new DefaultVocabularyTerm(term, term, this.getId());
            }
        }
        return null;
    }
    
    public synchronized List<VocabularyTerm> listAllTerms(int limit, int offset) {
        List<VocabularyTerm> matches = new ArrayList<VocabularyTerm>();
        for (int i = offset; i < this.terms.size(); i ++) {
            matches.add(new DefaultVocabularyTerm(null, terms.get(i), this.getId()));
            if ((i - offset) >= limit) {
                break;
            }
        }
        return matches;
    }

    public synchronized void addTerm(VocabularyTerm term) throws IOException {
        if (!this.terms.contains(term.getDisplayName())) {
            this.terms.add(term.getDisplayName());
        }
        this.writeOutList();
    }
    
    public synchronized void addTerms(Collection<VocabularyTerm> terms) throws IOException {
        for (VocabularyTerm term : terms) {
            if (!this.terms.contains(term.getDisplayName())) {
                this.terms.add(term.getDisplayName());
            }
        }
        this.writeOutList();
    }

    public synchronized void removeTerm(VocabularyTerm term) throws IOException {
        this.terms.remove(term.getDisplayName());
        this.writeOutList();
    }
    

    public synchronized void clear() throws IOException {
        this.terms.clear();
        this.writeOutList();
    }
    
    private void writeOutList() throws IOException {
        PrintWriter writer = new PrintWriter(new FileOutputStream(this.listFile));
        for (String term : this.terms) {
            writer.println(term);
        }
        writer.close();
    }

    public String getDisplayName() {
        return (this.displayName == null ? this.id : this.displayName);
    }

    public int getTermCount() {
        return this.terms.size();
    }

    public List<String> getSupportedProperties() {
        return Collections.emptyList();
    }

    public boolean isImplicitlyMaintained() {
        return true;
    }
    
    public boolean allowUnlistedTerms() {
        return true;
    }
}
