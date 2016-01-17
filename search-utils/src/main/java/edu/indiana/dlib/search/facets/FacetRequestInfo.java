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
package edu.indiana.dlib.search.facets;

import java.util.List;

/**
 * An encapsulation of all of the variables for a single facet
 * request.  An SRU/W request may contain a series of these 
 * requests. 
 */
public class FacetRequestInfo {
    
    public static int UNLIMITED = -1;
    
    public String facetFieldName;
    public int facetRequestCount;
    public int facetOffset;
    
    public String originalRequest;
    
    public FacetRequestInfo(String name) {
        this.facetFieldName = name;
        this.facetRequestCount = -1;
        this.facetOffset = -1;
        this.originalRequest = this.facetFieldName;
    }
    
    public FacetRequestInfo(String name, int count) {
        this.facetFieldName = name;
        this.facetRequestCount = count;
        this.facetOffset = -1;
        this.originalRequest = this.facetFieldName + "," + this.facetRequestCount;
    }
    
    public FacetRequestInfo(String name, int count, int offset) {
        this.facetFieldName = name;
        this.facetRequestCount = count;
        this.facetOffset = offset;
        this.originalRequest = this.facetFieldName + "," + this.facetRequestCount + "," + this.facetOffset;
    }
    
    public FacetRequestInfo(String name, int count, int offset, String originalRequest) {
        this.facetFieldName = name;
        this.facetRequestCount = count;
        this.facetOffset = offset;
        this.originalRequest = originalRequest;
    }
    
    public static String getEncodedString(List<FacetRequestInfo> fris) {
        StringBuffer sb = new StringBuffer();
        for (FacetRequestInfo fri : fris) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(fri.facetFieldName + "," 
                    + (fri.facetRequestCount == UNLIMITED ? "" : fri.facetRequestCount) + ","
                    + (fri.facetOffset == UNLIMITED ? "" : fri.facetOffset) + ",");
        }
        return sb.toString();
    }

    
}