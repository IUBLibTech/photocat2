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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import edu.indiana.dlib.catalog.dataimport.Metadata;
import edu.indiana.dlib.catalog.dataimport.Record;
import edu.indiana.dlib.catalog.dataimport.Records;

public class SpreadsheetRecords implements Records {
    
    private Workbook wb;
    
    int sheet;
    
    int row;

    public SpreadsheetRecords(File file) throws InvalidFormatException, IOException {
        this(file, 0, 0);
    }
    
    public SpreadsheetRecords(File file, int sheetIndex, int metadataRowIndex) throws InvalidFormatException, IOException {
        this(new FileInputStream(file), sheetIndex, metadataRowIndex);
    }
    
    public SpreadsheetRecords(InputStream is) throws InvalidFormatException, IOException {
        this(is, 0, 0);
    }
    
    public SpreadsheetRecords(InputStream is, int sheetIndex, int metadataRowIndex) throws InvalidFormatException, IOException {
        wb = WorkbookFactory.create(is);
        sheet = sheetIndex;
        row = metadataRowIndex;
    }
    
    public Metadata getMetadata() {
        try {
            return new SpreadsheetRecordIterator(wb, sheet, row).getMetdadata();
        } catch (InvalidFormatException ex) {
            throw new RuntimeException(ex);
        } catch (FileNotFoundException ex) {
            throw new RuntimeException(ex);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public Iterator<Record> iterator() {
        try {
            return new SpreadsheetRecordIterator(wb, sheet, row);
        } catch (InvalidFormatException ex) {
            throw new RuntimeException(ex);
        } catch (FileNotFoundException ex) {
            throw new RuntimeException(ex);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

}
