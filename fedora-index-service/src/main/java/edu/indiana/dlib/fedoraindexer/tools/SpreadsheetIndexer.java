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
package edu.indiana.dlib.fedoraindexer.tools;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.ValidationException;
import org.exolab.castor.xml.XMLContext;

import edu.indiana.dlib.search.analyzers.CaseInsensitiveSRUSupportAnalyzer;
import edu.indiana.dlib.xml.zthes.Relation;
import edu.indiana.dlib.xml.zthes.Term;
import edu.indiana.dlib.xml.zthes.TermNote;
import edu.indiana.dlib.xml.zthes.Zthes;

/**
 * <p>
 *   A simple command-line tool that parses limited vocabulary 
 *   information from an Excel Spreadsheet. 
 * </p>
 * <p>
 *   The current implementation indexes each known field listed
 *   in the spreadsheet.  Furthermore an ".exact" and ".facet"
 *   form will be indexed as well and a simple "zthes" field 
 *   containing a valid ZThes XML fragment that represents the
 *   whole record.
 * </p>
 */
public class SpreadsheetIndexer {

    /**
     * The leftmost column for an array of cells
     * whose values represent the hierarchy of 
     * broader to narrower terms.
     */
    private static final int TERM_HIERARCHY_COL_LOWEST_INDEX = 0;
    private static final int TERM_HIERARCHY_COL_HIGHEST_INDEX = 1;
    private static final int DESCRIPTION_COL_INDEX = 2;

    /**
     * A prefix for the URI (identifier) for terms.  The full
     * identifier will be the prefix plus a massaged version of
     * the term name.  If this is null, the term id will simply
     * be the same as the term name.
     */
    private String uriPrefix;
    
    public static void main(String[] args) throws Exception {
        File spreadsheetFile = new File(args[0]);
        Analyzer analyzer = new CaseInsensitiveSRUSupportAnalyzer();
        IndexWriter writer = new IndexWriter(args[1], analyzer);
        SpreadsheetIndexer indexer = new SpreadsheetIndexer(args[2]);
        try {
            int count = indexer.indexSpreadsheet(spreadsheetFile, writer, Integer.parseInt(args[3]), 1);
            System.out.println("Indexed " + count + " terms");
            writer.flush();
            //IndexReader reader = IndexReader.open(args[1]);
            //GettyTGNParser.addNTRelationships(reader, writer, analyzer, "dc.identifier.facet");
            //reader.close();
        } finally {
            writer.optimize();
            writer.close();
        }
    }
    
    public SpreadsheetIndexer(String uriPrefix) {
        this.uriPrefix = uriPrefix;
    }
    
    public int indexSpreadsheet(File spreadsheetFile, IndexWriter writer, int sheetNum, int rowOffset) throws BiffException, IOException {
        Analyzer analyzer = new CaseInsensitiveSRUSupportAnalyzer();
        
        Workbook workbook = Workbook.getWorkbook(spreadsheetFile);
        int count = 0;
        Sheet sheet = workbook.getSheet(sheetNum);
        for (int row = rowOffset; row < sheet.getRows(); row ++) {
            Document indexDoc = new Document();
            Term currentTerm = new Term();
            Cell termNameCell = getTermNameCell(sheet, row);
            Cell broaderTermNameCell = getBroaderTermNameCell(sheet, row);
            Cell descriptionCell = null;
            try {
                descriptionCell = sheet.getCell(DESCRIPTION_COL_INDEX, row);
            } catch (ArrayIndexOutOfBoundsException ex) {
                // descriptionCell will be null
            }
            if (termNameCell != null) {
                String termName = termNameCell.getContents().trim();
                currentTerm.setTermName(termName);
                indexDoc.add(new Field("dc.title", termName, Field.Store.NO, Field.Index.TOKENIZED));
                indexDoc.add(new Field("dc.title.exact", termName, Field.Store.NO, Field.Index.TOKENIZED));
                indexDoc.add(new Field("dc.title.facet", termName, Field.Store.YES, Field.Index.TOKENIZED));

                String termId = generateTermId(termName);
                currentTerm.setTermId(termId);
                indexDoc.add(new Field("dc.identifier", termId, Field.Store.NO, Field.Index.TOKENIZED));
                indexDoc.add(new Field("dc.identifier.exact", termId, Field.Store.NO, Field.Index.TOKENIZED));
                indexDoc.add(new Field("dc.identifier.facet", termId, Field.Store.YES, Field.Index.TOKENIZED));
            }
            if (broaderTermNameCell != null) {
                String broaderTermName = broaderTermNameCell.getContents().trim();
                String broaderTermId = generateTermId(broaderTermName);
                Relation bt = new Relation();
                bt.setRelationType("BT");
                bt.setTermId(broaderTermId);
                currentTerm.addRelation(bt);
                indexDoc.add(new Field("nt", broaderTermId, Field.Store.NO, Field.Index.TOKENIZED));
                indexDoc.add(new Field("nt.exact", broaderTermId, Field.Store.NO, Field.Index.TOKENIZED));
                indexDoc.add(new Field("nt.facet", broaderTermId, Field.Store.YES, Field.Index.TOKENIZED));
            }
            if (descriptionCell != null) {
                String description = descriptionCell.getContents().trim();
                TermNote note = new TermNote();
                note.setLabel("description");
                note.setContent(description);
                currentTerm.addTermNote(note);
                    
                indexDoc.add(new Field("description", description, Field.Store.NO, Field.Index.TOKENIZED));
            }
            if (termNameCell != null) {
                List<String> narrowerTerms = this.getNarrowerTermNames(sheet, termNameCell.getColumn(), termNameCell.getRow());
                if (narrowerTerms != null && !narrowerTerms.isEmpty()) {
                    for (String ntName : narrowerTerms) {
                        //System.out.println(ntName + " --> " + currentTerm.getTermName());
                        String narrowerTermId = generateTermId(ntName);
                        Relation nt = new Relation();
                        nt.setRelationType("NT");
                        nt.setTermId(narrowerTermId);
                        currentTerm.addRelation(nt);
                        indexDoc.add(new Field("bt", narrowerTermId, Field.Store.NO, Field.Index.TOKENIZED));
                        indexDoc.add(new Field("bt.exact", narrowerTermId, Field.Store.NO, Field.Index.TOKENIZED));
                        indexDoc.add(new Field("bt.facet", narrowerTermId, Field.Store.YES, Field.Index.TOKENIZED));
                    }
                }
            }
            currentTerm.setTermType("PT");
                
            // add a zthes XML record
            try {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                
                // create a new Marshaller
                XMLContext context = new XMLContext();
                Marshaller marshaller = context.createMarshaller();
                marshaller.setValidation(false);
                marshaller.setSupressXMLDeclaration(true);
                //marshaller.setEncoding("UTF-8");
                marshaller.setWriter(new OutputStreamWriter(os, "UTF-8"));
                
                // marshal the zthes record
                Zthes zthes = new Zthes();
                zthes.addTerm(currentTerm);
                marshaller.marshal(zthes);
                String zthesXml = new String(os.toByteArray(), "UTF-8");
                // The "setSuppressXMLDeclaration" doesn't seem
                // to be respected here, so we manually remove it.
                zthesXml = zthesXml.substring(zthesXml.indexOf("<Zthes"));
                Field xmlField = new Field("zthes", zthesXml, Field.Store.COMPRESS, Field.Index.NO);
                indexDoc.add(xmlField);
            } catch (MarshalException ex) {
                throw new RuntimeException(ex);
            } catch (ValidationException ex) {
                throw new RuntimeException(ex);
            }
            writer.addDocument(indexDoc, analyzer);
            count ++;
        }
        return count;
    }
    
