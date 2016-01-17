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
package edu.indiana.dlib.catalog.dataimport;

import java.util.ArrayList;
import java.util.List;

import org.apache.click.Control;
import org.apache.click.control.Container;
import org.apache.click.control.Field;

import edu.indiana.dlib.catalog.config.CollectionConfiguration;
import edu.indiana.dlib.catalog.config.ConfigurationException;
import edu.indiana.dlib.catalog.config.FieldData;
import edu.indiana.dlib.catalog.config.FieldDefinition;
import edu.indiana.dlib.catalog.config.ItemMetadata;
import edu.indiana.dlib.catalog.config.NameValuePair;
import edu.indiana.dlib.catalog.fields.UIField;

/**
 * A class that encapsulates the mapping between Record objects 
 * and ItemMetadata objects.
 */
public class FieldMapping {

    public enum ConflictResolution {
        OVERWRITE,
        ADD_ADDITIONAL_VALUE,
        //APPEND,
        RETAIN_ORIGINAL;
    }
    
    private CollectionConfiguration collection;
    
    /**
     * A mapping from {@link Record} field names to corresponding 
     * {@link edu.indiana.dlib.catalog.configuration.ItemMetadata}
     * field types for those fields that have exactly one
     * exposed part and no exposed attributes.
     */
    private List<FieldLink> fieldLinks;

    private String idFieldName;
    
    public FieldMapping(CollectionConfiguration c) {
        fieldLinks = new ArrayList<FieldLink>();
        collection = c;
    }
    
    public void setIdField(String idRecordFieldName) {
        this.idFieldName = idRecordFieldName;
    }
    
    /**
     * Links a field in the record to an ItemMetadata field/part.
     * @param recordFieldName the name of the field in the Record
     * @param imFieldDefinition the definition of the field to
     * which it maps.
     * @throws IllegalArgumentException if the FieldDefinition contains any attributes
     * or more than one part.
     */
    public void addFieldPartMapping(String recordFieldName, FieldDefinition imFieldDefinition, String partName, ConflictResolution ifConflict) {
        if (!imFieldDefinition.getDataSpecification().getValidPartNames().contains(partName)) {
            throw new IllegalArgumentException(imFieldDefinition.getType() + " does not support the part \"" + partName + "\"!");
        }
        FieldLink link = new FieldLink();
        link.recordFieldName = recordFieldName;
        link.imDefId = imFieldDefinition.getDefinitions().getId();
        link.imFieldType = imFieldDefinition.getType();
        link.imPartName = partName;
        link.ifConflict = ifConflict;
        fieldLinks.add(link);
    }
    
    /**
     * Returns the ID field value for the Record.
     * @param record the record whose ID is in question
     * @return the id value from the record
     */
    public String getId(Record record) {
        return record.getValue(record.getMetadata().getIndexForName(idFieldName));
    }

