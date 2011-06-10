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
package edu.indiana.dlib.catalog.batch;

import java.io.IOException;
import java.util.List;

/**
 * An interface that when implemented supports the creation and 
 * management of Batches within the application.  This interface
 * supports batches that are tied to a single user and collection
 * that persist across web sessions.
 * 
 * TODO: this should be scoped better with methods that don't
 *       require userId and collectionId.
 */
public interface BatchManager {

    public List<Batch> listAllBatches(String userId, String collectionId) throws IOException;

    public Batch createNewBatch(String userId, String collectionId, String name, List<String> ids) throws IOException;
    
    public void deleteBatch(String userId, String collectionId, int batchId) throws IOException;
    
    public void saveBatch(String userId, String collectionId, Batch batch) throws IOException;
    
    public Batch openBatch(String userId, String collectionId, int batchId) throws IOException;
    
    public void closeBatch(String userId, String collectionId, int batchId);
    
    public List<Batch> listOpenBatches(String userId, String collectionId);
    
    public boolean isBatchOpen(String userId, String collectionId, int batchId);
    
}
