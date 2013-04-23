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

import org.apache.click.control.Checkbox;

import edu.indiana.dlib.catalog.config.CollectionConfiguration;
import edu.indiana.dlib.catalog.config.ConfigurationException;
import edu.indiana.dlib.catalog.config.FieldConfiguration;
import edu.indiana.dlib.catalog.config.FieldData;
import edu.indiana.dlib.catalog.config.FieldDefinition;
import edu.indiana.dlib.catalog.config.NameValuePair;
import edu.indiana.dlib.catalog.fields.click.control.AbstractUIField;
import edu.indiana.dlib.catalog.fields.click.control.FieldExposingAttributesContainer;
import edu.indiana.dlib.catalog.fields.click.control.ValuePreservingFieldAttributesContainer;

/**
 * A UI Field that accepts a single text input, but validates
 * and parses it into multiple parts/attributes.
 */
public class RobustDateUIField extends AbstractUIField {

    public static final String ENTERED_DATE = "entered date";
    private static final String YEAR = "year";
    private static final String MONTH = "month";
    private static final String DAY = "day";
    private static final String APPROXIMATE = "approximate";
    
    public RobustDateUIField(FieldDefinition def, FieldConfiguration conf, CollectionConfiguration c) throws ConfigurationException {
        super(def, conf, c);
        
        RepeatableValueGroupContainer vc = new RepeatableValueGroupContainer(conf, def);
        RobustDateTextField textField = new RobustDateTextField(this.getFieldConfiguration().getFieldType() + "_value");
        textField.setWidth("10em");
        vc.setPartField(ENTERED_DATE, textField);
        valuesContainer = vc;
        add(valuesContainer);
        
        if (!conf.isAttributeDisabled("approximate")) {
            FieldExposingAttributesContainer ac = new FieldExposingAttributesContainer(conf);
            Checkbox approximateCheckbox = new Checkbox();
            approximateCheckbox.setLabel("approximate");
            ac.setAttributeField("approximate", approximateCheckbox);
            attributesContainer = ac;
            add(attributesContainer);
        } else {
            attributesContainer = new ValuePreservingFieldAttributesContainer();
            add(attributesContainer);
        }
    }
    
    /**
     * Returns the optional attribute "approximate".
     */
    public Collection<String> getOptionalAttributeNames() {
        return Collections.singleton(APPROXIMATE);
    }
    
    /**
     * Returns the required parts, ENTERED_DATE, YEAR, MONTH and DAY.
     */
    public Collection<String> getRequiredPartNames() {
        return Arrays.asList(new String[] { ENTERED_DATE, YEAR, MONTH, DAY });
    }
    
        
    /**
     * Delegates to the superclass, but then modifies the result 
     * to extract fielded data from parsable entered dates.
     */
    public FieldData getFieldData() {
        FieldData data = super.getFieldData();
        List<List<NameValuePair>> values = new ArrayList<List<NameValuePair>>();
        for (List<NameValuePair> value : data.getParts()) {
            for (NameValuePair part : new ArrayList<NameValuePair>(value)) {
                if (part.getName().equals(ENTERED_DATE)) {
                	String year = RobustDateTextField.getYear(part.getValue());
                	String month = RobustDateTextField.getMonth(part.getValue());
                	String day = RobustDateTextField.getDay(part.getValue());
                	if (year != null) {
                		value.add(new NameValuePair(YEAR, year));
                	}
                	if (month != null) {
                		value.add(new NameValuePair(MONTH, month));
                	}
                	if (day != null) {
                		value.add(new NameValuePair(DAY, day));
                	}
                } else if (part.getName().equals(YEAR) || part.getName().equals(MONTH) || part.getName().equals(DAY)) {
                    // skip this part, it is derived entirely from the entered date.
                } else {
                    throw new IllegalStateException("Unexpected part, \"" + part.getName() + "\"!");
                }
            }
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        data.getParts().clear();
        data.getParts().addAll(values);
        if (values.isEmpty()) {
            data.getAttributes().clear();
        }
        return data;
    }
    
    /**
     * Validates the given value against the formatting
     * rules.
     */
    public boolean isValueValid(FieldData values) {
        for (String enteredDate : values.getPartValues(ENTERED_DATE)) {
            if (!RobustDateTextField.isValueValid(enteredDate)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Returns the entered values.
     */
    public String getValueSummary() {
        StringBuffer sb = new StringBuffer();
        FieldData fieldData = getFieldData();
        if (fieldData == null) {
            return "no value entered";
        } else {
            String prefix = "";
            for (NameValuePair attribute : fieldData.getAttributes()) {
                if (attribute.getName().equals(APPROXIMATE) && "true".equals(attribute.getValue())) {
                    prefix = "circa ";
                    break;
                }
            }
            for (String value : fieldData.getPartValues(ENTERED_DATE)) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(prefix);
                sb.append(value);
            }
            if (sb.length() == 0) {
                return "no value entered";
            } else {
                return sb.toString();
            }
        }
    }
    
    /**
     * Returns true because the "month", "year" and "day" parts
     * are derived from the entered-date.
     */
    public boolean hasDerivativeParts() {
        return true;
    }
    
}
