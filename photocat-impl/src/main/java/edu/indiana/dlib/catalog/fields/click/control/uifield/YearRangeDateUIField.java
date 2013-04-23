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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.indiana.dlib.catalog.config.CollectionConfiguration;
import edu.indiana.dlib.catalog.config.ConfigurationException;
import edu.indiana.dlib.catalog.config.FieldConfiguration;
import edu.indiana.dlib.catalog.config.FieldData;
import edu.indiana.dlib.catalog.config.FieldDefinition;
import edu.indiana.dlib.catalog.config.NameValuePair;
import edu.indiana.dlib.catalog.fields.click.control.AbstractUIField;
import edu.indiana.dlib.catalog.fields.click.control.ValuePreservingFieldAttributesContainer;

/**
 * A UI Field that accepts a single text input, but validates
 * and parses it into multiple parts/attributes.
 */
public class YearRangeDateUIField extends AbstractUIField {

    private static final String INTERNAL_PART_NAME = "INTERNAL_USE_ONLY";
    
    public YearRangeDateUIField(FieldDefinition def, FieldConfiguration conf, CollectionConfiguration c) throws ConfigurationException {
        super(def, conf, c);
        attributesContainer = new ValuePreservingFieldAttributesContainer();
        add(attributesContainer);
        
        RepeatableValueGroupContainer vc = new RepeatableValueGroupContainer(conf, def);
        YearRangeDateTextField textField = new YearRangeDateTextField(this.getFieldConfiguration().getFieldType() + "_value");
        textField.setWidth("32em");
        vc.setPartField(INTERNAL_PART_NAME, textField);
        valuesContainer = vc;
        add(valuesContainer);
    }
    
    /**
     * Returns the parts that must be indicated in the specification (but not necessarily the value).
     */
    public Collection<String> getRequiredPartNames() {
        return Arrays.asList(new String[] { "year", "range first year", "range last year" });
    }

    /**
     * Returns the required attribute "approximate".
     */
    public Collection<String> getRequiredAttributeNames() {
        return Collections.singleton("approximate");
    }
    
    /**
     * Implements UIField, this method merges the supplied field
     * values into the simple one-field format exposed for editing
     * and passes that new FieldData object to the super class.
     */
    public void setFieldData(FieldData values) {
        if (values == null) {
            super.setFieldData(values);
        } else {
            // merge the values
            FieldData mergedValues = new FieldData(values.getFieldType());
            String approximate = "";
            for (NameValuePair attribute : values.getAttributes()) {
                if (attribute.getName().equals("approximate")) {
                    approximate = attribute.getValue();
                } else {
                    // this is an unrecognized and invalid attribute...
                }
            }
            for (List<NameValuePair> value : values.getParts()) {
                String year = null;
                String rangeStart = null;
                String rangeEnd = null;
                for (NameValuePair part : value) {
                    if (part.getName().equals("year")) {
                        year = part.getValue();
                    } else if (part.getName().equals("range first year")) {
                        rangeStart = part.getValue();
                    } else if (part.getName().equals("range last year")) {
                        rangeEnd = part.getValue();
                    }
                }
                if (year != null && rangeStart == null && rangeEnd == null) {
                    mergedValues.addValue(new NameValuePair(INTERNAL_PART_NAME, approximate + year));
                } else if (year == null && rangeStart != null && rangeEnd != null) {
                    mergedValues.addValue(new NameValuePair(INTERNAL_PART_NAME, approximate + rangeStart + "-" + rangeEnd));
                } else {
                    // invalid combination
                    throw new RuntimeException("Invalid field value! (" + approximate + ", " + year + ", " + rangeStart + ", " + rangeEnd + ")");
                }
            }
            super.setFieldData(mergedValues);
        }
    }
    
    /**
     * Implements UIField, this method invokes the super class implementation
     * then parses and splits up the simplified one-field format into the 
     * multi-part format of the storage model.
     */
    public FieldData getFieldData() {
        FieldData data = super.getFieldData();
        if (data == null) {
            return data;
        } else {
            FieldData processed = new FieldData(data.getFieldType());
            // split the values
            for (String entered : data.getPartValues(INTERNAL_PART_NAME)) {
                Matcher yearOnlyMatcher = Pattern.compile("\\d\\d\\d\\d").matcher(entered);
                Matcher yearRangeMatcher = Pattern.compile("(\\d\\d\\d\\d)\\Q-\\E(\\d\\d\\d\\d)").matcher(entered);
                Matcher yearApproximateMatcher = Pattern.compile("(\\Qca. \\E)(\\d\\d\\d\\d)").matcher(entered);
                if (yearOnlyMatcher.matches()) {
                    processed.addValue(new NameValuePair("year", entered));
                } else if (yearRangeMatcher.matches()) {
                    processed.addValue(new NameValuePair("range first year", yearRangeMatcher.group(1)), new NameValuePair("range last year", yearRangeMatcher.group(2)));
                } else if (yearApproximateMatcher.matches()) {
                    processed.setAttributes(Collections.singletonList(new NameValuePair("approximate", yearApproximateMatcher.group(1))));
                    processed.addValue(new NameValuePair("year", yearApproximateMatcher.group(2)));
                }
            }
            return processed;
        }
    }
    
    /**
     * Returns the merged values.
     */
    public String getValueSummary() {
        StringBuffer sb = new StringBuffer();
        for (String value : super.getFieldData().getPartValues(INTERNAL_PART_NAME)) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(value);
        }
        return sb.toString();
    }

}
