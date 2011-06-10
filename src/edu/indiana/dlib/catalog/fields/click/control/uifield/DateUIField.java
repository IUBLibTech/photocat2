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

package edu.indiana.dlib.catalog.fields.click.control.uifield;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.click.control.Checkbox;
import org.apache.click.control.TextField;

import edu.indiana.dlib.catalog.config.ConfigurationException;
import edu.indiana.dlib.catalog.config.FieldConfiguration;
import edu.indiana.dlib.catalog.config.FieldData;
import edu.indiana.dlib.catalog.config.FieldDefinition;
import edu.indiana.dlib.catalog.config.NameValuePair;
import edu.indiana.dlib.catalog.fields.click.control.AbstractUIField;
import edu.indiana.dlib.catalog.fields.click.control.FieldExposingAttributesContainer;
import edu.indiana.dlib.catalog.fields.click.control.ValuePreservingFieldAttributesContainer;

/**
 * A simple implementation of a date input box.  A date field is
 * displayed and maps to the "entered date" part of the data.  If
 * the date is of the format "yyyy", "yyyy-mm" or "yyyy-mm-dd", year
 * month and day values are parsed out and stored in the "year",
 * "month" and "day" part of the data respectively.
 */
public class DateUIField extends AbstractUIField {
    
    private static final String ENTERED_DATE = "entered date";
    private static final String YEAR = "year";
    private static final String MONTH = "month";
    private static final String DAY = "day";
    private static final String APPROXIMATE = "approximate";
    
    public DateUIField(FieldDefinition def, FieldConfiguration conf) throws ConfigurationException {
        super(def, conf);
        
        RepeatableValueGroupContainer vc = new RepeatableValueGroupContainer(conf, def);
        
        TextField enteredDateField = new TextField();
        enteredDateField.setWidth("10em"); // TODO: move this to the configuration
        vc.setPartField(ENTERED_DATE, enteredDateField);
        
        valuesContainer = vc;
        add(valuesContainer);
        
        if (!conf.isAttributeDisabled("approximate")) {
            FieldExposingAttributesContainer ac = new FieldExposingAttributesContainer(conf);
            Checkbox approximateCheckbox = new Checkbox();
            approximateCheckbox.setLabel(getMessage("approximate"));
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
                    Pattern w3cdtfPattern = Pattern.compile("(\\d+)(\\-(\\d\\d))?(\\-(\\d\\d))?");
                    Matcher dateMatcher = w3cdtfPattern.matcher(part.getValue());
                    if (dateMatcher.matches()) {
                        String year = dateMatcher.group(1);
                        if (year.length() != 0) {
                            value.add(new NameValuePair(YEAR, year));
                        }
                        String month = dateMatcher.group(3);
                        if (month != null && month.length() != 0) {
                            value.add(new NameValuePair(MONTH, month));
                        }
                        String day = dateMatcher.group(5);
                        if (day != null && day.length() != 0) {
                            value.add(new NameValuePair(DAY, day));
                        }
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
}
