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

import java.util.List;

import org.apache.click.control.ActionLink;
import org.apache.click.control.Column;
import org.apache.click.control.Table;
import org.apache.click.dataprovider.DataProvider;

import edu.indiana.dlib.catalog.batch.Batch;
import edu.indiana.dlib.catalog.fields.click.control.RelativeActionLink;

/**
 * A very basic extension of the Table class to represent
 * a table of Batch objects.  The columns added by default
 * expose the "id", "name" and "size".
 */
public class BatchTable extends Table {
    
    private List<Batch> batches;
    
    public BatchTable(String name, final List<Batch> batches) {
        super(name);
        this.batches = batches;
        setClass(CLASS_BLUE1);
        setPageSize(batches.size());
        setPaginatorAttachment(PAGINATOR_DETACHED);
        setSortedColumn("id");
        
        Column idColumn = new Column("id", getMessage("label-batch-id"));
        idColumn.setSortable(true);
        addColumn(idColumn);
        
        Column nameColumn = new Column("name", getMessage("label-batch-name"));
        nameColumn.setSortable(true);
        addColumn(nameColumn);
        
        Column sizeColumn = new Column("size", getMessage("label-batch-size"));
        sizeColumn.setSortable(true);
        addColumn(sizeColumn);
        
        setDataProvider(new DataProvider() {

            public Iterable getData() {
                return batches;
            }});
    }
    
    /**
     * Overrides the default implementation to return a 
     * RelativeActionLink() as required when there's forwarding
     * going on.
     */
    public ActionLink getControlLink() {
        if (controlLink == null) {
            controlLink = new RelativeActionLink();
        }
        return controlLink;
    }
    
}
