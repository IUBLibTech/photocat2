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
package edu.indiana.dlib.catalog.dataimport;

import java.util.ArrayList;
import java.util.List;

import edu.indiana.dlib.catalog.config.FieldData;
import edu.indiana.dlib.catalog.config.FieldDefinition;
import edu.indiana.dlib.catalog.config.ItemMetadata;

/**
 * A class that encapsulates the mapping between Record objects 
 * and ItemMetadata objects.
 */
public class FieldMapping {

    /**
     * A mapping from {@link Record} field names to corresponding 
     * {@link edu.indiana.dlib.catalog.configuration.ItemMetadata}
     * field types for those fields that have exactly one
     * exposed part and no exposed attributes.
     */
    private List<FieldLink> fieldLinks;

    private String idFieldName;
    
    public FieldMapping() {
        fieldLinks = new ArrayList<FieldLink>();
    }
    
    public void setIdField(String idRecordFieldName) {
        this.idFieldName = idRecordFieldName;
    }
    
    /**
     * Links a field in the record to an ItemMetadata field (which
     * must have no attributes and only one part).
     * @param recordFieldName the name of the field in the Record
     * @param imFieldDefinition the definition of the field to
     * which it maps.
     * @throws IllegalArgumentException if the FieldDefinition contains any attributes
     * or more than one part.
     */
    public void addSimpleFieldMapping(String recordFieldName, FieldDefinition imFieldDefinition) {
        if (imFieldDefinition.getDataSpecification().getValidAttributeNames() != null && !imFieldDefinition.getDataSpecification().getValidAttributeNames().isEmpty()) {
            throw new IllegalArgumentException(imFieldDefinition.getType() + " must not define any attributes to be considered a simple field!");
        }
        List<String> parts = imFieldDefinition.getDataSpecification().getValidPartNames();
        if (parts == null || parts.size() != 1) {
            throw new IllegalArgumentException(imFieldDefinition.getType() + " must define exactly one part to be considered a simple field!");
        }
        FieldLink link = new FieldLink();
        link.recordFieldName = recordFieldName;
        link.imDefId = imFieldDefinition.getDefinitions().getId();
        link.imFieldType = imFieldDefinition.getType();
        link.imPartName = parts.get(0);
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
    public void updatedItemMetadata(Record record, ItemMetadata currentItem) {
        if (!currentItem.getId().equals(getId(record))) {
            throw new IllegalStateException("id mismatch");
        }
        FieldData data = new FieldData(idFieldName);
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
         * The attribute name (or null if this record value maps
         * to a part) to which this record value maps.
         */
        private String imAttributeName;

        /**
         * The part name (or null if this record value maps to an
         * attribute) to which this record value maps.
         */
        private String imPartName;
        
    }
}
