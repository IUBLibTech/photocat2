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
package edu.indiana.dlib.catalog.servlets;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.WorkbookUtil;
import org.springframework.context.ApplicationContext;

import edu.indiana.dlib.catalog.accesscontrol.AuthenticationManager;
import edu.indiana.dlib.catalog.accesscontrol.AuthorizationManager;
import edu.indiana.dlib.catalog.accesscontrol.UserInfo;
import edu.indiana.dlib.catalog.batch.BatchManager;
import edu.indiana.dlib.catalog.config.CollectionConfiguration;
import edu.indiana.dlib.catalog.config.ConfigurationManager;
import edu.indiana.dlib.catalog.config.FieldConfiguration;
import edu.indiana.dlib.catalog.config.FieldData;
import edu.indiana.dlib.catalog.config.Item;
import edu.indiana.dlib.catalog.config.NameValuePair;
import edu.indiana.dlib.catalog.pages.ApplicationPage;
import edu.indiana.dlib.catalog.search.SearchException;
import edu.indiana.dlib.catalog.search.SearchManager;
import edu.indiana.dlib.catalog.search.UnsupportedQueryException;
import edu.indiana.dlib.catalog.search.structured.DefaultPagingSpecification;
import edu.indiana.dlib.catalog.search.structured.DefaultStructuredSearchQuery;
import edu.indiana.dlib.catalog.search.structured.StructuredSearchQuery;
import edu.indiana.dlib.catalog.search.structured.StructuredSearchResults;
import edu.indiana.dlib.catalog.search.structured.constraints.CollectionSearchConstraint;

public class ExportCollectionServlet extends HttpServlet {

