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

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;

import org.apache.click.control.Checkbox;
import org.apache.click.control.Option;
import org.apache.click.control.Select;
import org.apache.click.control.TextField;
import org.apache.click.extras.control.IntegerField;
import org.apache.click.extras.control.RegexField;

import edu.indiana.dlib.catalog.config.CollectionConfiguration;
import edu.indiana.dlib.catalog.config.ConfigurationException;
import edu.indiana.dlib.catalog.config.FieldConfiguration;
import edu.indiana.dlib.catalog.config.FieldDefinition;
import edu.indiana.dlib.catalog.fields.click.control.AbstractUIField;
import edu.indiana.dlib.catalog.fields.click.control.FieldExposingAttributesContainer;
import edu.indiana.dlib.catalog.fields.click.control.ValuePreservingFieldAttributesContainer;

public class ImpreciseDateUIField extends AbstractUIField {

    private static final String DECADE = "decade";
    private static final String YEAR = "year";
    private static final String MONTH = "month";
    private static final String DAY = "day";
    private static final String APPROXIMATE = "approximate";
    
    public ImpreciseDateUIField(FieldDefinition def, FieldConfiguration conf, CollectionConfiguration c) throws ConfigurationException {
        super(def, conf, c);
        
        RepeatableValueGroupContainer fc = new RepeatableValueGroupContainer(conf, def);
        IntegerField decadeField = new IntegerField();
        decadeField.setSize(4);
        fc.setPartField(DECADE, decadeField);
        IntegerField yearField = new IntegerField();
        yearField.setSize(4);
        fc.setPartField(YEAR, yearField);
        Select monthSelect = new Select();
        monthSelect.add("");
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMM (MM)");
        for (int i = 0; i < 12; i ++) {
            cal.set(Calendar.MONTH, i);
            monthSelect.add(new Option(i + 1, monthFormat.format(cal.getTime())));
        }
        fc.setPartField(MONTH, monthSelect);
        IntegerField dayField = new IntegerField();
        dayField.setSize(2);
        fc.setPartField(DAY, dayField);
        
        fc.setShowingLabels(true);
        valuesContainer = fc;
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
    public Collection<String> getOptionalPartNames() {
        return Arrays.asList(new String[] { DECADE, YEAR, MONTH, DAY });
    }
    
    public boolean hasDerivativeParts() {
        return true;
    }

}
