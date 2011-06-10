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

import java.util.List;

import org.apache.click.control.AbstractLink;
import org.apache.click.element.Element;
import org.apache.click.element.JsImport;

public class ClassTogglingLink extends AbstractLink {

    public String elementId;
    
    private String styleClass;
    
    private String alternateStyleClass;
    
    public ClassTogglingLink(String name, String label, String elementId, String styleClass, String alternateStyleClass) {
        super(name);
        this.elementId = elementId;
        this.styleClass = styleClass;
        this.alternateStyleClass = alternateStyleClass;
        this.setLabel(label);
    }

    public String getHref() {
        return "javascript:toggle('" + elementId + "', '" + styleClass + "', '" + alternateStyleClass + "');";
    }
    
    public List<Element> getHeadElements() {
        // We use lazy loading to ensure the CSS import is only added the 
        // first time this method is called. 
        if (headElements == null) { 
            // Get the head elements from the super implementation 
            headElements = super.getHeadElements(); 
            
            JsImport jsImport = new JsImport("/js/class-toggling-link.js");
            headElements.add(jsImport);
        } 
        return headElements; 
    }
}
