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
package edu.indiana.dlib.catalog.pages;

import java.util.List;
import java.util.Map;

import org.apache.click.control.ActionLink;
import org.apache.click.element.CssImport;
import org.apache.click.element.Element;
import org.apache.click.element.ResourceElement;
import org.apache.click.util.HtmlStringBuffer;
import org.apache.commons.lang.StringUtils;
import org.apache.xerces.util.URI;
import org.apache.xerces.util.URI.MalformedURIException;

import edu.indiana.dlib.catalog.accesscontrol.AuthenticationManager;
import edu.indiana.dlib.catalog.accesscontrol.AuthorizationManager;
import edu.indiana.dlib.catalog.config.CollectionConfiguration;
import edu.indiana.dlib.catalog.config.ConfigurationManager;
import edu.indiana.dlib.catalog.config.ConfigurationManagerException;
import edu.indiana.dlib.catalog.config.Item;
import edu.indiana.dlib.catalog.config.ItemManager;
import edu.indiana.dlib.catalog.config.NameValuePair;
import edu.indiana.dlib.catalog.pages.panel.ItemPanel;
import edu.indiana.dlib.catalog.search.SearchResultItemSummary;
import edu.indiana.dlib.catalog.search.structured.StructuredSearchResults;

public class ItemPage extends PublicBorderPage {

    public Item item;

    public boolean onSecurityCheck() {
        if (!super.onSecurityCheck()) {
            return false;
        } else {
            String id = getContext().getRequest().getParameter("id");
            ConfigurationManager cm = getConfigurationManager();
            ItemManager im = getItemManager();
            try {
                collection = cm.getCollectionConfiguration(im.getItemCollectionId(id));
                item = im.fetchItem(id, collection);
                unit = cm.getParent(collection.getId(), true);
                if (collection.getCollectionMetadata().getCollectionManagerAddress() != null) {
                    addModel("contactLink", "<a href=\"contact.htm" + (getScopeId() != null ? "?" + SCOPE_PARAM_NAME + "=" + getScopeId() + "&amp;id=" + id : "?id=" + id) + "\">" + getMessage("contact") + "</a>");
                }
                addControl(new ItemPanel("item-panel"));
            } catch (Throwable t) {
                LOGGER.error("Error fetching item with id \"" + id + "\"!", t);
                // item will be null, fall through
            }        
            
            if (item != null) {
                // verify that the item is public
                try {
                    if (!cm.isItemPublic(item)) {
                        // it'll be as if the item doesn't exist
                        item = null;
                        collection = null;
                        LOGGER.warn("User tried to access non-public item " + item.getId() + ".");
                        setRedirect("unknown-item.htm");
                        return false;
                    }
                } catch (ConfigurationManagerException ex) {
                    throw new RuntimeException(ex);
                }
                
                // see if we can provide result-set navigation links
                StructuredSearchResults results = SearchPage.getCurrentSearchResults(getContext().getSession());
                if (results != null) {
                    for (int i = 0; i < results.getResults().size(); i ++) {
                        if (results.getResults().get(i).getId().equals(item.getId())) {
                            if (i > 0 || results.getStartingIndex() != 0) {
                                // add the previous link
                                XHTMLActionLink prev = new XHTMLActionLink("prev", this, "goToPreviousItem");
                                prev.setLabel(getMessage("prev"));
                                prev.setParameter("id", item.getId());
                                if (getScopeId() != null) {
                                    prev.setParameter("scope", getScopeId());
                                }
                                addControl(prev);
                            }
                            
                            // add return to search link
                            XHTMLActionLink returnLink = new XHTMLActionLink("return", this, "returnToSearch");
                            returnLink.setLabel(getMessage("return"));
                            returnLink.setParameter("id", item.getId());
                            if (getScopeId() != null) {
                                returnLink.setParameter("scope", getScopeId());
                            }
                            addControl(returnLink);
                            
                            if (i + results.getStartingIndex() < (results.getTotalResultsCount() - 1)) {
                                // add next link
                                XHTMLActionLink next = new XHTMLActionLink("next", this, "goToNextItem");
                                next.setLabel(getMessage("next"));
                                next.setParameter("id", item.getId());
                                if (getScopeId() != null) {
                                    next.setParameter("scope", getScopeId());
                                }
                                addControl(next);
                            }
                        }
                    }
                }
            } else {
                setRedirect("unknown-item.htm");
                return false;
            }
        }
        return true;
    }

    public String getTitle() {
        if (item == null) {
            return getMessage("default-title");
        } else {
            try {
                SearchResultItemSummary summary = new SearchResultItemSummary(item, collection);
                StringBuffer sb = new StringBuffer();
                for (NameValuePair title : summary.getFields()) {
                    if (sb.length() > 0) {
                        sb.append(", ");
                    }
                    sb.append(title.getValue());
                }
                return getMessage("title", sb.toString());
            } catch (Throwable t) {
                return getMessage("default-title");
            }
        }
    }
    
