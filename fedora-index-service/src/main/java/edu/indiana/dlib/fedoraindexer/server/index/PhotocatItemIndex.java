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
package edu.indiana.dlib.fedoraindexer.server.index;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Properties;

import org.apache.lucene.document.Document;

import edu.indiana.dlib.catalog.config.CollectionConfiguration;
import edu.indiana.dlib.catalog.config.DocumentHelper;
import edu.indiana.dlib.catalog.config.FieldData;
import edu.indiana.dlib.catalog.config.Item;
import edu.indiana.dlib.catalog.config.NameValuePair;
import edu.indiana.dlib.catalog.config.impl.FedoraConfigurationManager;
import edu.indiana.dlib.catalog.config.impl.FedoraItemManager;
import edu.indiana.dlib.catalog.config.impl.fedora.SerializableItem;
import edu.indiana.dlib.catalog.search.impl.DLPSearchManager;
import edu.indiana.dlib.fedora.client.iudl.DLPFedoraClient;
import edu.indiana.dlib.fedora.client.iudl.PURLLogic;
import edu.indiana.dlib.fedoraindexer.server.FedoraObjectAdministrativeMetadata;
import edu.indiana.dlib.fedoraindexer.server.IndexInitializationException;
import edu.indiana.dlib.fedoraindexer.server.index.converters.CQLStyleLuceneDocumentConverter;

/**
 * A special purpose index that makes several calls to fedora to create 
 * a representation of an Item (for photocat) which is stored in the
 * index, and indexes all the item metadata fields for searching.
 */
public class PhotocatItemIndex extends AtomicObjectLuceneIndex {

    protected FedoraItemManager fim;
    
    protected FedoraConfigurationManager fcm;
    
    public PhotocatItemIndex(Properties config, DLPFedoraClient fc) throws IndexInitializationException {
        super(config);

        // create a FedoraItemManager (which needs more privileges than the fedora client)
        fim = new FedoraItemManager(
                getRequiredProperty(config, "username"), 
                getRequiredProperty(config, "password"), 
                getRequiredProperty(config, "host"), 
                getRequiredProperty(config, "context"), 
                Integer.parseInt(getRequiredProperty(config, "port")), 
                getRequiredProperty(config, "im-content-model"), 
                getRequiredProperty(config, "im-dsid"), 
                getRequiredProperty(config, "im-private-dsid"),
                getRequiredProperty(config, "blockAll"),
                getRequiredProperty(config, "blockHigh"),
                true);
        
        fcm = new FedoraConfigurationManager(
                getRequiredProperty(config, "username"), 
                getRequiredProperty(config, "password"), 
                getRequiredProperty(config, "host"), 
                getRequiredProperty(config, "context"), 
                Integer.parseInt(getRequiredProperty(config, "port")), 
                getRequiredProperty(config, "config-content-model"), 
                getRequiredProperty(config, "config-dsid"), 
                getRequiredProperty(config, "def-content-model"),
                getRequiredProperty(config, "def-dsid"),                
                getRequiredProperty(config, "relationship"),
                getRequiredProperty(config, "def-config-url"),
                "true");
    }
    
    private String getRequiredProperty(Properties config, String propertyName) throws IndexInitializationException {
        if (config.containsKey(propertyName)) {
            return config.getProperty(propertyName);
        } else {
            throw new IndexInitializationException("The required property \"" + propertyName + "\" was not provided!");
        }
    }

    protected Document createIndexDocument(FedoraObjectAdministrativeMetadata adminData) throws Exception {
        Document indexDoc = new Document();
        
        // fetch the collection
        final String collectionId = PURLLogic.getCollectionIdFromDefaultPURL(PURLLogic.getDefaultPURL(adminData.getFullItemId()));
        CollectionConfiguration c = fcm.getCollectionConfiguration(collectionId);
        if (c == null) {
            throw new RuntimeException("No collection configuration found for collection \"" + collectionId + "\"!");
        }
        // fetch the item
        Item item = fim.fetchItem(PURLLogic.getDefaultPURL(adminData.getFullItemId()), c);
        
        // index the item_metadata: all the field parts
        for (String fieldType : item.getMetadata().getRepresentedFieldTypes()) {
            FieldData data = item.getMetadata().getFieldData(fieldType);
            for (NameValuePair attribute : data.getAttributes()) {
                String indexName = DLPSearchManager.translateIndexName(fieldType) + "-attribute-" + DLPSearchManager.translateIndexName(attribute.getName());
                // keyword field
                CQLStyleLuceneDocumentConverter.addKeywordField(indexName, attribute.getValue(), indexDoc);
                // named field
                CQLStyleLuceneDocumentConverter.addKeywordField("keyword", attribute.getValue(), indexDoc);
                // SPECIAL_COMPLETION_FIELD
                CQLStyleLuceneDocumentConverter.addKeywordField("SPECIAL_COMPLETION_FIELD", indexName, indexDoc);
            }
            for (List<NameValuePair> parts : data.getParts()) {
                for (NameValuePair part : parts) {
                    String indexName = DLPSearchManager.translateIndexName(fieldType) + "-part-" + DLPSearchManager.translateIndexName(part.getName());
                    // keyword field
                    CQLStyleLuceneDocumentConverter.addKeywordField(indexName, part.getValue(), indexDoc);
                    // named field
                    CQLStyleLuceneDocumentConverter.addKeywordField("keyword", part.getValue(), indexDoc);
                    // SPECIAL_COMPLETION_FIELD
                    CQLStyleLuceneDocumentConverter.addKeywordField("SPECIAL_COMPLETION_FIELD", indexName, indexDoc);
                    
                    if (fieldType.equals("DATE_TAKEN") && part.getName().equals("year")) {
                        String year = part.getValue();
                        if (year.length() > 1) {
                            // SPECIAL_DECADE_FACET (maybe not even used)
                            CQLStyleLuceneDocumentConverter.addKeywordField("SPECIAL_DECADE_FACET", year.substring(0, year.length() - 1) + "0s", indexDoc);
                        }
                    }
                }
            }
        }
        
        // index the collectionId
        CQLStyleLuceneDocumentConverter.addKeywordField("keyword", item.getMetadata().getCollectionId(), indexDoc);
        CQLStyleLuceneDocumentConverter.addKeywordField("collectionId", item.getMetadata().getCollectionId(), indexDoc);
        
        // index the purl
        CQLStyleLuceneDocumentConverter.addKeywordField("keyword", item.getMetadata().getId(), indexDoc);
        CQLStyleLuceneDocumentConverter.addKeywordField("purl", item.getMetadata().getId(), indexDoc);
        
        // index the itemId
        CQLStyleLuceneDocumentConverter.addKeywordField("keyword", PURLLogic.getItemIdFromDefaultPURL(item.getMetadata().getId()), indexDoc);
        CQLStyleLuceneDocumentConverter.addKeywordField("itemId", PURLLogic.getItemIdFromDefaultPURL(item.getMetadata().getId()), indexDoc);
        
        // index the last modification date
        CQLStyleLuceneDocumentConverter.addKeywordField("modificationDate", adminData.getLastModificationDate(), indexDoc);
        
        // index the full document 
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SerializableItem i = new SerializableItem(item);
        DocumentHelper.writeOutDocument(baos, i.toDocument());
        CQLStyleLuceneDocumentConverter.addRecordField("i", new String(baos.toByteArray(), "UTF-8"), indexDoc);
        
        return indexDoc;
    }


}
