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
import java.util.Collections;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.WorkbookUtil;
import org.springframework.context.ApplicationContext;

import edu.indiana.dlib.catalog.accesscontrol.AuthenticationManager;
import edu.indiana.dlib.catalog.accesscontrol.AuthorizationManager;
import edu.indiana.dlib.catalog.accesscontrol.UserInfo;
import edu.indiana.dlib.catalog.config.ConfigurationManager;
import edu.indiana.dlib.catalog.pages.ApplicationPage;
import edu.indiana.dlib.catalog.search.BrowseResult;
import edu.indiana.dlib.catalog.search.BrowseResults;
import edu.indiana.dlib.catalog.search.SearchManager;
import edu.indiana.dlib.catalog.search.impl.DefaultBrowseQuery;
import edu.indiana.dlib.catalog.search.impl.FieldPartBrowseSet;
import edu.indiana.dlib.catalog.search.structured.DefaultPagingSpecification;
import edu.indiana.dlib.catalog.search.structured.SearchConstraint;
import edu.indiana.dlib.catalog.search.structured.constraints.CollectionSearchConstraint;

public class BrowseSpreadsheetExportServlet extends HttpServlet {

    private Logger LOGGER = Logger.getLogger(BrowseSpreadsheetExportServlet.class);
    
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        process(req, resp);
    }
    
    private void process(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String collectionId = req.getParameter("cid");
        String termType = req.getParameter("type");
        String partName = req.getParameter("part");
        ApplicationContext context = ApplicationPage.getApplicationContext(req.getSession().getServletContext());
        if (context == null) {
            throw new RuntimeException("Unable to find the ApplicationContext!");
        }
        AuthorizationManager am =  (AuthorizationManager) context.getBean("authorizationManager");
        ConfigurationManager cm = (ConfigurationManager) context.getBean("configurationManager");
        SearchManager sm = (SearchManager) context.getBean("searchManager");
        try {
            UserInfo currentUser = ((AuthenticationManager) context.getBean("authenticationManager")).getCurrentUser(req);
            if (am.canViewCollection(cm.getCollectionConfiguration(collectionId), cm.getParent(collectionId, false), currentUser)) {
            	BrowseResults results = sm.browse(new DefaultBrowseQuery(new FieldPartBrowseSet("browse", termType, partName), Collections.singletonList((SearchConstraint) new CollectionSearchConstraint(collectionId)), new DefaultPagingSpecification().pageSize(2500)));
                
            	Workbook wb = new HSSFWorkbook();
            	Sheet sheet = wb.createSheet(WorkbookUtil.createSafeSheetName("Browse Results"));
            	for (int i = 0; i < results.listBrowseResults().size(); i ++) {
	            	Row row = sheet.createRow((short) i);
	                // Create a cell and put a value in it.
	            	BrowseResult result = results.listBrowseResults().get(i);
	                Cell cell = row.createCell(0);
	                cell.setCellValue(result.getFieldValue());
	                Cell cell2 = row.createCell(1);
	                cell2.setCellValue(result.getHitCount());
            	}
            	
            	resp.setContentType("application/vnd.ms-excel");
            	wb.write(resp.getOutputStream());
            	resp.getOutputStream().close();
                return;
            } else {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            }
        } catch (Throwable t) {
            LOGGER.error("Error generating browse results spreadsheet!", t);
            throw new ServletException(t);
        }
    } 
	
	
}
