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
import java.io.OutputStream;
import java.net.URLEncoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;

import edu.indiana.dlib.catalog.accesscontrol.AuthenticationManager;
import edu.indiana.dlib.catalog.accesscontrol.AuthorizationManager;
import edu.indiana.dlib.catalog.accesscontrol.UserInfo;
import edu.indiana.dlib.catalog.batch.BatchManager;
import edu.indiana.dlib.catalog.config.ConfigurationManager;
import edu.indiana.dlib.catalog.pages.ApplicationPage;

public class ExportCollectionConfigurationServlet extends HttpServlet {

    private static Logger LOGGER = Logger.getLogger(ExportCollectionConfigurationServlet.class);
    
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        process(req, resp);
    }
    
    private void process(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String collectionId = req.getParameter("cid");
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
                    
                    resp.setHeader("Content-disposition", "attachment; filename=\"" + URLEncoder.encode(encode(collectionId), "UTF-8") + "-configuration.xml\"");
                    resp.setContentType("text/xml");
                    writeOutXML(cm.getCollectionConfiguration(collectionId).generateDocument(), resp.getOutputStream());
                    resp.getOutputStream().close();
                    return;
                } catch (Throwable t) {
                    LOGGER.error("Error exporting collection configuration!", t);
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    return;
                }
            } else {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            }
        } catch (Throwable t) {
            LOGGER.error("Error exporting collection configuration!", t);
            throw new ServletException(t);
        }
    }
    
    public static String encode(String string) {
        return string.replace("/", "-");
    }
   
    private static void writeOutXML(Document doc, OutputStream stream) throws TransformerException {
        DOMSource source = new DOMSource(doc);
        TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer t = tFactory.newTransformer();
        t.setOutputProperty(OutputKeys.ENCODING, "utf-8");
        t.setOutputProperty(OutputKeys.METHOD, "xml");
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        t.transform(source, new StreamResult(stream));
    }
    
}
