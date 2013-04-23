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
package edu.indiana.dlib.catalog.fields.click.control.autocomplete;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.click.Context;
import org.apache.click.ControlRegistry;
import org.apache.click.Page;
import org.apache.click.control.AbstractContainer;
import org.apache.click.element.CssImport;
import org.apache.click.element.Element;
import org.apache.click.element.JsImport;
import org.apache.click.util.ClickUtils;
import org.apache.click.util.HtmlStringBuffer;
import org.apache.log4j.Logger;

import edu.indiana.dlib.catalog.config.CollectionConfiguration;
import edu.indiana.dlib.catalog.config.FieldConfiguration;
import edu.indiana.dlib.catalog.config.VocabularySourceConfiguration;
import edu.indiana.dlib.catalog.servlets.CollectionPathRedirectionServlet;
import edu.indiana.dlib.catalog.vocabulary.VocabularySource;
import edu.indiana.dlib.catalog.vocabulary.VocabularySourceInitializationException;
import edu.indiana.dlib.catalog.vocabulary.VocabularyTerm;
import edu.indiana.dlib.catalog.vocabulary.impl.VocabularySourceFactory;

/**
 * An extension of AbstractContainer that allows for nested
 * VocabularySourceAutoCompleteTextField to work.  This class
 * is responsible for adding the required javascript to the 
 * page.
 * 
 * This class and VocabularySourceAutoCompleteTextField largely
 * copies the functionality of the extras AutoCompleteTextField.
 */
public class AutoCompleteAjaxTargetControl extends AbstractContainer {

    private Logger LOGGER = Logger.getLogger(AutoCompleteAjaxTargetControl.class);
    
    private CollectionConfiguration config;

    public AutoCompleteAjaxTargetControl(String name, CollectionConfiguration config) {
        super(name);
        this.config = config;
    }
    
    /**
     * Gets a VocabularySource implementation for the given
     * source id.  The current implementation walks through
     * the configuration for the collection and gets an instance
     * of the first one with a matching id.
     */
    private VocabularySource getSource(String sourceId) throws IOException, VocabularySourceInitializationException {
        for (FieldConfiguration field : config.listFieldConfigurations(true)) {
            for (VocabularySourceConfiguration sourceConfig : field.getVocabularySources()) {
                if (sourceConfig.getId().equals(sourceId)) {
                    return VocabularySourceFactory.getInstance().getVocabularySource(sourceConfig, config.getSourceDefinition(field, sourceConfig.getType()), config.getId());
                }
            }
        }
        return null;
    }
    
    public boolean isAjaxTarget(Context context) {
        if (context.getRequestParameter(getName()) != null) {
            return true;
        } else {
            //System.out.println(context.getRequestParameter(getName()));
            return false;
        }
    }
    
    public String getActionUrl() {
        return  getContext().getRequest().getContextPath() + CollectionPathRedirectionServlet.getRequestPath(getContext().getRequest()) + (getContext().getRequest().getQueryString() == null ? "" : "?" + getContext().getRequest().getQueryString());
    }
    
    /**
     * Return the list of HEAD {@link org.apache.click.element.Element elements}
     * (resources) to be included in the page. The resources are:
     * <p/>
     * <ul>
     * <li>/click/extras-control.css</li>
     * <li>/click/control.js</li>
     * <li>/click/prototype/prototype.js</li>
     * <li>/click/prototype/effects.js</li>
     * <li>/click/prototype/controls.js</li>
     * <li>/js/source-autocomplete.js</li>
     * </ul>
     * *

     * @see org.apache.click.Control#getHeadElements()
     *
     * @return the list of HEAD elements to be included in the page
     * @throws IllegalStateException if the field's name has not been set or
     * if the field is not attached to the Page
     */
    public List<Element> getHeadElements() {
        // Check that the field name and parent Page has been set
        String fieldName = getName();
        if (fieldName == null) {
            throw new IllegalStateException("AutoCompleteTextField name"
                + " is not defined. Set the name before calling"
                + " getHeadElements().");
        }

        Page page = getPage();
        if (page == null) {
            throw new IllegalStateException("The AutoCompleteTextField, '"
                + fieldName + "', is not attached to the Page. Add"
                + " AutoCompleteTextField to a parent form or container and"
                + " attach the parent to the Page before calling"
                + " getHeadElements().");
        }

        Context context = getContext();

        if (headElements == null) {
            headElements = super.getHeadElements();

            String versionIndicator = ClickUtils.getResourceVersionIndicator(context);

            headElements.add(new CssImport("/click/extras-control.css", versionIndicator));
            headElements.add(new JsImport("/click/control.js", versionIndicator));
            headElements.add(new JsImport("/click/prototype/prototype.js", versionIndicator));
            headElements.add(new JsImport("/click/prototype/effects.js", versionIndicator));
            headElements.add(new JsImport("/click/prototype/controls.js", versionIndicator));
            headElements.add(new JsImport("/js/source-autocomplete.js", versionIndicator));
        }
        return headElements;
    }

