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
package edu.indiana.dlib.catalog.fields;

import org.apache.click.Control;

import edu.indiana.dlib.catalog.config.FieldConfiguration;
import edu.indiana.dlib.catalog.config.FieldData;
import edu.indiana.dlib.catalog.config.FieldDefinition;

/**
 * A user interface compatible version of a Field.  Configuration 
 * information is reflected in the appearance and behavior of
 * this user interface component.
 * 
 * Implementations of this class must also have a constructor that
 * takes the following parameters:
 * <ul>
 *   <li>FieldDefinition - the definition for the field</li>
 *   <li>FieldConfiguration - the configuration for the field</li>
 * </ul>
 */
public interface UIField {
    
    /**
     * Gets the type of field represented by this object.
     */
    public String getFieldType();

    /**
     * Gets the FieldDefinition for this object.
     */
    public FieldDefinition getFieldDefinition();
    
    /**
     * Gets the FieldConfiguration for this object.  This is the
     * consolidated configuration that incorporates the configuration
     * values as well as the default values from the FieldDefinition.
     */
    public FieldConfiguration getFieldConfiguration();
       
    /**
     * Sets the default "value" of the control.  Subclasses may decide
     * how best to present this information.
     */
    public void setDefaultValue(FieldData values);
    
    /**
     * Sets the "value" of the control.  By convention, all bits 
     * of data contained in the FieldData object should be displayed
     * in the control.  Those parts that may be unrecognized 
     * may be displayed in a read-only fashion but should be
     * displayed to the user.
     */
    public void setFieldData(FieldData values);
    
    /**
     * Gets the "value" of this control translated into a FieldData
     * object.
     */
    public FieldData getFieldData();
    
    /**
     * Gets a simple one-line String representation of the 
     * "value" of this field.  This is likely a lossy representation
     * of the field that is appropriate for search results 
     * or other quick summaries of the field.
     */
    public String getValueSummary();
    
    /**
     * Gets the Control instance of this field for use in a 
     * Click web application framework.  All the methods in this
     * interface affect and are affected by the Control returned
     * by this method.  Therefore there may be times in which its
     * inappropriate to invoke setFieldData() as it may overwrite
     * values entered by the user.
     */
    public Control asClickControl();
    
    /**
     * A hint that if this control is empty (has a null or empty value)
     * the rendering of it may be skipped.  It only makes sense to do so
     * for read-only fields as typically a read-write field that has no
     * value will allow the user to enter a new one.
     */
    public boolean suppressIfEmptyAndReadOnly();
    
}
