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
package edu.indiana.dlib.catalog.search.impl;

import java.util.ArrayList;
import java.util.List;

import edu.indiana.dlib.catalog.search.BrowseResult;

public class DefaultBrowseResult implements BrowseResult {

    /**
     * TODO: fieldValue and displayLabel are redundant.  Display label should be used for display and
     * the value was only useful if there wasn't a query.
     */
    private String fieldValue;
    
    private String displayLabel;
    
    private int hitCount; 
    
    private String query;
    
    private List<BrowseResult> subResults;
    
    public DefaultBrowseResult(String fieldValue, int hitCount, String query) {
        this(fieldValue, fieldValue, hitCount, query, null);
    }
    
    public DefaultBrowseResult(String fieldValue, String displayLabel, int hitCount, String query) {
        this(fieldValue, displayLabel, hitCount, query, null);
    }
    
    public DefaultBrowseResult(String fieldValue, String displayLabel, int hitCount, String query, List<BrowseResult> subResults) {
        this.fieldValue = fieldValue;
        this.displayLabel = displayLabel;
        this.hitCount = hitCount;
        this.query = query;
        this.subResults = subResults;
    }
    
    public String getFieldValue() {
        return fieldValue;
    }
    
    public int getHitCount() {
        return hitCount;
    }

    public String getQuery() {
        return this.query;
    }

    public String getFieldDisplayLabel() {
        return displayLabel;
    }

    public List<BrowseResult> listBrowseResults() {
        return subResults;
    }
    
    public void addBrowseResult(BrowseResult result) {
        if (subResults == null) {
            subResults = new ArrayList<BrowseResult>();
        }
        subResults.add(result);
        hitCount += result.getHitCount();
    }
    
}
