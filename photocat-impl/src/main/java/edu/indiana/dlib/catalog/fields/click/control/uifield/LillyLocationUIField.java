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
package edu.indiana.dlib.catalog.fields.click.control.uifield;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.click.Control;
import org.apache.click.control.AbstractContainer;
import org.apache.click.control.Option;
import org.apache.click.control.Select;
import org.apache.click.control.TextField;
import org.apache.click.element.Element;
import org.apache.click.element.JsImport;
import org.apache.click.element.JsScript;
import org.apache.click.util.Format;
import org.apache.click.util.HtmlStringBuffer;

import edu.indiana.dlib.catalog.config.CollectionConfiguration;
import edu.indiana.dlib.catalog.config.ConfigurationException;
import edu.indiana.dlib.catalog.config.FieldConfiguration;
import edu.indiana.dlib.catalog.config.FieldData;
import edu.indiana.dlib.catalog.config.FieldDefinition;
import edu.indiana.dlib.catalog.config.NameValuePair;
import edu.indiana.dlib.catalog.fields.UIField;

/**
 * A field that presents input for the location.  The location, 
 * broadly speaking, may be 3 places, each with its own set of
 * input fields.  
 * 
 * <ul>
 *   <li>
 *     At the Lilly
 *     <ul>
 *       <li>cabinet number</li>
 *       <li>drawer number</li>
 *     </ul>
 *   </li>
 *   <li>
 *     At the ALF
 *     <ul>
 *       <li>box number</li>
 *     </ul>
 *   </li>
 *   <li>
 *     On Display
 *     <ul>
 *       <li>location (free text)</li>
 *     </ul>
 *   </li>
 * </ul>
 * 
 * This may initially be presented as a dropdown list that when
 * a value is selected results in the appropriate input fields
 * being displayed.
 */
public class LillyLocationUIField extends AbstractContainer implements UIField {

    private static final String OUT_ON_DISPLAY = "out on display";

    private static final String BOX_NUMBER = "box number";

    private static final String DRAWER_NUMBER = "drawer number";

    private static final String CABINET_NUMBER = "cabinet number";

    private Select locationSelect;
    
    private TextField cabinetNumberField;
    
    private TextField drawerNumberField;
    
    private TextField boxNumberField;
    
    private TextField outOnDisplayField;
    
    private FieldDefinition def;
    
    private FieldConfiguration conf;
    
    public LillyLocationUIField(FieldDefinition def, FieldConfiguration conf, CollectionConfiguration c) throws ConfigurationException {
        this.def = def;
        this.conf = conf;
        if (def.getDataSpecification().getValidAttributeNames() != null && !def.getDataSpecification().getValidAttributeNames().isEmpty()) {
            throw new ConfigurationException(getMessage("attributes-not-supported"));
        }
        
        if (!def.getDataSpecification().getValidPartNames().containsAll(getRequiredPartNames()) || !getRequiredPartNames().containsAll(def.getDataSpecification().getValidPartNames())) {
            throw new ConfigurationException(getMessage("invalid-part-names"));
        }
        
        locationSelect = new Select(conf.getFieldType() + "_location_select");
        locationSelect.add(new Option("nothing", ""));
        locationSelect.add(new Option("lilly", getMessage("select-location-lilly")));
        locationSelect.add(new Option("alf", getMessage("select-location-alf")));
        locationSelect.add(new Option("display", getMessage("select-location-display")));
        add(locationSelect);

        cabinetNumberField = new TextField(conf.getFieldType() + "_cabinetNumber");
        cabinetNumberField.setSize(3);
        add(cabinetNumberField);

        drawerNumberField = new TextField(conf.getFieldType() + "_drawerNumber");
        drawerNumberField.setSize(4);
        add(drawerNumberField);
        
        boxNumberField = new TextField(conf.getFieldType() + "_boxNumber");
        boxNumberField.setSize(3);
        add(boxNumberField);
        
        outOnDisplayField = new TextField(conf.getFieldType() + "_outOnDisplay");
        add(outOnDisplayField);
    }

