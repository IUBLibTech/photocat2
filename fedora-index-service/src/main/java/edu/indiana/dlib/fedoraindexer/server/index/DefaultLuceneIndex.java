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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.NIOFSDirectory;

import edu.indiana.dlib.fedoraindexer.server.FedoraObjectAdministrativeMetadata;
import edu.indiana.dlib.fedoraindexer.server.Index;
import edu.indiana.dlib.fedoraindexer.server.IndexInitializationException;
import edu.indiana.dlib.fedoraindexer.server.IndexOperationException;

/**
 * <p>
 *   An incomplete base {@link Index} implementation that allows for 
 *   easy subclassing to create custom {@link Index} objects that 
 *   deal with a Lucene index.
 * </p>
 * <p>
 *   This class fully implements all operations dealing with the index and
 *   provides a handy method for mapping the names of the basic 
 *   administrative data fields to the names to be included in the index.
 * </p>
 * <p>
 *   This class is thread-safe with regards to the index store (Lucene).
 *   This is accomplished by synchronizing all methods that access the
 *   underlying Lucene index.  Callers should be aware of this behavior
 *   to avoid setting up deadlocks.
 * <p>
 * <p>
 *   The configuration of this class is provided at construction time by
 *   means of a Properties file.  Along with those defined for {@link 
 *   AbstractIndex}, the follow properties are recognized by default, 
 *   though subclasses may add to this list.
 *   <ul>
 *     <li>luceneIndexDirectory - required</li>
 *     <li>
 *       unlockTime - an optional value that indicates the number 
 *       of milliseconds without modification an index directory
 *       may have before being forcefully unlocked by Index.  If
 *       unspecified, a time of 3600000 (1 hour) is used because 
 *       it is believed that no operation would need to lock an
 *       index for an hour without actually touching any of its
 *       files.
 *     </li>
 *     <li>
 *       analyzerClass - optional, the full name of a Lucene Analyzer 
 *       to be used when indexing objects.  If unspecified, this 
 *       defaults to the {@link 
 *       org.apache.lucene.analysis.standard.StandardAnalyzer}.
 *     </li>
 *     <li>
 *       fieldMappingPropertiesFile - (optional) the name of a 
 *       properties file whose key-value pairs represent a mapping from
 *       the basic administrative data field names to the names they 
 *       should use in the index.
 *     </li>
 *     <li>
 *       PID.filterExpression, FULL_ITEM_ID.filterExpression, TITLE.filterExpression,
 *       COLLECTION_ID.filterExpression, CONTENT_MODEL.filterExpression, 
 *       CREATION_DATE.filterExpression and LAST_MODIFICATION_DATE.filterExpression: 
 *       (optional) java regular expressions representing required values for the
 *       basic administrative field (listed in the prefix) in order for objects to
 *       be included in this index.  If no filterExpressions are supplied, no
 *       filtering will occur and all objects will be included.  Otherwise, each 
 *       expression will be evaluated against an incoming object and used to further
 *       restrict the objects that may be included in this index. 
 *     </li>
 *   </ul>
 * </p>
 */
public abstract class DefaultLuceneIndex extends AbstractIndex {

    protected String indexFilename;
    
    protected IndexWriter writer;
    
    protected Analyzer analyzer;
    
    private long unlockTime;
    
    protected Properties fieldMapping;

