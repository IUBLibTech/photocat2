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
package edu.indiana.dlib.catalog.config.impl;

import java.util.Collection;

import edu.indiana.dlib.catalog.config.DataView;
import edu.indiana.dlib.catalog.config.Aspect;

public class DefaultAspect implements Aspect {

    private String id;
    
    private DataView thumbnail;
    
    private DataView screen;
    
    private DataView large;
    
    private FedoraDjatokaDataView scalable;
    
    private Collection<DataView> dataViews;
    
    public DefaultAspect(String id, Collection<DataView> views, DataView thumbnail, DataView screen, DataView large, FedoraDjatokaDataView scalable) {
        this.id = id;
        this.thumbnail = thumbnail;
        this.dataViews = views;
        this.screen = screen;
        this.large = large;
        this.scalable = scalable;
    }
    
    public String getId() {
        return id;
    }
    
    public DataView getThumbnailView() {
        return thumbnail;
    }

    public Collection<DataView> listDataViews() {
        return dataViews;
    }

    public DataView getScreenView() {
        return screen;
    }

    public DataView getLargeView() {
        return large;
    }
    
    public FedoraDjatokaDataView getScalableView() {
        return scalable;
    }

}