    public Collection<String> getRequiredPartNames() {
        return Arrays.asList(new String[] { CABINET_NUMBER, DRAWER_NUMBER, BOX_NUMBER, OUT_ON_DISPLAY });
    }
    
    public List<Element> getHeadElements() {
        headElements = super.getHeadElements();
        JsImport jsImport = new JsImport("/js/lilly-location-ui-field.js");
        if (!headElements.contains(jsImport)) {
            headElements.add(jsImport);
            JsScript jsScript = new JsScript(
                    "jQuery.noConflict();\n"
                    + "jQuery(document).ready(function() {\n"
                    + "  updateFieldOptions('" + locationSelect.getId() + "', '" + getLillyDivId() + "', '" + getALFDivId() + "', '" + getOnDisplayDivId() + "');\n"
                    + "  return false;\n"
                    + "})\n");
            headElements.add(jsScript);
            headElements.add(new JsImport("/js/jquery/jquery-1.7.1.min.js"));
        }
        return headElements;
    }
    
    public void onRender() {
        locationSelect.setAttribute("onChange", "updateFieldOptions('" + locationSelect.getId() + "', '" + getLillyDivId() + "', '" + getALFDivId() + "', '" + getOnDisplayDivId() + "');\n");
        super.onRender();
        
    }
    
    public String getLillyDivId() {
        return getFieldType() + "_lilly";
    }
    
    public String getALFDivId() {
        return getFieldType() + "_alf";
    }
    
    public String getOnDisplayDivId() {
        return getFieldType() + "_display";
    }
    
    public void render(HtmlStringBuffer buffer) {
        locationSelect.render(buffer);
        buffer.elementStart("div");
        buffer.appendAttribute("id", getLillyDivId());
        buffer.closeTag();
        buffer.elementStart("table");
        buffer.closeTag();
        buffer.elementStart("tr");
        buffer.closeTag();
        buffer.elementStart("th");
        buffer.closeTag();
        buffer.append(getFieldConfiguration().getPartDisplayLabel(CABINET_NUMBER));
        buffer.elementEnd("th");
        buffer.elementStart("td");
        buffer.closeTag();
        cabinetNumberField.render(buffer);
        buffer.elementEnd("td");        
        buffer.elementEnd("tr");
        buffer.elementStart("tr");
        buffer.closeTag();
        buffer.elementStart("th");
        buffer.closeTag();
        buffer.append(getFieldConfiguration().getPartDisplayLabel(DRAWER_NUMBER));
        buffer.elementEnd("th");
        buffer.elementStart("td");
        buffer.closeTag();
        drawerNumberField.render(buffer);
        buffer.elementEnd("td");        
        buffer.elementEnd("tr");
        buffer.elementEnd("table");
        buffer.elementEnd("div");
        
        buffer.elementStart("div");
        buffer.appendAttribute("id", getALFDivId());
        buffer.closeTag();
        buffer.elementStart("table");
        buffer.closeTag();
        buffer.elementStart("tr");
        buffer.closeTag();
        buffer.elementStart("th");
        buffer.closeTag();
        buffer.append(getFieldConfiguration().getPartDisplayLabel(BOX_NUMBER));
        buffer.elementEnd("th");
        buffer.elementStart("td");
        buffer.closeTag();
        boxNumberField.render(buffer);
        buffer.elementEnd("td");        
        buffer.elementEnd("tr");
        buffer.elementEnd("table");
        buffer.elementEnd("div");
        
        buffer.elementStart("div");
        buffer.appendAttribute("id", getOnDisplayDivId());
        buffer.closeTag();
        buffer.elementStart("table");
        buffer.closeTag();
        buffer.elementStart("tr");
        buffer.closeTag();
        buffer.elementStart("th");
        buffer.closeTag();
        buffer.append(getFieldConfiguration().getPartDisplayLabel(OUT_ON_DISPLAY));
        buffer.elementEnd("th");
        buffer.elementStart("td");
        buffer.closeTag();
        outOnDisplayField.render(buffer);
        buffer.elementEnd("td");        
        buffer.elementEnd("tr");
        buffer.elementEnd("table");
        buffer.elementEnd("div");
    }

