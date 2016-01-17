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
package edu.indiana.dlib.catalog.pages.panel;

import java.util.List;

import org.apache.click.control.Panel;
import org.apache.click.element.CssImport;
import org.apache.click.element.Element;
import org.apache.click.element.JsImport;

/**
 * A Panel that displays an item in a way that features the 
 * visual representations.  Typically this includes display of
 * the screen sized view of each aspect, but may also include
 * a zoom feature if supported by the particular image.   
 * 
 * For simple cases, where there's a single view that does not
 * support zooming and scaling, this panel outputs simple HTML
 * that presents the screen sized image as a link to the larger
 * sized image.
 * 
 * TODO: When multiple aspects exist and javascript is enabled, 
 * this panel uses the JQuery UI library to present a tabbed view
 * of the aspects.
 * 
 * When possible, a particular view is presented with a link to 
 * an interactive view.
 */
public class ItemPanel extends Panel {

    public ItemPanel(String id) {
        super(id, "item-panel.htm");
    }
    
    public List<Element> getHeadElements() { 
        // Use lazy loading to ensure the JS is only added the 
        // first time this method is called. 
        if (headElements == null) { 
            // Get the head elements from the super implementation 
            headElements = super.getHeadElements(); 
            
            // add the IIPMooViewer content
            headElements.add(new JsImport("/js/jquery/jquery-1.7.1.min.js"));
            headElements.add(new JsImport("/js/djatoka/mootools-1.2-core.js"));
            headElements.add(new JsImport("/js/djatoka/mootools-1.2-more.js"));
            headElements.add(new JsImport("/js/djatoka/iipmooviewer-1.1.js"));
            headElements.add(new CssImport("/css/djatoka/iip.css"));
        } 
        return headElements; 
    }
    
}
