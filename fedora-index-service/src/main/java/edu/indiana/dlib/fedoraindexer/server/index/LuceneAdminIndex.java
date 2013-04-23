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

import java.util.Properties;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import edu.indiana.dlib.fedoraindexer.server.FedoraObjectAdministrativeMetadata;
import edu.indiana.dlib.fedoraindexer.server.IndexInitializationException;

/**
 * An extension of {@link DefaultLuceneIndex} that indexes just the
 * administrative data, and requests to do so immediately rather than
 * asynchronously.
 */
public class LuceneAdminIndex extends AtomicObjectLuceneIndex {

    public LuceneAdminIndex(Properties config) throws IndexInitializationException {
        super(config);
    }

    protected Document createIndexDocument(FedoraObjectAdministrativeMetadata adminData) {
        Document d = new Document();
        for (FedoraObjectAdministrativeMetadata.Field field : FedoraObjectAdministrativeMetadata.Field.values()) {
            String fieldName = getLuceneFieldName(field);
            for (String fieldValue : adminData.getFieldValues(field)) {
                if (fieldName == null || fieldValue == null) {
                    LOGGER.warn(this.getIndexName() + ": unable to index field (" + fieldName + "=" + fieldValue + ")");
                } else {
                    d.add(new Field(getLuceneFieldName(field), fieldValue, Field.Store.YES, Field.Index.UN_TOKENIZED));
                }
            }
        }
        return d;
    }

}