    /**
     * Locates and returns the cell representing the 
     * term in the current row if one exists.
     * The current implementation finds the right-most entered
     * value and and considers that to be the term.
     */
    private Cell getTermNameCell(Sheet sheet, int row) {
        for (int i = TERM_HIERARCHY_COL_HIGHEST_INDEX; i >= TERM_HIERARCHY_COL_LOWEST_INDEX; i --) {
            try {
                Cell cell = sheet.getCell(i, row);
                if (cell.getContents().trim().length() > 0) {
                    return cell;
                }
            } catch (ArrayIndexOutOfBoundsException ex) {
                // fall through to next iteration, this is equivalent to
                // no value being specified
            }
        }
        return null;
    }
    
    /**
     * Gets all narrower terms for the term in the given row and column 
     * by traversing the spreadsheet from the initial cell.
     */
    private List<String> getNarrowerTermNames(Sheet sheet, int col, int row) {
        if (col < TERM_HIERARCHY_COL_HIGHEST_INDEX) {
            List<String> ntList = new ArrayList<String>();
            String termName = sheet.getCell(col, row).getContents();
            for (int i = row + 1; ; i ++) {
                try {
                    Cell nextCell = sheet.getCell(col, i);
                    String nextCellValue = nextCell.getContents();
                    if (nextCellValue.equals("") || nextCellValue.equals(termName)) {
                        String nt = sheet.getCell(col + 1, i).getContents();
                        if (nt != null || nt.trim().length() > 0) {
                            ntList.add(nt);
                        }
                    } else {
                        break;
                    }
                } catch (ArrayIndexOutOfBoundsException ex) {
                    break;
                }
            }
            return ntList;
        } else {
            return null;
        }
    }
    
    /**
     * Locates and returns the cell representing the broader
     * term for the term in the current row if one exists.
     * The current implementation finds the right-most entered
     * value and and considers that to be the term, and then
     * searches the cell to the left of it.  If that cell is 
     * empty, it searches the cell above it, and so forth.
     */
    private Cell getBroaderTermNameCell(Sheet sheet, int row) {
        boolean foundTerm = false;
        for (int i = TERM_HIERARCHY_COL_HIGHEST_INDEX; i >= TERM_HIERARCHY_COL_LOWEST_INDEX; i --) {
            if (foundTerm == true) {
                for (int r = row; r > 0; r --) {
                    Cell cell = sheet.getCell(i, r);
                    if (cell.getContents().trim().length() > 0) {
                        return cell;
                    }
                }
            } else {
                try {
                    Cell cell = sheet.getCell(i, row);
    
                    if (cell.getContents().trim().length() > 0) {
                        foundTerm = true;
                    }
                } catch (ArrayIndexOutOfBoundsException ex) {
                    // fall through to next iteration, this is equivalent to
                    // no value being specified
                }
            }
        }
        return null;
    }
    
    private String generateTermId(String name) throws UnsupportedEncodingException {
        if (this.uriPrefix == null) {
            return name;
        } else {
            return this.uriPrefix + URLEncoder.encode(name.toLowerCase(), "UTF-8");
        }
    }
}