    // Event Handlers ---------------------------------------------------------

    /**
     * Register the field with the parent page to intercept POST autocompletion
     * requests.
     *
     * @see org.apache.click.Control#onInit()
     */
    public void onInit() {
        super.onInit();

        ControlRegistry.registerAjaxTarget(this);
        
        Page page = getPage();
        if (page == null) {
            // If parent page is not reachable, exit early
            return;
        }

        // See whether control has been registered at Page level.
        Object control = page.getModel().get(getName());

        // If not registered, then register control
        if (control == null) {
            // Ensure current parent control does not change
            Object parent = getParent();
            page.addControl(this);
            setParent(parent);

        } else if (!(control instanceof AutoCompleteAjaxTargetControl)) {
            String message =
                "Non AutoCompleteTextField object '"
                + control.getClass().toString()
                + "' already registered in Page as: "
                + getName();
            throw new IllegalStateException(message);
        }
    }
    
    /**
     * Process the page request and if an auto completion POST request then
     * render an list of suggested values.
     *
     * @see org.apache.click.Control#onProcess()
     *
     * @return false if an auto complete request, otherwise returns true
     */
    public boolean onProcess() {
        Context context = getContext();
        if (context.isPost()) {
            // If an auto complete POST request then render suggested list,
            // otherwise continue as normal
            if (context.isAjaxRequest()) {
                String criteria = context.getRequestParameter(getName());
                String sourceId = context.getRequestParameter(getName() + "_source");
                if (criteria != null) {
                    List autoCompleteList = getAutoCompleteList(criteria, sourceId);
                    renderAutoCompleteList(autoCompleteList);
                    return false;
                }
            } else {
                super.onProcess();
            }
        }
        return true;
    }
    
    /**
     * @see org.apache.click.control.Field#setParent(Object)
     *
     * @param parent the parent of the Control
     * @throws IllegalStateException if {@link #name} is not defined
     * @throws IllegalArgumentException if the given parent instance is
     * referencing <tt>this</tt> object: <tt>if (parent == this)</tt>
     */
    public void setParent(Object parent) {
        if (parent == null) {
            // If the field parent control is set to null (indicating the field
            // is being removed), also remove the field from its parent page
            Page page = getPage();
            if (page != null) {
                page.getControls().remove(this);
                page.getModel().remove(getName());
            }
        }
        super.setParent(parent);
    }
    
    /**
     * Render the suggested auto completion list to the servlet response.
     *
     * @param autoCompleteList the suggested list of auto completion values
     */
    protected void renderAutoCompleteList(List<String> autoCompleteList) {
        HtmlStringBuffer buffer = new HtmlStringBuffer(10 + (autoCompleteList.size() * 20));

        buffer.append("<ul>");

        for (int i = 0; i < autoCompleteList.size(); i++) {
            String value = autoCompleteList.get(i).toString();
            buffer.append("<li>");
            buffer.appendEscaped(value);
            buffer.append("</li>");
        }

        buffer.append("</ul>");

        HttpServletResponse response = getContext().getResponse();

        response.setContentType(getPage().getContentType());

        try {
            PrintWriter writer = response.getWriter();
            writer.print(buffer.toString());
            writer.flush();
            writer.close();

            getPage().setPath(null);

        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    
    public List getAutoCompleteList(String criteria, String sourceId) {
        // scour the configuration for sources
        VocabularySource source = null;
        try {
            source = getSource(sourceId);
        } catch (Throwable t) {
            LOGGER.error("Exception while getting source for autocomplete list.", t);
            // fall through, leaving source as null
        }
        if (source == null) {
            return Collections.EMPTY_LIST;
        }

        List<String> terms = new ArrayList<String>();
        if (source == null) {
            return Collections.EMPTY_LIST;
        } else {
            List<String> termNames = new ArrayList<String>(25);
            for (VocabularyTerm term : source.getTermsWithPrefix(criteria, 25, 0)) {
                termNames.add(term.getDisplayName());
            }
            return termNames;
        }
    }

    /**
     * Returns "{minChars:3}".
     */
    public String getAutoCompleteOptions() {
        return "{minChars:3}";
    }

}
