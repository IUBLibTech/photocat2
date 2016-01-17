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

import org.apache.click.control.Form;
import org.apache.click.control.HiddenField;
import org.apache.click.control.Option;
import org.apache.click.control.Select;
import org.apache.click.control.Submit;
import org.apache.click.control.TextField;
import org.apache.click.extras.control.IntegerField;

import edu.indiana.dlib.catalog.accesscontrol.AuthorizationSystemException;

public class ExportPage extends CollectionPage {

    private static final long serialVersionUID = 1L;

    /**
     * Determines whether the user is authorized to export content
     * from this collection.  To do so, the user must be registered
     * as a collection manager, not just a cataloger.
     */
    public boolean onSecurityCheck() {
        if (!super.onSecurityCheck()) {
            return false;
        } else {
            // Determine if the user can manage that collection
            try {
                boolean allow = getAuthorizationManager().canManageCollection(collection, unit, user);
                if (!allow) {
                    setRedirect("unauthorized.htm");
                    return false;
                }
                return true;
            } catch (AuthorizationSystemException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
    
    public void onInit() {
        super.onInit();
        
        Form exportForm = new Form("export");
        exportForm.setMethod("GET");
        exportForm.setActionURL(getContext().getRequest().getContextPath() + "/export-collection.xls");
        
        exportForm.add(new HiddenField("cid", collection.getId()));
        
        IntegerField maxRecordsField = new IntegerField("maxRecords", getMessage("maxRecords"));
        maxRecordsField.setValueObject(10);
        exportForm.add(maxRecordsField);
        
        exportForm.add(new TextField("nl", getMessage("nl")));
        
        exportForm.add(new HiddenField("structured", "true"));
        //Select formatField = new Select("structured", getMessage("structured"));
        //formatField.add(new Option("true", getMessage("structured-format")));
        //formatField.add(new Option("false", getMessage("summary-format")));
        //exportForm.add(formatField);
        
        exportForm.add(new Submit("submit"));
        
        addControl(exportForm);
    }
    
}
