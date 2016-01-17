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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface HistoryEnabledItemManager extends ItemManager {

    /**
     * Gets the history of the item metadata in order from most 
     * recent to oldest.
     * @param id the identifier for the item whose history is
     * being requested
     * @return a reverse chronologically ordered list of VersionInformation
     * objects representing all known versions of the item metadata
     */
    public List<VersionInformation> getItemMetadataHistory(String id) throws RepositoryException;
    
    /**
     * Gets the item metadata as it appeared on the given date.
     */
    public ItemMetadata getHistoricItemMetdata(String id, Date date) throws RepositoryException;
    
    /**
     * A basic class to encapsulate information about a version of the
     * item metadata.  Each instance of this object represents a discrete
     * save event for an item's metadata.
     */
    public static class VersionInformation {

        private Date date;
        
        private String id;
        
        private List<String> extendedPropertyNames;
        
        private Map<String, String> extendedPropertyValues;
        
        public VersionInformation(String id, Date date) {
            this.id = id;
            this.date = date;
            this.extendedPropertyNames = new ArrayList<String>();
            this.extendedPropertyValues = new HashMap<String, String>();
        }
        
        public void addProperty(String name, String value) {
            this.extendedPropertyNames.add(name);
            this.extendedPropertyValues.put(name, value);
        }
        
        public String getId() {
            return id;
        }
        
        public Date getDate() {
            return date;
        }
        
        public List<String> listExtendedVersionPropertyNamess() {
            return this.extendedPropertyNames;
        }
        
        public String getExtendedPropertyValue(String name) {
            return this.extendedPropertyValues.get(name);
        }
    }
}
