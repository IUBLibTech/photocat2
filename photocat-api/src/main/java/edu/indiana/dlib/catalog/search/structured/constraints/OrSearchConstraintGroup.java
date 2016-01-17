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
package edu.indiana.dlib.catalog.search.structured.constraints;

import java.util.List;

import edu.indiana.dlib.catalog.search.structured.AbstractSearchConstraint;
import edu.indiana.dlib.catalog.search.structured.SearchConstraint;

public class OrSearchConstraintGroup extends SerializableSearchConstraint {

    private List<SerializableSearchConstraint> constraints;
    
    private String displayName;
    
    private boolean implicit;
    
    public OrSearchConstraintGroup(List<SerializableSearchConstraint> constraints) {
        this.constraints = constraints;
        implicit = true;
    }
    
    public OrSearchConstraintGroup(String displayName, List<SerializableSearchConstraint> constraints, boolean implicit) {
        this.displayName = displayName;
        this.constraints = constraints;
        this.implicit = implicit;
    }
    
    public List<SerializableSearchConstraint> getOredConstraints() {
        return constraints;
    }
    
    public String getDisplay() {
        return displayName;
    }
    
    public boolean isImplicit() {
        return implicit;
    }
    
    public boolean equals(Object o) {
        if (o instanceof OrSearchConstraintGroup) {
            OrSearchConstraintGroup other = (OrSearchConstraintGroup) o;
            if (!(other.constraints.containsAll(constraints) && other.constraints.size() == constraints.size())) {
                return false;
            }
            
            return equal(displayName, other.displayName) && implicit == other.implicit;
        } else {
            return false;
        }
    }
    
    public int hashCode() {
        int hashCode = 0;
        for (SearchConstraint c : constraints) {
            hashCode += c.hashCode();
        }
        hashCode += displayName.hashCode();
        if (implicit) {
            hashCode = 0 - hashCode;
        }
        return hashCode;
    }

}
