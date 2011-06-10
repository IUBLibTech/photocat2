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
 *INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. 
 */
package edu.indiana.dlib.catalog.config;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.indiana.dlib.catalog.fields.UIField;

/**
 * A CollectionConfiguration which is bound the the appropriate 
 * FieldDefintions file linked.
 */
public class CollectionConfiguration extends CollectionConfigurationData {

    private List<FieldConfiguration> mergedFieldConfigurations;
    
    private Map<String, FieldDefinition> typeToDefinitionMap;
    
    private Definitions fieldDefintions;
    
    /**
     * A copy constructor to create a CollectionConfigurationData that
     * includes all values from the provided 'config' but replaces any
     * null values in default field configuration list with the defaults
     * in the provided FieldDefinition.
     * @throws ConfigurationException if the given configuration isn't
     * compatible with (or defined by) the definitions.
     */
    public CollectionConfiguration(CollectionConfigurationData config, Definitions definitions) {
        super(config);
        if (!definitions.getId().equals(this.getDefinitionId())) {
            throw new IllegalArgumentException("The provided definition is not appropriate for the given configuration! (\"" + config.getDefinitionId() + "\" != \"" + definitions.getId() + "\")");
        }
        this.fieldDefintions = definitions;
        
        this.mergedFieldConfigurations = new ArrayList<FieldConfiguration>();
        this.typeToDefinitionMap = new HashMap<String, FieldDefinition>();
        for (FieldConfiguration field : super.listFieldConfigurations()) {
            FieldDefinition def = definitions.getFieldDefinition(field.getFieldType());
            if (def != null) {
                this.mergedFieldConfigurations.add(new FieldConfiguration(field, def.getDefaultConfiguration()));
                this.typeToDefinitionMap.put(field.getFieldType(), def);
            } else {
                // this field is undefined, meaning it is a configuration
                // referencing a field that doesn't exist.
                this.mergedFieldConfigurations.add(field);
            }
        }
    }
    
    public List<FieldConfiguration> listFieldConfigurations() {
        return Collections.unmodifiableList(this.mergedFieldConfigurations);
    }
    
    /**
     * Gets a simple one-line String representation of the 
     * "value" of this field.  This is likely a lossy representation
     * of the field that is appropriate for search results 
     * or other quick summaries of the field.
     * 
     * If there's a problem with the configuration, this method may 
     * return null.
     */
    public String getValueSummary(ItemMetadata item, String fieldType) {
        try {
            if (item.getCollectionId().equals(this.getCollectionMetadata().getId())) {
                if (this.getFieldConfiguration(fieldType) != null) {
                    UIField field = this.newInstance(fieldType);
                    FieldData fieldData = item.getFieldData(fieldType);
                    if (fieldData == null) {
                        return null;
                    } else {
                        field.setFieldData(fieldData);
                        return field.getValueSummary();
                    }
                } else {
                    throw new IllegalArgumentException("The provided fieldType, \"" + fieldType + "\", is not configured for the \"" + this.getCollectionMetadata().getId() + "\" collection!");
                }
            } else {
                throw new IllegalArgumentException("The provided item, \"" + item.getId() + "\", is not a member of the \"" + this.getCollectionMetadata().getId() + "\" collection!");
            }
        } catch (ConfigurationException ex) {
            return null;
        }
    }
    
    public UIField newInstance(String fieldType) throws ConfigurationException {
        // get the JavaImplementation
        FieldDefinition def = this.fieldDefintions.getFieldDefinition(fieldType);
        if (def == null) {
            throw new IllegalArgumentException("Undefined fieldType, \"" + fieldType + "\".");
        } else {
            try {
                // instantiate the class
                Class fieldClass = Class.forName(def.getJavaImplementation().getJavaClassName());
                if (UIField.class.isAssignableFrom(fieldClass)) {
                    Object fieldClassInstance = fieldClass.getConstructor(FieldDefinition.class, FieldConfiguration.class).newInstance(def, this.getFieldConfiguration(def.getType()));
                    // add any properties
                    if (!def.getJavaImplementation().getJavaClassProperties().isEmpty()) {
                        // implement this when we actually have a use case
                        throw new UnsupportedOperationException("Properties are not yet supported!");
                    }
                    
                    return (UIField) fieldClassInstance;
                } else {
                    throw new ConfigurationException("Configured class, \"" + def.getJavaImplementation().getJavaClassName() + "\" for fieldType \"" + fieldType + "\" is not an instance of " + UIField.class.getName() + ".");
                }
            } catch (ClassNotFoundException ex) {
                throw new ConfigurationException(ex);
            } catch (SecurityException ex) {
                throw new ConfigurationException(ex);
            } catch (IllegalArgumentException ex) {
                throw new ConfigurationException(ex);
            } catch (InstantiationException ex) {
                throw new ConfigurationException(ex);
            } catch (IllegalAccessException ex) {
                throw new ConfigurationException(ex);
            } catch (InvocationTargetException ex) {
                throw new ConfigurationException(ex);
            } catch (NoSuchMethodException ex) {
                throw new ConfigurationException(ex);
            } 
        }
    }
    
    public FieldConfiguration getFieldConfiguration(String fieldType) {
        for (FieldConfiguration config : this.mergedFieldConfigurations) {
            if (config.getFieldType().equals(fieldType)) {
                return config;
            }
        }
        return null;
    }
    
    public FieldDefinition getFieldDefinition(String fieldType) {
        return this.typeToDefinitionMap.get(fieldType);
    }
    
    public SourceDefinition getSourceDefinition(String type) {
        return this.fieldDefintions.getSourceDefinition(type);
    }
    
    public Definitions getDefinitions() {
        return this.fieldDefintions;
    }

}
