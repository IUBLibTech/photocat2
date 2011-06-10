/**
 * Copyright 2011, Trustees of Indiana University
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *   
 *   Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *   
 *   Neither the name of Indiana University nor the names of its
 *   contributors may be used to endorse or promote products derived from this
 *   software without specific prior written permission.
 *   
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. 
 */
package edu.indiana.dlib.catalog.dataimport.spreadsheet;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import edu.indiana.dlib.catalog.dataimport.Metadata;
import edu.indiana.dlib.catalog.dataimport.Record;

public class SpreadsheetRecordIterator implements Iterator<Record> {

    private Workbook workbook;

    private Metadata metadata; 
    
    private Iterator<Row> rows;
    
    public static Metadata readFirstRowAsMetadata(Workbook wb) {
        Iterator<Row> rows = wb.getSheetAt(0).iterator();
        if (rows.hasNext()) {
            ArrayList<String> fieldNames = new ArrayList<String>();
            for (Cell cell : rows.next()) {
                fieldNames.add(cell.getStringCellValue());
            }
            return new Metadata(fieldNames.toArray(new String[0]));
        }
        return null;
    }
    
    public SpreadsheetRecordIterator(Workbook wb) throws InvalidFormatException, FileNotFoundException, IOException {
        Sheet firstSheet = wb.getSheetAt(0);
        rows = firstSheet.iterator();
        metadata = readFirstRowAsMetadata(wb);
    }
    
    public boolean hasNext() {
        return rows.hasNext();
    }

    public Record next() {
        if (rows.hasNext()) {
            ArrayList<String> fieldValues = new ArrayList<String>();
            Row row = rows.next();
            for (Cell cell : row) {
                if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
                    fieldValues.add(cell.getStringCellValue());
                } else if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                    fieldValues.add(String.valueOf(cell.getNumericCellValue()));
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
