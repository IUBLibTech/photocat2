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
package edu.indiana.dlib.catalog.search.impl.click.control;

import org.apache.click.control.ActionLink;
import org.apache.click.control.Column;
import org.apache.click.control.Table;
import org.apache.click.dataprovider.DataProvider;

import edu.indiana.dlib.catalog.fields.click.control.RelativeActionLink;
import edu.indiana.dlib.catalog.search.BrowseResults;

public class BrowseTable extends Table {
    
	public static final String PAGE_SIZE = "pageSize";
	
    public BrowseTable(String name, final BrowseResults results) {
        super(name);
        try {
        	String pageSizeStr = getContext().getRequestParameter(PAGE_SIZE);
        	setPageSize(Integer.parseInt(pageSizeStr));
        } catch (Throwable t) {
        	setPageSize(25);
        }
        Column termNameColumn = new Column("fieldValue", getMessage("label-value"));
        termNameColumn.setSortable(true);
        this.addColumn(termNameColumn);
        Column countColumn = new Column("hitCount", getMessage("label-count"));
        countColumn.setSortable(true);
        this.addColumn(countColumn);
        
        this.setDataProvider(new DataProvider() {

            public Iterable getData() {
                return results.listBrowseResults();
            }});
        this.setClass(CLASS_BLUE1);
    }
    
    public ActionLink getControlLink() {
        if (controlLink == null) {
            controlLink = new RelativeActionLink();
        }
        return controlLink;
    }
    
}
