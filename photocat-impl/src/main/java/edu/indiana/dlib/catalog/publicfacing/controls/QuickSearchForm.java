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
package edu.indiana.dlib.catalog.publicfacing.controls;

import java.util.ArrayList;
import java.util.List;

import org.apache.click.control.Field;
import org.apache.click.control.FieldSet;
import org.apache.click.control.Form;
import org.apache.click.control.HiddenField;
import org.apache.click.control.Option;
import org.apache.click.control.OptionGroup;
import org.apache.click.control.Select;
import org.apache.click.control.Submit;
import org.apache.click.control.TextField;
import org.apache.click.util.ContainerUtils;
import org.apache.click.util.HtmlStringBuffer;

import edu.indiana.dlib.catalog.config.CollectionConfiguration;
import edu.indiana.dlib.catalog.config.ConfigurationManager;
import edu.indiana.dlib.catalog.config.ConfigurationManagerException;
import edu.indiana.dlib.catalog.pages.SearchPage;
import edu.indiana.dlib.catalog.search.SearchManager;
import edu.indiana.dlib.catalog.search.structured.DefaultPagingSpecification;
import edu.indiana.dlib.catalog.search.structured.DefaultStructuredSearchQuery;
import edu.indiana.dlib.catalog.search.structured.SearchConstraint;
import edu.indiana.dlib.catalog.search.structured.StructuredSearchQuery;
import edu.indiana.dlib.catalog.search.structured.constraints.CollectionSearchConstraint;
import edu.indiana.dlib.catalog.search.structured.constraints.OrSearchConstraintGroup;
import edu.indiana.dlib.catalog.search.structured.constraints.SerializableSearchConstraint;
import edu.indiana.dlib.catalog.search.structured.constraints.UserQuerySearchConstraint;

/**
 * A form that when processed compiles a URL for the search page
 * incorporating the entered query and selections and redirects 
 * the browser to that URL.
 */
public class QuickSearchForm extends Form {

    private ConfigurationManager cm;
    
    private SearchManager sm;
    
    private TextField queryField;
    
    private Select scopeSelect;
    
    private HiddenField originalScope;
    
    public QuickSearchForm(String name, ConfigurationManager cm, SearchManager sm, String scopeId) throws ConfigurationManagerException {
        super(name);
        this.cm = cm;
        this.sm = sm;
        
        queryField = new TextField("query");
        queryField.setSize(20);
        add(queryField);
        
        originalScope = new HiddenField("originalScope", String.class);
        originalScope.setValue(scopeId);
        add(originalScope);
        
        scopeSelect = new Select("searchScope");
        scopeSelect.add(new Option("", getMessage("all-photographs")));
        for (CollectionConfiguration unit : cm.getCollectionConfigurations(ConfigurationManager.UNIT ,true)) {
            OptionGroup group = new OptionGroup(unit.getCollectionMetadata().getShortName());
            group.add(new Option(unit.getId(), getMessage("unit-option-group", unit.getCollectionMetadata().getShortName())));
            for (CollectionConfiguration collection : cm.getChildren(unit.getId(), ConfigurationManager.COLLECTION, true)) {
                group.add(new Option(collection.getId(), getMessage("unit-collection", collection.getCollectionMetadata().getShortName())));
            }
            if (!group.getChildren().isEmpty()) {
                scopeSelect.add(group);
            }
        }
        for (CollectionConfiguration collection : cm.getOrphans(ConfigurationManager.COLLECTION, true)) {
            scopeSelect.add(new Option(collection.getId(), getMessage("unaffiliated-collection", collection.getCollectionMetadata().getShortName())));
        }
        add(scopeSelect);
        
        add(new Submit("search", getMessage("submit-search"), this, "onSearch"));
    }
    
