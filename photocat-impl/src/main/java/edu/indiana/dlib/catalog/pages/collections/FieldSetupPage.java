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

import java.util.ArrayList;
import java.util.List;

import org.apache.click.control.Checkbox;
import org.apache.click.control.Column;
import org.apache.click.control.Form;
import org.apache.click.control.Submit;
import org.apache.click.control.TextArea;
import org.apache.click.control.TextField;
import org.apache.click.extras.control.FieldColumn;
import org.apache.click.extras.control.FormTable;
import org.apache.click.extras.control.IntegerField;
import org.apache.log4j.Logger;

import edu.indiana.dlib.catalog.accesscontrol.AuthorizationSystemException;
import edu.indiana.dlib.catalog.config.CollectionConfigurationData;
import edu.indiana.dlib.catalog.config.ConfigurationManager;
import edu.indiana.dlib.catalog.config.ConfigurationManagerException;
import edu.indiana.dlib.catalog.config.FieldConfiguration;
import edu.indiana.dlib.catalog.config.impl.DefaultCollectionConfigurationData;

public class FieldSetupPage extends CollectionAdminPage {

    public FormTable table = new FormTable();
    
    public void onInit() {
        super.onInit();
        
        ConfigurationManager cm = getConfigurationManager();
        
        table.addStyleClass("simple");
        //table.setAttribute("width", "600px");
        table.getForm().setButtonAlign(Form.ALIGN_RIGHT);
        table.getForm().setActionURL("field-setup.htm");
        
        table.addColumn(new Column("fieldType"));
        
        FieldColumn column1 = new FieldColumn("displayLabel", new TextField());
        table.addColumn(column1);
        
        FieldColumn column2 = new FieldColumn("usageNotes", new TextArea());
        table.addColumn(column2);
        
        FieldColumn column3 = new FieldColumn("repeatable", new Checkbox());
        table.addColumn(column3);
        
        IntegerField field35 = new IntegerField();
        field35.setSize(2);
        FieldColumn column35 = new FieldColumn("startingBoxes", field35);
        table.addColumn(column35);
        
        FieldColumn column4 = new FieldColumn("displayedInCatalogingBriefView", new Checkbox());
        table.addColumn(column4);
        
        IntegerField field5 = new IntegerField();
        field5.setSize(2);
        FieldColumn column5 = new FieldColumn("catalogingSortIndex", field5);
        table.addColumn(column5);
        
        FieldColumn column6 = new FieldColumn("displayedInDiscoveryBriefView", new Checkbox());
        table.addColumn(column6);
        
        FieldColumn column7 = new FieldColumn("displayedInDiscoveryFullView", new Checkbox());
        table.addColumn(column7);
        
        IntegerField field8 = new IntegerField();
        field8.setSize(2);
        FieldColumn column8 = new FieldColumn("publicSortIndex", field8);
        table.addColumn(column8);
        
        FieldColumn column9 = new FieldColumn("isPrivate", new Checkbox());
        table.addColumn(column9);
        
        // TODO: vocabulary sources
        // TODO: part names
        
        // Copy the existing configurations so we don't muck with the cached copy
        // that's shared by the entire application.
        List<FieldConfiguration> newConfigurations = new ArrayList<FieldConfiguration>();
        for (FieldConfiguration fc : collection.listFieldConfigurations(true)) {
            newConfigurations.add(new FieldConfiguration(fc));
        }
        table.setRowList(newConfigurations);
        
        table.getForm().add(new Submit("ok", "   OK   ", this, "onOkClick"));
        table.getForm().add(new Submit("cancel", this, "onCancelClick"));
    }
    
    public boolean onOkClick() {
        if (table.getForm().isValid()) {
            // TODO: perhaps save a more minimalist set of field configuration values
            //       by comparing the values with the defaults provided by the definition
            //       and only saving those that differ.  For now, we just store the
            //       values provided.
            
            // clone the configuration (so our potentially unworkable updates don't muck with the shared object)
            CollectionConfigurationData cdata = new DefaultCollectionConfigurationData(collection);
            cdata.setFieldConfigurations(table.getRowList());
            try {
                getConfigurationManager().storeConfiguration(cdata);
                getConfigurationManager().clearCache();
                setRedirect("manage-collection.htm");
                return false;
            } catch (Throwable t) {
                Logger.getLogger(getClass()).error("Error updating collection! ("+ collection.getId() + ")", t);
                if (t.getLocalizedMessage() != null) {
                    table.getForm().setError(t.getLocalizedMessage());
                } else {
                    table.getForm().setError(t.getClass().getSimpleName());
                }
                return true;
            }
        }
        return true;
    }

    public boolean onCancelClick() {
        table.setRowList(collection.getFieldConfigurations());
        table.setRenderSubmittedValues(false);
        setRedirect("manage-collection.htm");
        return false;
    }
    
}