    /**
     * Updates the provided 'currentItem' with values from the record.
     * @param record the record that serves as the source
     * @param currentItem the current value of the item (or null if none exists)
     * @return the value for the relevant field (a new object) possibly null if
     * the field has no value
     * @throws IllegalStateException if the record doesn't match the item
     */
    public void updatedItemMetadata(Record record, ItemMetadata currentItem) throws ConfigurationException {
        if (!currentItem.getId().equals(getId(record))) {
            throw new IllegalStateException("id mismatch");
        }
        for (FieldLink fieldLink : fieldLinks) {
            String newValue = record.getValue(fieldLink.recordFieldName);
            // if the newValue is empty, set it to null for
            // simplified logic below
            if (newValue != null) {
                newValue = newValue.trim();
                if (newValue.length() == 0) {
                    newValue = null;
                }
            }
            
            if (newValue == null) {
                // no value is specified in the spreadsheet, while one 
                // MIGHT want to interpret this as a desire to clear 
                // any existing value in the field, the potential for 
                // accidental and widespread destruction dictates that 
                // that functionality is not provided.
                continue;
            }

            if (ConflictResolution.OVERWRITE.equals(fieldLink.ifConflict) || currentItem.getFieldData(fieldLink.imFieldType) == null) {
                // if we're willing to overwrite existing values or there is no existing
                // value.... THEN do an update
                FieldData data = new FieldData(fieldLink.imFieldType);
                data.addValue(new NameValuePair(fieldLink.imPartName, newValue));

                currentItem.setFieldValue(fieldLink.imFieldType, getProcessedValidatedFieldData(data, fieldLink, newValue));
            } else if (ConflictResolution.ADD_ADDITIONAL_VALUE.equals(fieldLink.ifConflict)) {
                // if we wish to add the spreadsheet value to any 
                // existing values, then we do that.  Note: the caller
                // should have verified that the field is actually repeatable
                FieldData data = currentItem.getFieldData(fieldLink.imFieldType);
                if (data != null) {
                    // see if the value already exists
                    boolean exists = false;
                    for (List<NameValuePair> value : data.getParts()) {
                        for (NameValuePair part : value) {
                            if (part.getName().equals(fieldLink.imPartName) && part.getValue().equals(newValue)) {
                                exists = true;
                                break;
                            }
                        }
                    }
                    if (exists) {
                        // the value exists, go to the field link
                        continue;
                    }
                } else {
                    data = new FieldData(fieldLink.imFieldType);
                }
                data.addValue(new NameValuePair(fieldLink.imPartName, newValue));

                currentItem.setFieldValue(fieldLink.imFieldType, getProcessedValidatedFieldData(data, fieldLink, newValue));
            } else {
                // do nothing to the record in response to this FieldLink
            }
        }
    }
    
    private FieldData getProcessedValidatedFieldData(FieldData data, FieldLink fieldLink, String newValueFromImport) throws ConfigurationException {
        // push the data through the actual field in case the field
        // instance does something special (like parse out pieces)
        UIField field = collection.newInstance(fieldLink.imFieldType);
        field.setFieldData(data);
        
        if (!field.isValueValid(data)) {
            throw new RuntimeException("The value \"" + newValueFromImport + "\" is invalid for field " + data.getFieldType() + ".");
        }

        return field.getFieldData();
    }
    
    /**
     * A recursive method to validate any Fields
     * within the given control.  
     * 
     * THIS DOESN'T WORK AS WE'D LIKE BECAUSE OF THE
     * FUNNY BUSINESS THAT TAKES PLACE IN REPEATABLE FIELDS.
     */
    private void validateControl(Control control) {
        System.out.println(control.getClass().getName());
        if (control instanceof Container) {
            Container container = (Container) control;
            for (Control child : container.getControls()) {
                validateControl(child);
            }
        }
        if (control instanceof Field) {
            Field clickField = (Field) control;
            if (clickField.getValidate()) {
                clickField.validate();
                if (!clickField.isValid()) {
                    throw new RuntimeException("The value \"" + clickField.getValue() + "\" is not valid! " + clickField.getError());
                } else {
                    System.out.println(clickField.getValue() + " is a valid value!");
                }
            }
        }
    }
    
    /**
     * Determine whether the relevant value (if any) in the Record
     * should replace (overwrite) an existing value in the field. 
     * @param def the definition of the field to be updated
     * @return true if any existing values should be replaced, false if
     * new values should be combined with existing values.
     */
    public boolean shouldRecordDataReplaceField(FieldDefinition def) {
        return false;
    }
    
    private static class FieldLink {
        
        /**
         * The name of the field in the record.
         */
        private String recordFieldName;
        
        /**
         * The type of the field it maps to in item metadata.
         */
        private String imFieldType;
        
        /**
         * The definition id that defines the type of the field
         * it maps to in item metadata.
         */
        private String imDefId;

        /**
         * The part name to which this record value maps.
         */
        private String imPartName;
        
        /**
         * What should happen if the spreadsheet has a value
         * for a field and the item does as well.
         */
        private ConflictResolution ifConflict;
        
    }
}
