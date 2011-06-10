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

import org.apache.click.Control;
import org.apache.click.control.TextField;
import org.apache.click.util.HtmlStringBuffer;

import edu.indiana.dlib.catalog.fields.click.control.AutoCompleteEnabled;
import edu.indiana.dlib.catalog.vocabulary.VocabularySource;

public class VocabularySourceAutoCompleteTextField extends TextField {

    private static final long serialVersionUID = 1L;
    
    private SourceSelectorField selector;

    private String warningStyleClass;
    
    public VocabularySourceAutoCompleteTextField(String name, SourceSelectorField selector) {
        super(name);
        this.selector = selector;
        this.warningStyleClass = "warning-message";
    }
    
    /**
     * Checks if the value entered into this field meets the suggested
     * requirements of the vocabulary source.  This means that if the
     * source returns false for allowUnlistedTerms(), and this term is
     * unlisted, this method will return false.  Otherwise it will 
     * return true.
     * @return false if the selected source disallows unlisted terms but
     * an unlisted term is entered.
     */
    public boolean isTermValid() {
        VocabularySource source = this.selector.getCurrentSource();
        if (source != null && !source.allowUnlistedTerms()) {
            String value = this.getValue();
            if (value != null && value.trim().length() > 0 && !source.lookupTermByName(value)) {
                return false;
            }
        } 
        return true;
    }
    
    /**
     * Registers this class with its surrounding AutoCompleteAjaxTargetControl. 
     */
    public void onRender() {
        super.onRender();
        AutoCompleteAjaxTargetControl target = locateTargetControl(getParent());
        if (target == null) {
            throw new IllegalStateException();
        }
        
        /**
         * The javascript method invoked by onFocus is written to be functional even
         * if copied to another element.  Because of that, the third and fourth parameters
         * are pretty tricky and need to computed here.  Basically the third parameter must
         * be a substring of this item's identifier that if replaced with the value of the
         * fourth parameter would be the id of the "source" text field.
         */
        String id = this.getId();
        String authorityId = this.getSelector().getId();
        int commonPrefixLength = 0;
        for (int i = 0; i < id.length() && i < authorityId.length(); i ++) {
            if (id.charAt(i) != authorityId.charAt(i)) {
                commonPrefixLength = i;
                break;
            }
        }
        int commonSuffixLength = 0;
        for (int i = 0; i < id.length() && i < authorityId.length(); i ++) {
            if (id.charAt(id.length() - 1 - i) != authorityId.charAt(authorityId.length() - 1 - i)) {
                commonSuffixLength = i;
                break;
            }
        }
        String token = id.substring(commonPrefixLength);
        if (commonSuffixLength > token.length()) { 
            // this replacement won't work
            token = null;
        } else {
            token = token.substring(0, token.length() - commonSuffixLength);
        }
        String replacement = authorityId.substring(commonPrefixLength);
        if (commonSuffixLength > replacement.length()) {
            replacement = "";
        } else {
            replacement = replacement.substring(0, replacement.length() - commonSuffixLength);
        }
        if (token != null) {
            this.setAttribute("onFocus", "instantiateAutocompleter(this, 'auto_complete_div_', '" + token + "', '" + replacement + "', '" + target.getName() + "', '" + target.getActionUrl() + "', " + target.getAutoCompleteOptions() + ")");
        }
    }
    
    
    public SourceSelectorField getSelector() {
        return this.selector;
    }
    
    protected String getAutoCompleteListDivId() {
        return "auto_complete_div_" + getId();
    }
    
    protected String getValidationMessageDivId() {
        return "validation_div_" + getId();
    }
    
    public void setWarningStyleClass(String styleClass) {
        this.warningStyleClass = styleClass;
    }
    
    protected String getWarningStyleClass() {
        return this.warningStyleClass;
    }
    
    /**
     * Render the HTML representation of the AutoCompleteTextField.
     *
     * @see #toString()
     *
     * @param buffer the specified buffer to render the control's output to
     */
    public void render(HtmlStringBuffer buffer) {
        super.render(buffer);

        buffer.elementStart("div");
        buffer.appendAttribute("class", "auto_complete");
        buffer.appendAttribute("id", getAutoCompleteListDivId());
        buffer.closeTag();
        buffer.elementEnd("div");
        
        buffer.elementStart("div");
        if (getWarningStyleClass() != null) {
            buffer.appendAttribute("class", getWarningStyleClass());
        }
        buffer.appendAttribute("id", getValidationMessageDivId());
        if (this.isTermValid()) {
            buffer.appendAttribute("style", "display:none;");
        }
        buffer.closeTag();
        buffer.append(getMessage("warn-term-not-in-thesaurus", this.getValue()));
        buffer.elementEnd("div");
    }
    
    protected AutoCompleteAjaxTargetControl locateTargetControl(Object o) {
        if (o == null) {
            return null;
        } else if (o instanceof AutoCompleteEnabled) {
            return ((AutoCompleteEnabled) o).getAutoCompleteAjaxTargetControl();
        } else if (o instanceof AutoCompleteAjaxTargetControl) {
            return (AutoCompleteAjaxTargetControl) o;
        } else if (o instanceof Control) {
            return locateTargetControl(((Control) o).getParent());
        } else {
            return null;
        }
    }
}