    public boolean onSearch() {
        List<SearchConstraint> constraints = new ArrayList<SearchConstraint>();
        
        if (queryField.getValue() != null && queryField.getValue().trim().length() > 0) {
            // add user's query
            constraints.add(new UserQuerySearchConstraint(queryField.getValue()));
        }
        String originalScopeId = originalScope.getValue();
        String scopeIdRestriction = scopeSelect.getValue();

        // Normally we'd retain the original scope, *EXCEPT* when it would be invalid..
        // that means that if the search constraint was no less restrictive than the
        // original scope we'll retain it... otherwise we'll throw it out entirely.  
        // This means that:
        // A search from an item page that isn't intentionally restricted to the collection
        // to which that item belongs will rescope the application to include everything
        if (!originalScopeId.equals(scopeIdRestriction)) {
            originalScopeId = null;
            try {
                if (scopeIdRestriction != null && !scopeIdRestriction.equals("")) {
                    CollectionConfiguration scope = cm.getCollectionConfiguration(scopeIdRestriction);
                    
                    // Add the search constraint for the selected scope
                    if (scope == null) {
                        // do nothing, no constraint needed
                    } else if (scope.getCollectionMetadata().isUnit()) {
    
                        List<SerializableSearchConstraint> cc = new ArrayList<SerializableSearchConstraint>();
                        for (CollectionConfiguration c : cm.getChildren(scope.getId(), ConfigurationManager.COLLECTION, true)) {
                            cc.add(new CollectionSearchConstraint(c.getId()));
                        }
                        constraints.add(new OrSearchConstraintGroup(getMessage("unit-search-constraint", scope.getCollectionMetadata().getShortName()), cc, false));
                    } else {
                        constraints.add(new CollectionSearchConstraint(scope, false));
                    }
                }
            } catch (ConfigurationManagerException ex) {
                this.setError("Error performing search!" + (ex.getLocalizedMessage() != null ? " (" + ex.getLocalizedMessage() + ")" : ""));
                return true;
            }
        }
        
        StructuredSearchQuery query = new DefaultStructuredSearchQuery(new DefaultPagingSpecification().pageSize(15), constraints);
        getPage().setRedirect("search.htm?" + SearchPage.getURLQueryFromParameterMap(SearchPage.getParameterMapForQuery(query, sm, originalScopeId), false));
        return false;
    }
    
    /**
     * Render the given form start tag and the form hidden fields to the given
     * buffer.
     *
     * @param buffer the HTML string buffer to render to
     * @param formFields the list of form fields
     */
    protected void renderHeader(HtmlStringBuffer buffer, List<Field> formFields) {

        buffer.elementStart(getTag());

        buffer.appendAttribute("method", getMethod());
        buffer.appendAttribute("id", getId());
        buffer.appendAttribute("action", getActionURL());
        buffer.appendAttribute("enctype", getEnctype());

        appendAttributes(buffer);

        if (isJavaScriptValidation()) {
            String javaScript = "return on_" + getId() + "_submit();";
            buffer.appendAttribute("onsubmit", javaScript);
        }
        buffer.closeTag();
        buffer.append("\n");

        // render fieldset open tag
        buffer.elementStart("fieldset");
        buffer.closeTag();
        
        // Render hidden fields
        for (Field field : ContainerUtils.getHiddenFields(this)) {
            field.render(buffer);
            buffer.append("\n");
        }
    }
    
    /**
     * Close the form tag and render any additional content after the Form.
     * <p/>
     * Additional content includes <tt>javascript validation</tt> and
     * <tt>javascript focus</tt> scripts.
     *
     * @param formFields all fields contained within the form
     * @param buffer the buffer to render to
     */
    protected void renderTagEnd(List<Field> formFields, HtmlStringBuffer buffer) {

        buffer.elementEnd("fieldset");
        
        buffer.elementEnd(getTag());
        buffer.append("\n");

        renderFocusJavaScript(buffer, formFields);

        renderValidationJavaScript(buffer, formFields);
    }
    
}
