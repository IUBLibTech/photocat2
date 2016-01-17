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

import java.io.IOException;
import java.util.Properties;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.Term;

import edu.indiana.dlib.fedoraindexer.server.FedoraObjectAdministrativeMetadata;
import edu.indiana.dlib.fedoraindexer.server.IndexInitializationException;
import edu.indiana.dlib.fedoraindexer.server.IndexOperationException;

/**
 * <p>
 *   This extension of {@link DefaultLuceneIndex} is meant to index
 *   objects that are atomic with regards to search results.  If 
 *   a single object represents multiple discoverable parts (for example
 *   a book that is addressable by page) the {@link CompoundObjectLuceneIndex}
 *   is a more suitable base class.
 * </p>
 * 
 * {@inheritDoc}
 */
public abstract class AtomicObjectLuceneIndex extends DefaultLuceneIndex {

    public AtomicObjectLuceneIndex(Properties config) throws IndexInitializationException {
        super(config);
    }

    /**
     * Updates the index to reflect the most current version of the object
     * represented by the "adminData".  
     */
    public void indexObject(Operation op, FedoraObjectAdministrativeMetadata adminData) throws IndexOperationException {
        // Perform the potentially time-consuming Document
        // generation before opening the index writer or 
        // taking the monitor for this object.
        Document doc = null;
        if (!op.equals(Operation.REMOVE)) {
            try {
                doc = this.createIndexDocument(adminData);
            } catch (Throwable t) {
                LOGGER.error(t);
                throw new IndexOperationException(this.getIndexName() + ": Unable to generate index document!", t);
            }
        }
        
        synchronized (this) {
            boolean opened = false;
            if (this.writer == null) {
                this.open();
                opened = true;
            }
            try {
                if (doc != null && doc.getFields().isEmpty()) {
                    LOGGER.info(this.getIndexName() + " no fields to index, demoting " + op + " operation to a " + Operation.REMOVE + " operation.");
                    op = Operation.REMOVE;
                } else if (doc != null) {
                    // Check for the presence of a PID field and
                    // add one if it's missing.
                    if (doc.getField(getLuceneFieldName(FedoraObjectAdministrativeMetadata.Field.PID)) == null) {
                        doc.add(new Field(getLuceneFieldName(FedoraObjectAdministrativeMetadata.Field.PID), adminData.getPid(), Field.Store.YES, Field.Index.UN_TOKENIZED));
                        LOGGER.debug("Added a unique identifier (pid) field to the index document created by " + this.getIndexName() + ".");
                    }
                }
                if (op.equals(Operation.ADD)) {
                    writer.updateDocument(createTermToFindObjectsWithPid(adminData), doc);
                    LOGGER.info(adminData.getPid() + " (" + adminData.getFullItemId() + ") ADDED to \"" + this.getIndexName() + "\"");
                } else if (op.equals(Operation.REMOVE)) {
                    writer.deleteDocuments(createTermToFindObjectsWithPid(adminData));
                    LOGGER.info(adminData.getPid() + " (" + adminData.getFullItemId() + ") REMOVED from \"" + this.getIndexName() + "\"");
                } else if (op.equals(Operation.UPDATE)) {
                    if (doc == null) {
                        LOGGER.warn("  " + adminData.getPid() + " (" + adminData.getFullItemId() + ") Update will act as a removal from \"" + this.getIndexName() + "\" because no index document was generated!");
                    }
                    writer.updateDocument(createTermToFindObjectsWithPid(adminData), doc);
                    LOGGER.info(adminData.getPid() + " (" + adminData.getFullItemId() + ") UPDATED in \"" + this.getIndexName() + "\"");
                }
            } catch (CorruptIndexException ex) {
                LOGGER.error(ex);
                throw new IndexOperationException(this.getIndexName() + ": Unable to update index because it is corrupt!", ex);
            } catch (IOException ex) {
                LOGGER.error(ex);
                throw new IndexOperationException(this.getIndexName() + ": Unable to update index!", ex);
            } catch (Throwable t) {
                LOGGER.error(t);
                throw new IndexOperationException(this.getIndexName() + ": Unable to generated index document!", t);
            } finally {
                if (opened) {
                    this.close();
                }
            }
        }
    }

    protected Term createTermToFindObjectsWithPid(FedoraObjectAdministrativeMetadata adminData) {
        return new Term(getLuceneFieldName(FedoraObjectAdministrativeMetadata.Field.PID), adminData.getPid());
    }

    protected abstract Document createIndexDocument(FedoraObjectAdministrativeMetadata adminData) throws Exception;

}