    public List<Element> getHeadElements() { 
        // Use lazy loading to ensure the JS is only added the 
        // first time this method is called. 
        if (headElements == null) { 
            // Get the head elements from the super implementation 
            headElements = super.getHeadElements(); 
            headElements.add(new CssImport("/css/item.css"));
            try {
                headElements.add(new CanonicalUrlElement(new URI(item.getId()).toString()));
            } catch (MalformedURIException ex) {
                LOGGER.debug("Item \"" + item.getId() + "\" is not a valid URI and is not added as the canonical URL for the item display page.");
            }
        } 
        return headElements; 
    }
    
    public void onRender() {
        super.onRender();
        addModel("purl-link", getMessage("purl", item.getId()));
    }
    
    public boolean goToPreviousItem() {
        StructuredSearchResults results = SearchPage.getCurrentSearchResults(getContext().getSession());
        if (results != null) {
            for (int i = 0; i < results.getResults().size(); i ++) {
                if (results.getResults().get(i).getId().equals(item.getId()) && ((i + results.getStartingIndex()) < results.getTotalResultsCount())) {
                    if (i > 0) { 
                        setRedirect("item.htm?id=" + results.getResults().get(i -1).getId() + (getScopeId() != null ? "&" + SCOPE_PARAM_NAME + "=" + getScopeId() : ""));
                        return false;
                    } else {
                        int globalIndex = i + results.getStartingIndex();
                        results = SearchPage.getPreviousPage(getContext().getSession(), getSearchManager());
                        int newIndex = globalIndex - (results.getStartingIndex()) - 1;
                        if (newIndex >= results.getResults().size()) {
                            LOGGER.warn("Result set changed while user was flipping through results.");
                            return true;
                        }
                        if (results != null && !results.getResults().isEmpty()) {
                            setRedirect("item.htm?id=" + results.getResults().get(newIndex).getId() + (getScopeId() != null ? "&" + SCOPE_PARAM_NAME + "=" + getScopeId() : ""));
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }
    
    public boolean returnToSearch() {
        String query = SearchPage.getCurrentSearchURLQuery(getContext().getSession(), getScopeId(), getSearchManager(), false);
        setRedirect("search.htm" + (query == null ? "" : "?" + query));
        return false;
    }
    
    public boolean goToNextItem() {
        StructuredSearchResults results = SearchPage.getCurrentSearchResults(getContext().getSession());
        if (results != null) {
            for (int i = 0; i < results.getResults().size(); i ++) {
                if (results.getResults().get(i).getId().equals(item.getId()) && (i + results.getStartingIndex() < (results.getTotalResultsCount() - 1))) {
                    if (i + 1 < results.getResults().size()) {
                        setRedirect("item.htm?id=" + results.getResults().get(i + 1).getId() + (getScopeId() != null ? "&" + SCOPE_PARAM_NAME + "=" + getScopeId() : ""));
                        return false;
                    } else {
                        results = SearchPage.getNextPage(getContext().getSession(), getSearchManager());
                        if (results != null && !results.getResults().isEmpty()) {
                            setRedirect("item.htm?id=" + results.getResults().get(0).getId() + (getScopeId() != null ? "&" + SCOPE_PARAM_NAME + "=" + getScopeId() : ""));
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }
    
    private static class XHTMLActionLink extends ActionLink {
     
        public XHTMLActionLink(String name, Object listener, String method) {
            super(name, listener, method);
        }

        /**
         * Render the Image tag to the buffer.
         *
         * @param buffer the buffer to render the image tag to
         */
        protected void renderImgTag(HtmlStringBuffer buffer) {
            buffer.elementStart("img");
            //buffer.appendAttribute("border", 0); NOT XHTML VALID
            buffer.appendAttribute("style", "border:0;");
            //buffer.appendAttribute("hspace", 2); NOT XHTML VALID
            buffer.appendAttribute("class", "link");

            if (getTitle() != null) {
                buffer.appendAttributeEscaped("alt", getTitle());
            } else {
                buffer.appendAttributeEscaped("alt", getLabel());
            }

            String src = getImageSrc();
            if (StringUtils.isNotBlank(src)) {
                if (src.charAt(0) == '/') {
                    src = getContext().getRequest().getContextPath() + src;
                }
                buffer.appendAttribute("src", src);
            }

            buffer.elementEnd();
        }
        
    }

    public static class CanonicalUrlElement extends ResourceElement {

        private String url;
        
        public CanonicalUrlElement(String url) {
            this.url = url;
            setAttribute("href", url);
            setAttribute("rel", "canonical");
        }
        
        public String getTag() {
            return "link";
        }
        
        public void render(HtmlStringBuffer buffer) {
            buffer.elementStart(getTag());

            if (isRenderId()) {
                buffer.appendAttribute("id", getId());
            }

            buffer.appendAttributeEscaped("rel", "canonical");
            
            buffer.appendAttributeEscaped("href", url);

            Map<String, String> localAttributes = getAttributes();
            for (Map.Entry<String, String> entry : localAttributes.entrySet()) {
                String name = entry.getKey();
                if (!name.equals("id") && !name.equals("href") && !name.equals("rel")) {
                    buffer.appendAttributeEscaped(name, entry.getValue());
                }
            }

            buffer.elementEnd();
        }

        public boolean equals(Object o) {
            return (o instanceof CanonicalUrlElement && ((CanonicalUrlElement) o).url.equals(url));
        }


        public int hashCode() {
            return url.hashCode();
        }
        
    }
    
}
