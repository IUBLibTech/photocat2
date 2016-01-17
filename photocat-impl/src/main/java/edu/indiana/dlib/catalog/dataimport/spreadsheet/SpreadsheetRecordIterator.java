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
package edu.indiana.dlib.catalog.dataimport.spreadsheet;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import edu.indiana.dlib.catalog.dataimport.Metadata;
import edu.indiana.dlib.catalog.dataimport.Record;

public class SpreadsheetRecordIterator implements Iterator<Record> {

    private Metadata metadata; 
    
    private Iterator<Row> rows;
    
    public SpreadsheetRecordIterator(Workbook wb) throws InvalidFormatException, FileNotFoundException, IOException {
        this(wb, 0, 0);
    }
    
    public SpreadsheetRecordIterator(Workbook wb, int sheetIndex, int metadataRowIndex) throws InvalidFormatException, FileNotFoundException, IOException {
        Sheet sheet = wb.getSheetAt(sheetIndex);
        rows = sheet.iterator();
        
        // read the metadata (and advance past those rows)
        int index = 0;
        while (rows.hasNext() && index <= metadataRowIndex) {
            if (index == metadataRowIndex) {
                ArrayList<String> fieldNames = new ArrayList<String>();
                for (Cell cell : rows.next()) {
                    fieldNames.add(cell.getStringCellValue());
                }
                metadata = new Metadata(fieldNames.toArray(new String[0]));
            } else {
                rows.next();
            }
            index ++;
        }
    }
    
    public Metadata getMetdadata() {
        return metadata;
    }
    
    public boolean hasNext() {
        return rows.hasNext();
    }

    public Record next() {
        if (rows.hasNext()) {
            ArrayList<String> fieldValues = new ArrayList<String>();
            Row row = rows.next();
            for (int col = 0; col < metadata.getFieldCount(); col ++) {
                Cell cell = row.getCell(col);
                if (cell == null) {
                    fieldValues.add(null);
                } else if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
                    fieldValues.add(cell.getStringCellValue());
                } else if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                    fieldValues.add(new DataFormatter().formatCellValue(cell));
                } else if (cell.getCellType() == Cell.CELL_TYPE_BLANK) {
                    fieldValues.add(null);
                } else {
                    throw new RuntimeException("Cell [" + cell.getRowIndex() + ", " + cell.getColumnIndex() + "] is of type " + cell.getCellType() + ".");
                }
            }
            return new Record(metadata, fieldValues.toArray(new String[0]));
        } else {
            throw new IllegalStateException();
        }
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

}
