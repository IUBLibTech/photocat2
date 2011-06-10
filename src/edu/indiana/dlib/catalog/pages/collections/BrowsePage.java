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
package edu.indiana.dlib.catalog.pages.collections;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.click.control.ActionLink;
import org.apache.click.control.Column;
import org.apache.click.control.Table;
import org.apache.click.element.CssImport;
import org.apache.click.element.Element;
import org.apache.click.extras.control.LinkDecorator;

import edu.indiana.dlib.catalog.config.FieldConfiguration;
import edu.indiana.dlib.catalog.config.FieldDefinition;
import edu.indiana.dlib.catalog.fields.click.control.RelativeActionLink;
import edu.indiana.dlib.catalog.search.BrowseResults;
import edu.indiana.dlib.catalog.search.impl.DefaultBrowseQuery;
import edu.indiana.dlib.catalog.search.impl.click.control.BrowseTable;

public class BrowsePage extends CollectionPage {
    
    public Table browseTable;
    
    private String fieldType;
    
    private String fieldPartName;
    
    public String resultHeading;
    
    public List<LabelTypePart> labelTypePartList;
    
    public void onInit() {
        super.onInit();
        this.fieldType = getContext().getRequestParameter("fieldType");
        this.fieldPartName = getContext().getRequestParameter("fieldPartName");
        
        labelTypePartList = new ArrayList<LabelTypePart>();
        for (FieldConfiguration conf : collection.listFieldConfigurations()) {
            FieldDefinition def = collection.getFieldDefinition(conf.getFieldType());
            for (String part : def.getDataSpecification().getValidPartNames()) {
                String label = (def.getDataSpecification().getValidPartNames().size() == 1 ? conf.getDisplayLabel() : conf.getDisplayLabel() + " (" + conf.getPartDisplayLabel(part) + ")");
                labelTypePartList.add(new LabelTypePart(label, def.getType(), part));
            }
        }
        if (this.fieldType != null && this.fieldPartName != null) {
            FieldDefinition def = collection.getFieldDefinition(fieldType);
            FieldConfiguration fieldConf = collection.getFieldConfiguration(fieldType);
            if (def != null && fieldConf != null) {
                try {
                    BrowseResults results = getSearchManager().browse(new DefaultBrowseQuery(collection.getId(), fieldType, fieldPartName, 0, 2500));
                    this.browseTable = new BrowseTable("browseTable", results);
                    this.browseTable.setPageSize(25);
                    this.browseTable.getControlLink().setParameter("fieldType", fieldType);
                    this.browseTable.getControlLink().setParameter("fieldPartName", fieldPartName);
                    
                    ActionLink viewRecordsActionLink = new RelativeActionLink("viewRecords", getMessage("view-records"), this, "onViewRecords");
                    viewRecordsActionLink.setParameter("fieldType", fieldType);
                    viewRecordsActionLink.setParameter("fieldPartName", fieldPartName);
                    this.addControl(viewRecordsActionLink);
                    Column column = new Column(getMessage("view-records")); 
                    column.setDecorator(new LinkDecorator(this.browseTable, viewRecordsActionLink, "fieldValue")); 
                    column.setSortable(false); 
                    this.browseTable.addColumn(column); 
                    
                    this.addControl(browseTable);
                    if (def.getDataSpecification().getValidPartNames().size() == 1) {
                        this.resultHeading = getMessage("results-heading-only-part", fieldConf.getDisplayLabel());
                    } else {
                        this.resultHeading = getMessage("results-heading", fieldType, fieldConf.getDisplayLabel());
                    }
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
            }
        }
    }
    
    public boolean onViewRecords() {
        String type = this.fieldType;
        String partName = this.fieldPartName;
        String value = getContext().getRequestParameter("value");
        String filterName = this.collection.getFieldConfiguration(type).getDisplayLabel() + ": " + value; 
        try {
            this.setRedirect("search.htm?form_name=searchForm&filter_selected=on&filter_value=" + URLEncoder.encode(getSearchManager().getPartExactMatchQueryClause(type, partName, value), "UTF-8") + "\"&filter_name=" + URLEncoder.encode(filterName, "UTF-8") + "&pageSize=20&action=Search");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
        return true;
    }
    
    public List<Element> getHeadElements() { 
        // We use lazy loading to ensure the CSS import is only added the 
        // first time this method is called. 
        if (headElements == null) { 
            // Get the head elements from the super implementation 
            headElements = super.getHeadElements(); 

            CssImport cssImport = new CssImport("/css/browse.css"); 
            headElements.add(cssImport); 
        } 
        return headElements; 
    }

    public static class LabelTypePart {
        public String label;
        public String part;
        public String type;
        
        public LabelTypePart(String label, String type, String part) {
            this.label = label;
            this.type = type;
            this.part = part;
        }
        
        public String getLabel() {
            return this.label;
        }
        
        public String getPart() {
            return this.part;
        }
        
        public String getType() {
            return this.type;
        }
    }
}
