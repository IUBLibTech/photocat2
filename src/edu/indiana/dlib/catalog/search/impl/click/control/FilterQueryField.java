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
package edu.indiana.dlib.catalog.search.impl.click.control;

import org.apache.click.control.Checkbox;
import org.apache.click.control.FieldSet;
import org.apache.click.control.HiddenField;
import org.apache.click.util.HtmlStringBuffer;

/**
 * A field representing a filter on a set of search results.
 * This Field only makes itself visible in the event that
 * a value has been posted.
 */
public class FilterQueryField extends FieldSet {

    private Checkbox checkbox;
    
    private HiddenField hiddenName;
    
    private HiddenField hiddenValue;
    
    public FilterQueryField(String name) {
        super(name);
        this.setShowBorder(false);
        this.setLabel(getMessage("label-filter"));
        this.setColumns(2);
        this.checkbox = new Checkbox(this.getName() + "_selected");
        this.hiddenValue = new HiddenField(this.getName() + "_value", String.class);
        this.hiddenName = new HiddenField(this.getName() + "_name", String.class);
        this.add(this.checkbox);
        this.add(this.hiddenValue);
        this.add(this.hiddenName);
    }
    
    public void setValue(String name, String query) {
        this.checkbox.setChecked(true);
        this.checkbox.setLabel(name);
        this.hiddenValue.setValue(query);
        this.hiddenName.setValue(name);
    }
    
    public String getValue() {
        if (this.checkbox.isChecked()) {
            return this.hiddenValue.getValue();
        } else {
            return null;
        }
    }
    
    public void onRender() {
        super.onRender();
        this.checkbox.setLabel(this.hiddenName.getValue());
    }
    
    public void render(HtmlStringBuffer buffer) {
        if (this.checkbox.isChecked()) {
            super.render(buffer);
        }
    }
    
    protected void renderFields(HtmlStringBuffer buffer) {
        buffer.elementStart("table");
        buffer.closeTag();
        buffer.elementStart("tr");
        buffer.closeTag();
        buffer.elementStart("td");
        buffer.closeTag();
        this.checkbox.render(buffer);
        buffer.elementEnd("td");
        buffer.elementStart("td");
        buffer.closeTag();
        if (this.checkbox.getLabel() != null) {
            buffer.append(this.checkbox.getLabel());
        }
        buffer.elementEnd("td");
        buffer.elementEnd("tr");
        buffer.elementEnd("table");
    }

}
