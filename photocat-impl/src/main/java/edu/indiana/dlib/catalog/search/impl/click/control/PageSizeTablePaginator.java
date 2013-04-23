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
import org.apache.click.control.Table;
import org.apache.click.control.TablePaginator;
import org.apache.click.util.HtmlStringBuffer;

public class PageSizeTablePaginator extends TablePaginator {

    public PageSizeTablePaginator(BrowseTable table) {
		super(table);
	}

    public void render(HtmlStringBuffer buffer) {
        final Table table = getTable();
        final ActionLink controlLink = table.getControlLink();
        if (table.getSortedColumn() != null) {
            controlLink.setParameter(Table.SORT, null);
            controlLink.setParameter(Table.COLUMN, table.getSortedColumn());
            controlLink.setParameter(Table.ASCENDING, String.valueOf(table.isSortedAscending()));
        } else {
            controlLink.setParameter(Table.SORT, null);
            controlLink.setParameter(Table.COLUMN, null);
            controlLink.setParameter(Table.ASCENDING, null);
        }
        HtmlStringBuffer pageSizeBuffer = new HtmlStringBuffer();
        for (int size : new int[] { 25, 50, 100 }) {
        	if (pageSizeBuffer.length() > 0) {
        		pageSizeBuffer.append(" | ");
        	}
        	controlLink.setLabel(String.valueOf(size));
        	controlLink.setParameter(Table.PAGE, "0");
        	controlLink.setParameter(BrowseTable.PAGE_SIZE, String.valueOf(size));
        	controlLink.setTitle(String.valueOf(size));
        	pageSizeBuffer.append(controlLink.toString());
        }
        String pageSizeLinks = pageSizeBuffer.toString();
        buffer.append(table.getMessage("table-page-size-links", pageSizeLinks));

        controlLink.setParameter(BrowseTable.PAGE_SIZE, String.valueOf(table.getPageSize()));
        super.render(buffer);
    }

	
}