    public String getFieldType() {
        return def.getType();
    }

    public FieldDefinition getFieldDefinition() {
        return def;
    }

    public FieldConfiguration getFieldConfiguration() {
        return conf;
    }

    public void setDefaultValue(FieldData values) {
        if (getFieldData() != null) {
            setFieldData(values);
        }
    }

    public void setFieldData(FieldData values) {
        if (values == null || values.getParts().isEmpty()) {
            cabinetNumberField.setValue(null);
            drawerNumberField.setValue(null);
            boxNumberField.setValue(null);
            outOnDisplayField.setValue(null);
        } else {
            for (NameValuePair part : values.getParts().get(0)) {
                if (part.getName().equals(CABINET_NUMBER)) {
                    cabinetNumberField.setValue(part.getValue());
                    locationSelect.setValue("lilly");
                } else if (part.getName().equals(DRAWER_NUMBER)) {
                    drawerNumberField.setValue(part.getValue());
                    locationSelect.setValue("lilly");
                } else if (part.getName().equals(BOX_NUMBER)) {
                    boxNumberField.setValue(part.getValue());
                    locationSelect.setValue("alf");
                } else if (part.getName().equals(OUT_ON_DISPLAY)) {
                    outOnDisplayField.setValue(part.getValue());
                    locationSelect.setValue("display");
                } else {
                    // Log this unexpected field which will be lost
                    // if the user saves.
                }
            }
        }
    }

    public FieldData getFieldData() {
        List<NameValuePair> value = new ArrayList<NameValuePair>();
        if (locationSelect.getSelectedValues().contains("lilly")) {
            addFieldPartValue(cabinetNumberField, CABINET_NUMBER, value);
            addFieldPartValue(drawerNumberField, DRAWER_NUMBER, value);
        } else if (locationSelect.getSelectedValues().contains("alf")) {
            addFieldPartValue(boxNumberField, BOX_NUMBER, value);
        } else if (locationSelect.getSelectedValues().contains("display")) {
            addFieldPartValue(outOnDisplayField, OUT_ON_DISPLAY, value);
        }
        if (value.isEmpty()) {
            return null;
        } else {
            return new FieldData(getFieldType(), null, Collections.singletonList(value));
        }
    }
    
    /**
     * The field is considered invalid if it contains any parts or attributes
     * that aren't known.
     */
    public boolean isValueValid(FieldData values) {
        for (List<NameValuePair> value : values.getParts()) {
            for (NameValuePair part : value) {
                if (!(getRequiredPartNames().contains(part.getName()))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void addFieldPartValue(TextField field, String partName, List<NameValuePair> parts) {
        if (field.getValue() != null && field.getValue().trim().length() > 0) {
            parts.add(new NameValuePair(partName, field.getValue().trim()));
        }
    }
    
    public String getValueSummary() {
        if (cabinetNumberField.getValue() != null && cabinetNumberField.getValue().trim().length() > 0) {
            return getMessage("summary-lilly", cabinetNumberField.getValue(), drawerNumberField.getValue());
        } else if (boxNumberField.getValue() != null && boxNumberField.getValue().trim().length() > 0) {
            return getMessage("summary-alf", boxNumberField.getValue());
        } else if (outOnDisplayField.getValue() != null && outOnDisplayField.getValue().trim().length() > 0) {
            return getMessage("summary-display", outOnDisplayField.getValue());
        } else {
            return "";
        }
    }
    
    /**
     * Summarizes the relevant bits of the location field, possibly including an HTML link 
     * for puzzles at the ALF.
     * TODO: the record isn't public, so we can't link to it here.
     */
    public String getValueSummaryHtml() {
        if (boxNumberField.getValue() != null && boxNumberField.getValue().trim().length() > 0) {
            return getMessage("summary-alf-html", boxNumberField.getValue());
        } else {
            return new Format().html(getValueSummary());
        }
    }

    public Control asClickControl() {
        return this;
    }

    public boolean suppressIfEmptyAndReadOnly() {
        return false;
    }

    public boolean hasDerivativeParts() {
        return false;
    }

}