    public DefaultLuceneIndex(Properties config) throws IndexInitializationException {
        super(config);
        
        // set up analyzer
        String analyzerClassName = config.getProperty("analyzerClass");
        if (analyzerClassName == null) {
            this.analyzer = new StandardAnalyzer();
        } else {
            try {
                this.analyzer = (Analyzer) Class.forName(config.getProperty("analyzerClass")).newInstance();
            } catch (InstantiationException ex) {
                this.analyzer = new StandardAnalyzer();
                LOGGER.warn("Unable to create an instance of \"" + analyzerClassName + "\"!" , ex);
            } catch (IllegalAccessException ex) {
                this.analyzer = new StandardAnalyzer();
                LOGGER.warn("Unable to create an instance of \"" + analyzerClassName + "\", illegal access!" , ex);
            } catch (ClassNotFoundException ex) {
                this.analyzer = new StandardAnalyzer();
                LOGGER.warn("Unable to create an instance of \"" + analyzerClassName + "\", missing class!" , ex);
            }
        }
        
        // Set up index writer
        this.indexFilename = config.getProperty("luceneIndexDirectory");
        if (this.indexFilename == null) {
            throw new IndexInitializationException("'luceneIndexDirectory' was not specified in the configuration for index '" + this.getIndexName() + "'!");
        }
        
        // set up mapping
        this.fieldMapping = new Properties();
        String fieldMappingFilename = config.getProperty("fieldMappingPropertiesFile");
        if (fieldMappingFilename != null) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(fieldMappingFilename);
                this.fieldMapping.load(fis);
            } catch (FileNotFoundException ex) {
                throw new IndexInitializationException("Unable to open field mapping, \"" + fieldMappingFilename + "\": file not found!", ex);
            } catch (IOException ex) {
                throw new IndexInitializationException("Unable to read field mapping, \"" + fieldMappingFilename + "\"!", ex);
            } finally {
                try {
                    if (fis != null) {
                        fis.close();
                    }
                } catch (IOException ex) {
                    throw new IndexInitializationException("Unable to close file \"" + fieldMappingFilename + "\"!", ex);
                }
            }
        }
        
        // set the unlock time
        this.unlockTime = 3600000;
        String unlockTimeStr = config.getProperty("unlockTime");
        if (unlockTimeStr != null) {
            try {
                this.unlockTime = Long.parseLong(unlockTimeStr);
            } catch (Throwable t) {
                LOGGER.warn("Invalid unlock time \"" + unlockTimeStr + "\".", t);
            }
        }
    }

    public synchronized void open() throws IndexOperationException {
        File indexDirFile = new File(this.indexFilename);
        if (!indexDirFile.exists()) {
            indexDirFile.mkdir();
        }
        try {
            Directory indexDir = new NIOFSDirectory(indexDirFile);
            if (IndexWriter.isLocked(indexDir)) {
                long lastModification = IndexReader.lastModified(indexDir);
                if (lastModification != 0) {
                    long duration = System.currentTimeMillis() - lastModification;
                    LOGGER.warn("The index: \"" + indexDir + "\" has been locked but not modified for " + duration + "ms.");
                    if (duration > this.unlockTime) {
                        IndexWriter.unlock(indexDir);
                        LOGGER.warn("The index \"" + indexDir + "\" was forcefully unlocked!");
                    }
                }
            }
            this.writer = new IndexWriter(indexDir, this.analyzer, IndexWriter.MaxFieldLength.UNLIMITED);
            //this.writer = new IndexWriter(indexDir, this.analyzer);
        } catch (CorruptIndexException ex) {
            throw new IndexOperationException("Unable to open index, \"" + indexDirFile.getAbsolutePath() + "\": corrupt!", ex);
        } catch (LockObtainFailedException ex) {
            throw new IndexOperationException("Unable to open index, \"" + indexDirFile.getAbsolutePath() + "\": locked!", ex);
        } catch (IOException ex) {
            throw new IndexOperationException("Unable to open index, \"" + indexDirFile.getAbsolutePath() + "\"!", ex);
        }
    }
    
    public synchronized void close() throws IndexOperationException {
        if (this.writer != null) {
            try {
                this.writer.close();
            } catch (CorruptIndexException ex) {
                throw new IndexOperationException(this.getIndexName() + ": Unable to close index because it is corrupt!", ex);
            } catch (IOException ex) {
                throw new IndexOperationException(this.getIndexName() + ": Unable to close index!", ex);
            }
        } 
        this.writer = null;
    }

    public synchronized void optimize() throws IndexOperationException {
        boolean opened = false;
        if (this.writer == null) {
            this.open();
            opened = true;
        }
        try {
            LOGGER.info(this.getIndexName() + ": preparing to optimize index");
            writer.optimize();
            LOGGER.info(this.getIndexName() + ": index optimization complete");
        } catch (CorruptIndexException ex) {
            throw new IndexOperationException(this.getIndexName() + ": Unable to optimize index because it is corrupt!", ex);
        } catch (IOException ex) {
            throw new IndexOperationException(this.getIndexName() + ": Unable to optimize index!", ex);
        }
        if (opened) {
            this.close();
        }
    }

    protected String getLuceneFieldName(FedoraObjectAdministrativeMetadata.Field field) {
        if (this.fieldMapping != null && this.fieldMapping.containsKey(field.name())) {
            return this.fieldMapping.getProperty(field.name());
        } else {
            return field.name(); 
        }
    }
}
