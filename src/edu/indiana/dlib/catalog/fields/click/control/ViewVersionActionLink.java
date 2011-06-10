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
package edu.indiana.dlib.catalog.fields.click.control;

import java.util.ArrayList;
import java.util.List;

import org.apache.click.ActionResult;
import org.apache.click.Control;
import org.apache.click.ajax.DefaultAjaxBehavior;
import org.apache.click.control.ActionLink;
import org.apache.click.element.Element;
import org.apache.click.element.JsImport;
import org.apache.click.element.JsScript;
import org.apache.click.util.HtmlStringBuffer;

import edu.indiana.dlib.catalog.config.CollectionConfiguration;
import edu.indiana.dlib.catalog.config.FieldConfiguration;
import edu.indiana.dlib.catalog.config.HistoryEnabledItemManager;
import edu.indiana.dlib.catalog.config.ItemMetadata;
import edu.indiana.dlib.catalog.config.HistoryEnabledItemManager.VersionInformation;

public class ViewVersionActionLink extends ActionLink {

    private VersionInformation version;
    
    private HistoryEnabledItemManager itemManager;

    private CollectionConfiguration config;
    
    public ViewVersionActionLink(VersionInformation version, HistoryEnabledItemManager itemManager, CollectionConfiguration config) {
        super("version_link_" + version.getDate().getTime());
        super.setId("version_link_" + version.getDate().getTime());
        this.version = version;
        this.itemManager = itemManager;
        this.config = config;
        this.addBehavior(new VersionSummaryAjaxBehavior());
    }
    
    public String getLabel() {
        if (label == null) {
            label = getPage().getFormat().date(version.getDate());
        } 
        return label;
    }
    
    public VersionInformation getVersion() {
        return this.version;
    }
    
    public String getTargetDivId() {
        return getId() + "_ajax_target";
    }
    
    public String getHref(Object value) {
        String href = super.getHref(value);
        return href.substring(href.lastIndexOf('/') + 1);
    }
    
    public List<Element> getHeadElements() {
        if (headElements == null) {
            headElements = new ArrayList<Element>();
            headElements.add(new JsImport("/js/jquery/jquery-1.5.min.js"));
            
            headElements.add(new JsImport("/js/view-version-action-link.js"));
            
            StringBuffer script = new StringBuffer();
            script.append("jQuery.noConflict();\n");
            script.append("jQuery(document).ready(function() {\n");
            script.append("  jQuery(\"#" + getId() + "\").click(function(event){\n");
            script.append("      toggleVersionInformation('" + getId() + "', '" + getTargetDivId() + "');\n");
            script.append("      return false;\n");
            script.append("  })\n");
            script.append("  jQuery(\"#" + getTargetDivId() + "\").hide();\n");
            script.append("})\n");
            headElements.add(new JsScript(script.toString()));
        }
        return headElements;
    }
    
    public void render(HtmlStringBuffer buffer) {
        super.render(buffer);
        buffer.elementStart("div");
        buffer.appendAttribute("id", getTargetDivId());
        buffer.closeTag();
        buffer.elementEnd("div");
    }
    
    public class VersionSummaryAjaxBehavior extends DefaultAjaxBehavior {
        
        public ActionResult onAction(Control source) {
            HtmlStringBuffer buffer = new HtmlStringBuffer();
            try {
                ItemMetadata metadata = itemManager.getHistoricItemMetdata(version.getId(), version.getDate());
                buffer.elementStart("table");
                buffer.closeTag();
                boolean hasAnyData = false;
                for (FieldConfiguration fieldConfig : config.listFieldConfigurations()) {
                    if (metadata.getFieldData(fieldConfig.getFieldType()) != null) {
                        hasAnyData = true;
                        buffer.elementStart("tr");
                        buffer.closeTag();
                        buffer.elementStart("th");
                        buffer.closeTag();
                        buffer.append(fieldConfig.getDisplayLabel());
                        buffer.elementEnd("th");
                        buffer.elementStart("td");
                        buffer.closeTag();
                        buffer.append(config.getValueSummary(metadata, fieldConfig.getFieldType()));
                        buffer.elementEnd("td");
                        buffer.elementEnd("tr");
                    }
                }
                if (!hasAnyData) {
                    buffer.elementStart("tr");
                    buffer.closeTag();
                    buffer.elementStart("td");
                    buffer.closeTag();
                    buffer.append(getMessage("empty-record"));
                    buffer.elementEnd("td");
                    buffer.elementEnd("tr");
                }
                buffer.elementEnd("table");
            } catch (Exception ex) {
                return new ActionResult(ViewVersionActionLink.this.getMessage("exception-getting-version"), ActionResult.HTML);
            }
            return new ActionResult(buffer.toString(), ActionResult.HTML);
        }
        
        /**
         * Adds the jquery javascript libraries.
         */
        public void addHeadElementsOnce(Control control) {
            control.getHeadElements().add(new JsImport("/js/jquery/jquery-1.5.min.js"));
        }

    }
}
