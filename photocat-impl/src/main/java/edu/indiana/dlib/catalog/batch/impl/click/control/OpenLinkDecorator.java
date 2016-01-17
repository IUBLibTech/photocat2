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
package edu.indiana.dlib.catalog.batch.impl.click.control;

import org.apache.click.Context;
import org.apache.click.control.AbstractLink;
import org.apache.click.control.Table;
import org.apache.click.extras.control.LinkDecorator;

import edu.indiana.dlib.catalog.batch.Batch;
import edu.indiana.dlib.catalog.batch.BatchManager;

/**
 * A special-purpose decorator that updates the link it's
 * decorating to render different text for the link depending
 * on the status of the batch (open or not).
 */
public class OpenLinkDecorator extends LinkDecorator {

    private BatchManager bm;
    
    private String openText;
    
    private String closeText;
    
    public OpenLinkDecorator(Table table, AbstractLink link, String idProperty, BatchManager bm, String openText, String closeText) {
        super(table, link, idProperty);
        this.bm = bm;
        this.openText = openText;
        this.closeText = closeText;
    }
    
    public String render(Object row, Context context) {
        Batch batch = (Batch) row;
        if (bm.isBatchOpen(batch.getUserId(), batch.getCollectionId(), batch.getId())) {
            linksArray[0].setLabel(closeText);
        } else {
            linksArray[0].setLabel(openText);
        }
        return super.render(row, context);
    }

}
