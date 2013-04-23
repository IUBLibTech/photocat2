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
package edu.indiana.dlib.fedoraindexer.server;

/**
 * An {@code Index} implementation encapsulates a search engine's
 * index for fedora objects.
 */
public interface Index {
    
    public static enum Operation {
        ADD,
        REMOVE,
        UPDATE;
    }
    
    /**
     * Gets the name of this index.  This name is not guaranteed
     * to be unique, nor meaningful, but when appropriately used
     * can serve to differentiate index's in output or logging.
     */
    public String getIndexName();

    /**
     * <p>
     *   Updates the index regarding the given object.  Depending on
     *   the operation, this may remove all information about the
     *   given object, update existing information or add information
     *   about this object which was until now unknown.
     * </p>
     * <p>
     *   If {@link #open()} has been invoked since the last invocation
     *   of {@link #close()}, this operation will be spared the overhead
     *   of opening access to the underlying index.  Otherwise, this 
     *   operation will open and then close write access to the 
     *   underlying index.
     * </p>
     * @param op ADD, REMOVE or UPDATE to indicate how the index
     * should be modified.  In most implementations, UPDATE behaves
     * just like ADD in cases where no information was previously
     * known.
     * @param objectAdminInfo The basic information about an object
     * stored in the fedora repository.
     * @throws IndexOperationException
     */
    public void indexObject(Operation op, FedoraObjectAdministrativeMetadata objectAdminInfo) throws IndexOperationException;    

    /**
     * <p>
     *   A method that allows {@code Index} implementations to specify
     *   which objects they index.  This method should return true if 
     *   the indicated object is meant to be included in this index or
     *   false if it is not.  In the case where it cannot be determined
     *   with just the basic administrative information, this method 
     *   should return true.  In all cases, this method should not 
     *   perform any time-consuming calculations or request remote
     *   information but instead prioritize processing time over 
     *   accuracy.
     * </p>
     * <p>
     *   Invoking this method before {@link #indexObject(
     *   edu.indiana.dlib.fedoraindexer.server.Index.Operation, 
     *   FedoraObjectAdministrativeMetadata)} is merely a courtesy 
     *   and may improve performance but should not be expected for
     *   proper behavior by implementors.
     * </p>
     * @param objectAdmininInfo
     * @return
     */
    public boolean shouldIndexObject(FedoraObjectAdministrativeMetadata adminInfo);
    
    /**
     * Explicitly opens write access to the underlying index (whether it
     * be a file system implementation or a database or whatever).  This
     * will remain open until {@link #close()} is called.  This is not
     * required before invoking {@code #indexObject()} but instead can be
     * used to batch updates to improve performance.  Callers must invoke
     * {@link #close()} before terminating the application or risk retaining
     * a write lock on the underlying index.
     */
    public void open() throws IndexOperationException;
    
    /**
     * Explicitly closes write access to the underlying index.
     * {@see #open()}
     */
    public void close() throws IndexOperationException;
    
    /**
     * Optimizes the underlying index for searching.  Implementations of this
     * method can be expected to be taxing and should not be called when
     * high volumes of searching or any amount of updating is expected for
     * the underlying search index.
     */
    public void optimize() throws IndexOperationException;
    
}
