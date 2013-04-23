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

import java.util.Collection;

import edu.indiana.dlib.catalog.config.impl.FedoraDjatokaDataView;

/**
 * A grouping of DataView objects that represents
 * one logical aspect of the item.  This may be various sized 
 * images from a particular master image.
 * 
 * Common practice is to use the ID from the object in the
 * repository that contains the various derivative images
 * for the aspect id.  Since Aspect objects are created by
 * the ItemManager implementation, they can use whatever
 * semantics are most useful for that implementation.  
 * @see FileSubmitter
 */
public interface Aspect {

    public DataView getThumbnailView();
    
    public DataView getScreenView();
    
    public DataView getLargeView();
    
    /**
     * Returns a view that represents a landing page that displays
     * a zooming and panning view of the object.
     */
    public FedoraDjatokaDataView getScalableView();
    
    public Collection<DataView> listDataViews();
    
    /**
     * This may be the same as an ItemMetadata id, in which case it
     * can be assumed that the images are stored with, rather than
     * linked to the item.  
     */
    public String getId();
    
}
