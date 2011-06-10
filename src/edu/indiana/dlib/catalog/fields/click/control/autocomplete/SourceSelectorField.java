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

package edu.indiana.dlib.catalog.fields.click.control.autocomplete;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.click.control.Option;
import org.apache.click.control.Select;
import org.apache.click.util.HtmlStringBuffer;

import edu.indiana.dlib.catalog.vocabulary.VocabularySource;

public class SourceSelectorField extends Select {

    private Map<String, VocabularySource> sourceMap;

    public SourceSelectorField(String name, VocabularySource source) {
        super(name);
        this.sourceMap = new HashMap<String, VocabularySource>();
        Option option = new Option(source.getId(), source.getDisplayName());
        this.add(option);
        this.sourceMap.put(option.getValue(), source);
    }
    
    public SourceSelectorField(String name, List<VocabularySource> sources, String partName) {
        super(name);
        this.sourceMap = new HashMap<String, VocabularySource>();
        for (VocabularySource source : sources) {
            Option option = new Option(source.getId(), source.getDisplayName());
            this.add(option);
            this.sourceMap.put(option.getValue(), source);
        }
    }
    
    public void render(HtmlStringBuffer buffer) {
        if (this.sourceMap.size() > 1) {
            super.render(buffer);
        } else {
            // render as hidden
            buffer.elementStart("input");
            buffer.appendAttribute("type", "hidden");
            buffer.appendAttribute("value", this.sourceMap.values().iterator().next().getId());
            buffer.appendAttribute("id", getId());
            buffer.appendAttribute("name", getName());
            buffer.elementEnd();
        }
    }
    
    public VocabularySource getCurrentSource() {
        if (this.sourceMap.size() == 1) {
            return this.sourceMap.entrySet().iterator().next().getValue();
        } else if (getValue() != null) {
            return this.sourceMap.get(getValue());
        } else {
            return null;
        }
    }
    
}
