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
package edu.indiana.dlib.catalog.pages.collections;

import org.apache.click.control.Field;
import org.apache.click.control.Form;
import org.apache.click.control.HiddenField;
import org.apache.click.control.Submit;
import org.apache.click.control.TextField;

import edu.indiana.dlib.catalog.accesscontrol.AuthorizationSystemException;
import edu.indiana.dlib.catalog.asynchronous.UserOperationManager;
import edu.indiana.dlib.catalog.batch.SearchAndReplaceOperation;
import edu.indiana.dlib.catalog.config.ConfigurationException;
import edu.indiana.dlib.catalog.config.FieldConfiguration;

/**
 * A page that exposes a search and replace action for
 * one field.  Currently the only type supported is replacement
 * of the entire field part when a given value is present.
 * 
 * Ultimately this page should present a UI that allows a user to
 * select a field, then a part then a value (with clues about how
 * many exist) then enter a replacement value. 
 */
public class SearchAndReplacePage extends CollectionPage {

    public String locationName;
    
    public String matchName;
    
    public String replacementName;

    private HiddenField fieldType;
    
    private HiddenField fieldPart;
    
    private HiddenField value;
    
    private Field replacement;
    
    /**
     * Determines whether the user can perform Search and Replace operations.
     */
    public boolean onSecurityCheck() {
        if (!super.onSecurityCheck()) {
            return false;
        } else {
            try {
                String fieldType = getContext().getRequest().getParameter("fieldType");
                return !collection.getFieldConfiguration(fieldType).isReadOnly() && !collection.newInstance(fieldType).hasDerivativeParts() && getAuthorizationManager().canManageCollection(collection, unit, user);
            } catch (AuthorizationSystemException ex) {
                throw new RuntimeException(ex);
            } catch (ConfigurationException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
    
    public void onInit() {
        super.onInit();
        String valueToReplace = getContext().getRequest().getParameter("value");
        if (valueToReplace.trim().length() == 0) {
            addModel("reason", getMessage("value-must-be-supplied"));
        } else {
            Form replaceForm = new Form("form");
            fieldType = new HiddenField("fieldType", getContext().getRequest().getParameter("fieldType"));
            replaceForm.add(fieldType);
            fieldPart = new HiddenField("fieldPart", getContext().getRequest().getParameter("fieldPart"));
            replaceForm.add(fieldPart);
            value = new HiddenField("value", valueToReplace);
            replaceForm.add(value);
            replacement = new TextField("replacement", getMessage("replacement", value.getValue(), collection.getFieldPartName(getContext().getRequest().getParameter("fieldType"), getContext().getRequest().getParameter("fieldPart"))));
            replaceForm.add(replacement);
            replaceForm.add(new Submit("replace", getMessage("replace"), this, "onReplace"));
            addControl(replaceForm);
        }
    }
    
    
    
    public boolean onReplace() {
        FieldConfiguration field = collection.getFieldConfiguration(fieldType.getValue());
        String partName = fieldPart.getValue();
        String valueToReplace = value.getValue();
        String replacementValue = replacement.getValue();
        String description = getMessage("search-and-replace");
        if (replacementValue == null || replacementValue.length() == 0) {
            description = getMessage("remove-values");
        }
        SearchAndReplaceOperation op = new SearchAndReplaceOperation(description, getSearchManager(), getItemManager(), getBatchManager(), collection, user, field, partName, valueToReplace, replacementValue);
        UserOperationManager opMan = UserOperationManager.getOperationManager(getContext().getRequest(), user.getUsername());
        opMan.queueOperation(op); 
        setRedirect("browse.htm");
        return false;
    }
    
    
}
