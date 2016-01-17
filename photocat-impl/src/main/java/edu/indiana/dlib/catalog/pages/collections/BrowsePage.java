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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
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
import edu.indiana.dlib.catalog.search.BrowseQuery;
import edu.indiana.dlib.catalog.search.BrowseResult;
import edu.indiana.dlib.catalog.search.BrowseResults;
import edu.indiana.dlib.catalog.search.BrowseSet;
import edu.indiana.dlib.catalog.search.impl.DefaultBrowseQuery;
import edu.indiana.dlib.catalog.search.impl.FieldPartBrowseSet;
import edu.indiana.dlib.catalog.search.impl.click.control.BrowseTable;
import edu.indiana.dlib.catalog.search.impl.click.control.PageSizeTablePaginator;
import edu.indiana.dlib.catalog.search.structured.DefaultPagingSpecification;
import edu.indiana.dlib.catalog.search.structured.PagingSpecification;
import edu.indiana.dlib.catalog.search.structured.SearchConstraint;
import edu.indiana.dlib.catalog.search.structured.constraints.CollectionSearchConstraint;
public class BrowsePage extends CollectionPage {
    
    public Table browseTable;
    
    public String fieldType;
    
    public String fieldPartName;
    
    private BrowseResults results;
    
    public String resultHeading;
    
    public List<LabelTypePart> labelTypePartList;
    
    public void onInit() {
        super.onInit();
        fieldType = getContext().getRequestParameter("fieldType");
        fieldPartName = getContext().getRequestParameter("fieldPartName");
        
        labelTypePartList = new ArrayList<LabelTypePart>();
        for (FieldConfiguration conf : collection.listFieldConfigurations(true)) {
            FieldDefinition def = collection.getFieldDefinition(conf);
            if (def == null) {
                throw new NullPointerException("No field definition found for field " + conf.getFieldType() + "!");
            }
            List<String> validPartNames = new ArrayList<String>();
            for (String partName : def.getDataSpecification().getValidPartNames()) {
            	if (!conf.isPartDisabled(partName) && partName.indexOf("authority") == -1) {
            		validPartNames.add(partName);
            	}
            }
            for (String part : validPartNames) {
                String label = (validPartNames.size() == 1 ? conf.getDisplayLabel() : conf.getDisplayLabel() + " (" + conf.getPartDisplayLabel(part) + ")");
                labelTypePartList.add(new LabelTypePart(label, def.getType(), part));
            }
        }
        if (fieldType != null && fieldPartName != null) {
            FieldDefinition def = collection.getFieldDefinition(collection.getFieldConfiguration(fieldType));
            FieldConfiguration fieldConf = collection.getFieldConfiguration(fieldType);
            if (def != null && fieldConf != null) {
                try {
                    BrowseSet set = new FieldPartBrowseSet("", fieldType, fieldPartName, true);
                    List<SearchConstraint> constraints = new ArrayList<SearchConstraint>();
                    constraints.add(new CollectionSearchConstraint(collection.getId()));
                    PagingSpecification pagingSpec = new DefaultPagingSpecification().startingIndex(0).pageSize(2500);
                    results = getSearchManager().browse(new DefaultBrowseQuery(set, constraints, pagingSpec));
                
                    browseTable = new BrowseTable("browseTable", results);
                    browseTable.setPaginator(new PageSizeTablePaginator((BrowseTable) browseTable));
                    browseTable.setShowBanner(true);
                    browseTable.getControlLink().setParameter("fieldType", fieldType);
                    browseTable.getControlLink().setParameter("fieldPartName", fieldPartName);
                    
                    ActionLink viewRecordsActionLink = new RelativeActionLink("viewRecords", getMessage("view-records"), this, "onViewRecords");
                    viewRecordsActionLink.setParameter("fieldType", fieldType);
                    viewRecordsActionLink.setParameter("fieldPartName", fieldPartName);
                    addControl(viewRecordsActionLink);
                    
                    Column column = new Column(getMessage("view-records")); 
                    column.setDecorator(new LinkDecorator(browseTable, viewRecordsActionLink, "query")); 
                    column.setSortable(false); 
                    browseTable.addColumn(column);
                    
                    if (!collection.getFieldConfiguration(fieldType).isReadOnly() && !collection.newInstance(fieldType).hasDerivativeParts() && getAuthorizationManager().canManageCollection(collection, unit, user)) {
                        ActionLink updateRecordsActionLink = new RelativeActionLink("replaceValue", getMessage("replace-value"), this, "onReplaceValue");
                        updateRecordsActionLink.setParameter("fieldType", fieldType);
                        updateRecordsActionLink.setParameter("fieldPartName", fieldPartName);
                        addControl(updateRecordsActionLink);
                        Column replaceColumn = new Column(getMessage("replace-value")); 
                        replaceColumn.setDecorator(new LinkDecorator(browseTable, updateRecordsActionLink, "fieldValue")); 
                        replaceColumn.setSortable(false); 
                        browseTable.addColumn(replaceColumn);
                    }
                    
                    addControl(browseTable);
                    List<String> validPartNames = new ArrayList<String>();
                    for (String partName : def.getDataSpecification().getValidPartNames()) {
                    	if (!fieldConf.isPartDisabled(partName) && partName.indexOf("authority") == -1) {
                    		validPartNames.add(partName);
                    	}
                    }
                    if (validPartNames.size() == 1) {
                        resultHeading = getMessage("results-heading-only-part", fieldConf.getDisplayLabel());
                    } else {
                        resultHeading = getMessage("results-heading", fieldConf.getPartDisplayLabel(fieldPartName), fieldConf.getDisplayLabel());
                    }
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
            }
        }
    }
    
    public boolean onViewRecords() {
        String value = null;
        String query = getContext().getRequestParameter("value");
        for (BrowseResult result : results.listBrowseResults()) {
            if (result.getQuery().equals(query)) {
                value = result.getFieldValue();
                break;
            }
        }
        String filterName = this.collection.getFieldConfiguration(getContext().getRequestParameter("fieldType")).getDisplayLabel() + ": " + value; 
        try {
            this.setRedirect("search.htm?form_name=searchForm&filter_selected=on&filter_value=" + URLEncoder.encode(query, "UTF-8") + "&filter_name=" + URLEncoder.encode(filterName, "UTF-8") + "&pageSize=20&action=Search");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
        return false;
    }
    
    public boolean onReplaceValue() {
        String value = getContext().getRequestParameter("value");
        try {
            this.setRedirect("search-and-replace.htm?fieldType=" + URLEncoder.encode(fieldType, "UTF-8") + "&fieldPart=" + URLEncoder.encode(fieldPartName, "UTF-8") + "&value=" + URLEncoder.encode(value, "UTF-8"));
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
        return false;
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
