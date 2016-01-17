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
package edu.indiana.dlib.catalog.config;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.indiana.dlib.catalog.config.impl.IdGenerator;
import edu.indiana.dlib.catalog.fields.UIField;

/**
 * A CollectionConfiguration which is bound the the appropriate 
 * FieldDefintions file linked.
 */
public class CollectionConfiguration extends CollectionConfigurationData {

    private Logger LOGGER = Logger.getLogger(CollectionConfiguration.class);
    
    private List<FieldConfiguration> mergedFieldConfigurations;
    
    private Map<FieldConfiguration, FieldDefinition> confToDefinitionMap;
    
    /**
     * A mapping from definition id to the data structure containing
     * the full field definition specification.  This is checked to
     * ensure that it contains definitions for every field that is 
     * part of this configuration.
     */
    private Map<String, Definitions> fieldDefinitionsMap;
    
    private IdGenerator idGenerator;
    
    /**
     * A copy constructor to create a CollectionConfigurationData that
     * includes all values from the provided 'config' but replaces any
     * null values in default field configuration list with the defaults
     * in the appropriate field definition.
     * @throws ConfigurationException if the given configuration isn't
     * contains fields that aren't compatible with (or defined by) the
     * provided definitions.
     */
    public CollectionConfiguration(CollectionConfigurationData config, Collection<Definitions> defsCollection) {
        super(config);
        fieldDefinitionsMap = new HashMap<String, Definitions>();
        for (Definitions def : defsCollection) {
            if (fieldDefinitionsMap.containsKey(def.getId())) {
                throw new IllegalArgumentException("More than one definition were provided with the id \"" + def.getId() + "\"!");
            }
            fieldDefinitionsMap.put(def.getId(), def);
        }
        mergedFieldConfigurations = new ArrayList<FieldConfiguration>();
        confToDefinitionMap = new HashMap<FieldConfiguration, FieldDefinition>();
        for (FieldConfiguration field : super.getFieldConfigurations()) {
            Definitions definitions = fieldDefinitionsMap.get(field.getDefinitionId());
            if (definitions == null) {
                throw new IllegalArgumentException("No field definitions found with id \"" + field.getDefinitionId() + "\"!");
            }
            FieldDefinition fieldDef = definitions.getFieldDefinition(field.getFieldType());
            if (fieldDef != null) {
                mergedFieldConfigurations.add(new FieldConfiguration(field, fieldDef.getDefaultConfiguration()));
                confToDefinitionMap.put(field, fieldDef);
            } else {
                // this field is undefined, meaning it is a configuration
                // referencing a field that doesn't exist.
                mergedFieldConfigurations.add(field);
                throw new RuntimeException("Undefined field! " + field.getFieldType() + ", " + field.getDefinitionId() + "!");
            }
        }
        Collections.sort((List<FieldConfiguration>) mergedFieldConfigurations, new Comparator<FieldConfiguration>() {
            public int compare(FieldConfiguration o1, FieldConfiguration o2) {
                return o1.getCatalogingSortIndex() - o2.getCatalogingSortIndex();
            }});
        if (collectionMetadata.getRecordCreationProperties() != null) {
            try {
                idGenerator = new IdGenerator(collectionMetadata.getRecordCreationProperties());
            } catch (MalformedURLException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
    
    /**
     * Gets a list of the FieldConfiguration objects for this configuration.
     * If "includePrivate" is true, they will be sorted as appropriate for
     * the cataloging view, otherwise they will be sorted for public view and
     * only include those for which "isDisplayedInDiscoveryFullView()" is true.
     */
    public List<FieldConfiguration> listFieldConfigurations(boolean includePrivate) {
        if (includePrivate) {
            return Collections.unmodifiableList(mergedFieldConfigurations);
        } else {
            List<FieldConfiguration> result = new ArrayList<FieldConfiguration>();
            for (FieldConfiguration c : mergedFieldConfigurations) {
                if (c.isDisplayedInDiscoveryFullView()) {
                    result.add(c);
                }
            }
            
            Collections.sort(result, new Comparator<FieldConfiguration>() {

                public int compare(FieldConfiguration o1, FieldConfiguration o2) {
                    return o1.getPublicSortIndex() - o2.getPublicSortIndex();
                }});
            return result;
        }
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
            if (item.getCollectionId().equals(getCollectionMetadata().getId())) {
                if (getFieldConfiguration(fieldType) != null) {
                    UIField field = newInstance(fieldType);
                    FieldData fieldData = item.getFieldData(fieldType);
                    if (fieldData == null) {
                        return null;
                    } else {
                        field.setFieldData(fieldData);
                        return field.getValueSummary();
                    }
                } else {
                    throw new IllegalArgumentException("The provided fieldType, \"" + fieldType + "\", is not configured for the \"" + getCollectionMetadata().getId() + "\" collection!");
                }
            } else {
                throw new IllegalArgumentException("The provided item, \"" + item.getId() + "\", is not a member of the \"" + getCollectionMetadata().getId() + "\" collection!");
            }
        } catch (ConfigurationException ex) {
            LOGGER.warn("Error generating value summary for field type \"" + fieldType + "\"!", ex);
            return null;
        }
    }
    
    /**
     * Gets a valid XHTML fragment representation of the 
     * "value" of this field.  This may be a lossy representation
     * of the field, but is the most complete representation for
     * human readability.
     * 
     * If there's a problem with the configuration, this method may 
     * return null.
     */
    public String getValueSummaryHtml(ItemMetadata item, String fieldType) {
        try {
            if (item.getCollectionId().equals(getCollectionMetadata().getId())) {
                if (getFieldConfiguration(fieldType) != null) {
                    UIField field = newInstance(fieldType);
                    FieldData fieldData = item.getFieldData(fieldType);
                    if (fieldData == null) {
                        return null;
                    } else {
                        field.setFieldData(fieldData);
                        return field.getValueSummaryHtml();
                    }
                } else {
                    throw new IllegalArgumentException("The provided fieldType, \"" + fieldType + "\", is not configured for the \"" + getCollectionMetadata().getId() + "\" collection!");
                }
            } else {
                throw new IllegalArgumentException("The provided item, \"" + item.getId() + "\", is not a member of the \"" + getCollectionMetadata().getId() + "\" collection!");
            }
        } catch (ConfigurationException ex) {
            return null;
        }
    }
    
    public UIField newInstance(String fieldType) throws ConfigurationException {
        // get the JavaImplementation
        FieldConfiguration conf = getFieldConfiguration(fieldType);
        FieldDefinition def = confToDefinitionMap.get(conf);
        if (def == null) {
            throw new IllegalArgumentException("Undefined field, \"" + conf.getFieldType() + "\" defined in \"" + conf.getDefinitionId() + "\".");
        } else {
            try {
                // instantiate the class
                Class fieldClass = Class.forName(def.getJavaImplementation().getJavaClassName());
                if (UIField.class.isAssignableFrom(fieldClass)) {
                    Object fieldClassInstance = fieldClass.getConstructor(FieldDefinition.class, FieldConfiguration.class, CollectionConfiguration.class).newInstance(def, getFieldConfiguration(def.getType()), this);
                    // add any properties
                    if (!def.getJavaImplementation().getJavaClassProperties().isEmpty()) {
                        // implement this when we actually have a use case
                        throw new UnsupportedOperationException("Properties are not yet supported!");
                    }
                    
                    return (UIField) fieldClassInstance;
                } else {
                    throw new ConfigurationException("Configured class, \"" + def.getJavaImplementation().getJavaClassName() + "\" for fieldType \"" + conf.getFieldType() + "\" is not an instance of " + UIField.class.getName() + ".");
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
        for (FieldConfiguration config : mergedFieldConfigurations) {
            if (config.getFieldType().equals(fieldType)) {
                return config;
            }
        }
        return null;
    }
    
    public FieldDefinition getFieldDefinition(FieldConfiguration conf) {
        return confToDefinitionMap.get(conf);
    }
    
    public SourceDefinition getSourceDefinition(FieldConfiguration conf, String type) {
        Definitions d = fieldDefinitionsMap.get(conf.getDefinitionId());
        return d.getSourceDefinition(type);
    }
    
    public Definitions getDefinitions(FieldConfiguration conf) {
        return fieldDefinitionsMap.get(conf.getDefinitionId());
    }
        
    /**
     * Gets the human-readable name for a part of a field.  Throughout
     * the user application it's necessary to present just a part of a
     * field (for instance when browsing) but the logic to present a 
     * reasonable name isn't trivial.  This method consolidates that
     * logic in one place.
     * 
     * The current implementation does the following:
     * 1.  If there is only a single part of the field, or if the field 
     *     only has two parts and one is bound to an authority, this
     *     method returns the specified display name for the whole 
     *     field with precedence for that value in the configuration over
     *     the definition.
     * 2.  field name -- part name
     */
    public String getFieldPartName(String fieldType, String partName) {
        FieldConfiguration fieldConf = getFieldConfiguration(fieldType);
        FieldDefinition fieldDef = getFieldDefinition(fieldConf);
        if (fieldConf == null || fieldDef == null) {
            // this is invalid but we'll degrade gracefully
            return fieldType + " -- " + partName;
        } else {
            List<String> validPartNames = fieldDef.getDataSpecification().getValidPartNames();
            if (!validPartNames.contains(partName)) {
                // this is invalid but we'll degrade gracefully
                return fieldConf.getDisplayLabel() + " -- " + partName;
            } else {
                List<String> relevantPartNames = getBrowsablePartNames(fieldConf);
                if (relevantPartNames.size() == 1 && relevantPartNames.contains(partName)) {
                    // this is the single important part of the field, just present the
                    // field display label
                    return fieldConf.getDisplayLabel();
                } else {
                    return fieldConf.getDisplayLabel() + " -- " + fieldConf.getPartDisplayLabel(partName);
                }
            }
        }
        
    }
    
    /**
     * Gets an ItemMetadata object representing all the public
     * or unrecognized fields.  This method will never return
     * null even if there are no public fields.
     */
    public ItemMetadata getPublicItemMetadata(ItemMetadata im) {
    	ItemMetadata publicIm = new ItemMetadata(im.getId(), im.getCollectionId());
    	for (String type : im.getRepresentedFieldTypes()) {
    		FieldConfiguration conf = getFieldConfiguration(type);
    		if (conf != null && conf.isPrivate()) {
    			// skip this recognized private field
    		} else {
    			// copy this unrecognized or public field
    		    publicIm.setFieldValue(type, im.getFieldData(type));
    		}
    	}
    	return publicIm;
    }
    
    /**
     * Gets an ItemMetadata object representing just the identifying
     * information and the private fields for the provided 
     * ItemMetadata object.
     */
    public ItemMetadata getPrivateItemMetadata(ItemMetadata im) {
    	ItemMetadata privateIm = new ItemMetadata(im.getId(), im.getCollectionId());
    	for (String type : im.getRepresentedFieldTypes()) {
    		FieldConfiguration conf = getFieldConfiguration(type);
    		if (conf != null && conf.isPrivate()) {
    		    // copy this recognized private field
    			privateIm.setFieldValue(type, im.getFieldData(type));
    		} else {
    			// skip this unrecognized or public field
    		}
    	}
    	return privateIm;
    }

    public List<String> getEnabledAttributeNames(FieldConfiguration fieldConf) {
        FieldDefinition fieldDef = getFieldDefinition(fieldConf);
        if (fieldConf != null && fieldDef != null) {
            List<String> relevantAttributeNames = new ArrayList<String>(fieldDef.getDataSpecification().getValidAttributeNames());
            relevantAttributeNames.removeAll(fieldConf.listDisabledAttributes());
            return relevantAttributeNames;
        } else {
            return Collections.emptyList();
        }
    }
    
    public List<String> getEnabledPartNames(FieldConfiguration fieldConf) {
        FieldDefinition fieldDef = getFieldDefinition(fieldConf);
        if (fieldConf != null && fieldDef != null) {
            List<String> relevantPartNames = new ArrayList<String>(fieldDef.getDataSpecification().getValidPartNames());
            relevantPartNames.removeAll(fieldConf.listDisabledParts());
            return relevantPartNames;
        } else {
            return Collections.emptyList();
        }
    }
    
    public List<String> getBrowsablePartNames(FieldConfiguration fieldConf) {
        FieldDefinition fieldDef = getFieldDefinition(fieldConf);
        if (fieldConf != null && fieldDef != null) {
            List<String> relevantPartNames = new ArrayList<String>(fieldDef.getDataSpecification().getValidPartNames());
            relevantPartNames.removeAll(fieldConf.listDisabledParts());
            if (fieldConf.getVocabularySources() != null) {
                for (VocabularySourceConfiguration vc : fieldConf.getVocabularySources()) {
                    if (vc.getAuthorityBinding() != null) {
                        relevantPartNames.remove(vc.getAuthorityBinding());
                    }
                }
            }
            return relevantPartNames;
        } else {
            return Collections.emptyList();
        }
    }
    
    public IdGenerator getIdGenerator() {
        return idGenerator;
    }
}