    private static Logger LOGGER = Logger.getLogger(ExportCollectionServlet.class);
    
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        process(req, resp);
    }
    
    private void process(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String collectionId = req.getParameter("cid");
        String newlineSubstitution = req.getParameter("nl");
        boolean structured = "true".equals(req.getParameter("structured"));
        Map<String, String> translationMap = null;
        if (newlineSubstitution != null) {
            translationMap = new HashMap<String, String>();
            translationMap.put("\n", newlineSubstitution);
        }
        int maxRecords = (req.getParameter("maxRecords") != null ? Integer.parseInt(req.getParameter("maxRecords")) : 10);
        ApplicationContext context = ApplicationPage.getApplicationContext(req.getSession().getServletContext());
        if (context == null) {
            throw new RuntimeException("Unable to find the ApplicationContext!");
        }
        AuthorizationManager am =  (AuthorizationManager) context.getBean("authorizationManager");
        ConfigurationManager cm = (ConfigurationManager) context.getBean("configurationManager");
        BatchManager bm = (BatchManager) context.getBean("batchManager");
        try {
            UserInfo currentUser = ((AuthenticationManager) context.getBean("authenticationManager")).getCurrentUser(req);
            if (am.canViewCollection(cm.getCollectionConfiguration(collectionId), cm.getParent(collectionId, false), currentUser)) {
                try {
                    Workbook wb = null;
                    if (!structured) {
                        wb = createSummaryWorkbook((SearchManager) context.getBean("searchManager"), cm.getCollectionConfiguration(collectionId), maxRecords, translationMap);
                    } else {
                        wb = createImportableWorkbook((SearchManager) context.getBean("searchManager"), cm.getCollectionConfiguration(collectionId), maxRecords, translationMap);
                    }
                    resp.setHeader("Content-disposition", "attachment; filename=\"" + URLEncoder.encode(encode(collectionId), "UTF-8") + ".xls\"");
                    resp.setContentType("application/vnd.ms-excel");
                    wb.write(resp.getOutputStream());
                    resp.getOutputStream().close();
                    return;
                } catch (Throwable t) {
                    LOGGER.error("Error generating spreadsheet!", t);
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    return;
                }
            } else {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            }
        } catch (Throwable t) {
            LOGGER.error("Error generating spreadsheet!", t);
            throw new ServletException(t);
        }
    } 
    
    public static String encode(String string) {
        return string.replace("/", "-");
    }
    
    /**
     * Creates a workbook containing every metadata record in the collection (up to maxRecord)
     * in a format suitable to summarize the data.  
     */
    public static Workbook createSummaryWorkbook(SearchManager sm, CollectionConfiguration collection, int maxRecords, Map<String, String> translationMap) throws SearchException, UnsupportedQueryException {
        Workbook wb = new HSSFWorkbook();
        Sheet sheet = wb.createSheet(WorkbookUtil.createSafeSheetName("export"));
        Row titleRow = sheet.createRow((short) 0);
        int col = 2;
        titleRow.createCell(0).setCellValue("id");
        titleRow.createCell(1).setCellValue("short id");
        for (FieldConfiguration field : collection.listFieldConfigurations(true)) {
            titleRow.createCell(col ++).setCellValue(field.getDisplayLabel());
        }
        StructuredSearchQuery query = new DefaultStructuredSearchQuery(new CollectionSearchConstraint(collection.getId()));
        StructuredSearchResults results = sm.search(query);
        for (int i = 0; i < results.getTotalResultsCount() && i < maxRecords; i ++) {
            Row row = sheet.createRow((short) i + 1);
            if (i - results.getStartingIndex() >= results.getResults().size()) {
                // fetch next page
                results = sm.search(new DefaultStructuredSearchQuery(DefaultPagingSpecification.getNextPage(results.getSearchQuery().getPagingSpecification()), query.getSearchConstraints()));
            }
            col = 2;
            try {
                Item item = results.getResults().get(i - results.getStartingIndex());
                row.createCell(0).setCellValue(item.getId());
                row.createCell(1).setCellValue(item.getIdWithinCollection());
                for (FieldConfiguration field : collection.listFieldConfigurations(true)) {
                    String value = collection.getValueSummary(item.getMetadata(), field.getFieldType());
                    if (translationMap != null && value != null) {
                        for (Map.Entry<String, String> entry : translationMap.entrySet()) {
                            value = value.replace(entry.getKey(), entry.getValue());
                        }
                    }
                    row.createCell(col ++).setCellValue(value);
                }
            } catch (Throwable t) {
                LOGGER.error("Error generating spreadsheet record " + i + " (aborting).", t);
                break;
            }
        }
        return wb;
    }
    
    /**
     * Creates a workbook containing every metadata record in the collection (up to maxRecord)
     * in a format suitable to be reimported.  For repeated values, this will create addition
     * rows below the first row for the record that have blank values except for the title
     * and repeated fields.  For example, a record with id "100" a single value field "name"
     * that equals "Doe, John" and a repeatable field "subject" with the values "one" and "two"
     * will result in the following spreadsheet:
     * <table>
     *   <tr>
     *     <th>id</th>
     *     <th>name</th>
     *     <th>subject</th>
     *   </tr>
     *   <tr>
     *     <td>100</td>
     *     <td>Doe, John</td>
     *     <td>one</td>
     *   </tr>
     *   <tr>
     *     <td>100</td>
     *     <td></td>
     *     <td>two</td>
     *   </tr>
     */
    public static Workbook createImportableWorkbook(SearchManager sm, CollectionConfiguration collection, int maxRecords, Map<String, String> translationMap) throws SearchException, UnsupportedQueryException {
        Workbook wb = new HSSFWorkbook();
        Sheet sheet = wb.createSheet(WorkbookUtil.createSafeSheetName("export"));
        Row titleRow = sheet.createRow((short) 0);
        int col = 1;
        titleRow.createCell(0).setCellValue("id");
        for (FieldConfiguration field : collection.listFieldConfigurations(true)) {
            for (String partName : collection.getEnabledPartNames(field)) {
                titleRow.createCell(col ++).setCellValue(field.getDisplayLabel() + ": " + field.getPartDisplayLabel(partName));
            }
            for (String attributeName : collection.getEnabledAttributeNames(field)) {
                titleRow.createCell(col ++).setCellValue(field.getDisplayLabel() + ": " + field.getAttributeDisplayLabel(attributeName));
            }
        }
        StructuredSearchQuery query = new DefaultStructuredSearchQuery(new CollectionSearchConstraint(collection.getId()));
        StructuredSearchResults results = sm.search(query);
        int rowIndex = 1;
        for (int i = 0; i < results.getTotalResultsCount() && i < maxRecords; i ++) {
            if (i - results.getStartingIndex() >= results.getResults().size()) {
                // fetch next page
                results = sm.search(new DefaultStructuredSearchQuery(DefaultPagingSpecification.getNextPage(results.getSearchQuery().getPagingSpecification()), query.getSearchConstraints()));
            }
            Item item = results.getResults().get(i - results.getStartingIndex());
            int maxRepeatableValueCount = 1;
            for (FieldConfiguration field : collection.listFieldConfigurations(true)) {
                FieldData data = item.getMetadata().getFieldData(field.getFieldType());
                maxRepeatableValueCount = Math.max(maxRepeatableValueCount, data != null ? data.getEnteredValueCount() : 0);
            }
            for (int rep = 0; rep < maxRepeatableValueCount; rep ++) {
                Row row = sheet.createRow(rowIndex ++);
                col = 1;
                try {
                    row.createCell(0).setCellValue(item.getId());
                    
                    for (FieldConfiguration field : collection.listFieldConfigurations(true)) {
                        FieldData data = item.getMetadata().getFieldData(field.getFieldType());
                        for (String partName : collection.getEnabledPartNames(field)) {
                            if (data != null) {
                                List<String> partValues = data.getPartValues(partName);
                                if (partValues != null && partValues.size() > rep) {
                                    row.createCell(col).setCellValue(partValues.get(rep));
                                }
                            }
                            col ++;
                        }
                        for (String attributeName : collection.getEnabledAttributeNames(field)) {
                            String attributeValue = null;
                            if (data != null) {
                                for (NameValuePair attribute : data.getAttributes()) {
                                    if (attribute.getName().equals(attributeName)) {
                                        attributeValue = attribute.getValue();
                                        break;
                                    }
                                }
                            }
                            if (attributeValue != null && rep == 0) {
                                row.createCell(col).setCellValue(attributeValue);
                            }
                            col ++;
                        }
                    }
                } catch (Throwable t) {
                    LOGGER.error("Error generating spreadsheet record " + i + " (aborting).", t);
                    break;
                }
            }
        }
        return wb;
    }
    
}
