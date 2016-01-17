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

import java.util.Collection;
import java.util.Collections;

/**
 * The basic metadata that must be indexed for every object
 * in the repository in order for minimal functionality of 
 * DLP systems.  Specifically this information is needed to
 * map collection level item identifiers to fedora pids, which
 * is needed to resolve purls (Pursistent URLs).
 */
public class FedoraObjectAdministrativeMetadata {

    public static enum Field {
        PID,
        FULL_ITEM_ID,
        TITLE,
        COLLECTION_ID,
        CONTENT_MODEL,
        CREATION_DATE,
        LAST_MODIFICATION_DATE;
    }
    
    private String pid;
    
    private String fullItemId;
    
    private String title;
    
    private String collectionId;
    
    private Collection<String> contentModels;
    
    private String creationDate;
    
    private String lastModificationDate;

    public FedoraObjectAdministrativeMetadata(String pid, String fullItemId, String title, String collectionId, Collection<String> contentModels, String creationDate, String lastModificationDate) {
        this.pid = pid;
        this.fullItemId = fullItemId;
        this.title = title;
        this.collectionId = collectionId;
        this.contentModels = contentModels;
        this.creationDate = creationDate;
        this.lastModificationDate = lastModificationDate;
    }
    
    /**
     * Returns a collection containing all of the 
     * current values for a given field.  Most fields
     * (all but the content model) are guaranteed to
     * return a collection with exactly one element,
     * but to treat all fields consistently and to
     * guarantee a type, a collection of strings is
     * returned by this method.
     */
    public Collection<String> getFieldValues(Field field) {
        switch (field) {
            case PID:
                return Collections.singleton(this.pid);
            case FULL_ITEM_ID:
                return Collections.singleton(this.fullItemId);
            case TITLE:
                return Collections.singleton(this.title);
            case COLLECTION_ID:
                return Collections.singleton(this.collectionId);
            case CONTENT_MODEL:
                return this.contentModels;
            case CREATION_DATE:
                return Collections.singleton(this.creationDate);
            case LAST_MODIFICATION_DATE:
                return Collections.singleton(this.lastModificationDate);
            default:
                throw new IllegalArgumentException();
        }
    }
    
    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getPid() {
        return pid;
    }

    public void setFullItemId(String fullItemId) {
        this.fullItemId = fullItemId;
    }

    public String getFullItemId() {
        return fullItemId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setCollectionId(String collectionId) {
        this.collectionId = collectionId;
    }

    public String getCollectionId() {
        return collectionId;
    }

    public void setContentModel(Collection<String> contentModels) {
        this.contentModels = contentModels;
    }

    public Collection<String> getContentModel() {
        return contentModels;
    }
    
    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setLastModifiedDate(String lastModificationDate) {
        this.lastModificationDate = lastModificationDate;
    }

    public String getLastModificationDate() {
        return lastModificationDate;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("[" + this.pid + " - " + this.fullItemId);
        if (this.contentModels != null) {
            for (String contentModel : this.contentModels) {
                sb.append(", " + contentModel);
            }
        }
        sb.append("]");
        return sb.toString();
    }
}