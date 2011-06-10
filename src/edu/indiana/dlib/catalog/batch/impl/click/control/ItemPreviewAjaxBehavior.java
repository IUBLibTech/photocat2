/**
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
package edu.indiana.dlib.catalog.batch.impl.click.control;

import java.util.List;

import org.apache.click.ActionResult;
import org.apache.click.Context;
import org.apache.click.Control;
import org.apache.click.ajax.DefaultAjaxBehavior;
import org.apache.click.control.Select;
import org.apache.click.element.Element;
import org.apache.click.element.JsImport;
import org.apache.click.element.JsScript;
import org.apache.click.util.HtmlStringBuffer;

import edu.indiana.dlib.catalog.config.CollectionConfiguration;
import edu.indiana.dlib.catalog.config.FieldConfiguration;
import edu.indiana.dlib.catalog.config.Item;
import edu.indiana.dlib.catalog.config.ItemManager;
import edu.indiana.dlib.catalog.config.RepositoryException;

/**
 * An AjaxBehavior implementation that returns a snippet of HTML
 * that represents a preview for an item.  This includes a preview
 * image as well as a summary of the metadata. 
 * 
 * This AjaxBehavior is written to be attached to a Select whose 
 * possible selected values are the identifier for the item to 
 * preview.
 */
public class ItemPreviewAjaxBehavior extends DefaultAjaxBehavior {

    private String targetDivId;
    
    private ItemManager im;
    
    private CollectionConfiguration config;
    
    public ItemPreviewAjaxBehavior(String targetDivId, ItemManager im, CollectionConfiguration config) {
        super();
        this.targetDivId = targetDivId;
        this.im = im;
        this.config = config;
    }
    
    public boolean isAjaxTarget(Context context) {
        return super.isAjaxTarget(context);
    }
    
    public ActionResult onAction(Control source) {
        super.onAction(source);
        HtmlStringBuffer buffer = new HtmlStringBuffer();
        if (source instanceof Select) {
            String selectedId = null;
            for (Object value : ((Select) source).getSelectedValues()) {
                selectedId = (String) value;
            }
            if (selectedId != null) {
                try {
                    Item item = im.fetchItem(selectedId);
                    if (item.getPreview() != null) {
                        buffer.elementStart("table");
                        buffer.closeTag();
                        
                        buffer.elementStart("tr");
                        buffer.closeTag();
                        
                        buffer.elementStart("th");
                        buffer.appendAttribute("colspan", "2");
                        buffer.closeTag();
                        buffer.elementStart("h3");
                        buffer.closeTag();
                        buffer.append(item.getIdWithinCollection());
                        buffer.elementEnd("h3");
                        buffer.elementEnd("th");
                        
                        buffer.elementEnd("tr");
                        
                        buffer.elementStart("tr");
                        buffer.closeTag();
                        buffer.elementStart("tr");
                        buffer.closeTag();
                        buffer.elementStart("td");
                        buffer.elementStart("img");
                        buffer.appendAttribute("src", item.getPreview().getURL());
                        buffer.appendAttribute("alt", "preview image");
                        buffer.elementEnd();
                        buffer.elementEnd("td");
                        
                        buffer.elementStart("td");
                        buffer.closeTag();
                        buffer.elementStart("table");
                        buffer.closeTag();
                        for (FieldConfiguration fieldConfig : config.listFieldConfigurations()) {
                            if (item.getMetadata().getFieldData(fieldConfig.getFieldType()) != null) {
                                buffer.elementStart("tr");
                                buffer.closeTag();
                                buffer.elementStart("th");
                                buffer.closeTag();
                                buffer.append(fieldConfig.getDisplayLabel());
                                buffer.elementEnd("th");
                                buffer.elementStart("td");
                                buffer.closeTag();
                                buffer.append(config.getValueSummary(item.getMetadata(), fieldConfig.getFieldType()));
                                buffer.elementEnd("td");
                                buffer.elementEnd("tr");
                            }
                        }
                        buffer.elementEnd("table");
                        buffer.elementEnd("td");
                        buffer.elementEnd("tr");
                        buffer.elementEnd("table");
                    }
                } catch (RepositoryException ex) {
                    ex.printStackTrace();
                }
            } else {
                buffer.append("No Item selected");
            }
        }
        ActionResult result = new ActionResult(buffer.toString(), ActionResult.HTML); 
        return result; 
    }
    
    public void addHeadElementsOnce(Control source) {
        super.addHeadElementsOnce(source);
        List<Element> headElements = source.getHeadElements();
        headElements.add(new JsImport("/js/jquery/jquery-1.5.min.js"));
        headElements.add(new JsImport("/js/item-preview.js"));
        StringBuffer script = new StringBuffer();
        script.append("jQuery.noConflict();\n");
        script.append("jQuery(document).ready(function() {\n");
        script.append("  jQuery(\"#" + source.getId() + "\").change(function(event){\n");
        script.append("      showItemPreview('" + source.getId() + "', '" + targetDivId + "');\n");
        script.append("      return false;\n");
        script.append("  })\n");
        script.append("})\n");
        headElements.add(new JsScript(script.toString()));
    } 
}
