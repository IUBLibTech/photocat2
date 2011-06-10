/*
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
package edu.indiana.dlib.catalog.servlets;

import java.io.IOException;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;

import edu.indiana.dlib.catalog.accesscontrol.AuthenticationManager;
import edu.indiana.dlib.catalog.accesscontrol.AuthorizationManager;
import edu.indiana.dlib.catalog.accesscontrol.UserInfo;
import edu.indiana.dlib.catalog.config.ConfigurationManager;
import edu.indiana.dlib.catalog.config.ItemManager;
import edu.indiana.dlib.catalog.config.TransformationConfiguration;
import edu.indiana.dlib.catalog.pages.ApplicationPage;

/**
 * A simple servlet to deliver XSLT transformations of item metadata.
 * This servlet requires the following request parameters:
 * <ul>
 *   <li>id - the id of the item</li>
 *   <li>cid - the id of the collection</li>
 *   <li>tid - the id of the transformation in the collection configuration</li>
 * </ul>
 */
public class TransformationServlet extends HttpServlet {

    private Logger LOGGER = Logger.getLogger(TransformationServlet.class);
    
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        process(req, resp);
    }
    
    private void process(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String id = req.getParameter("id");
        String collectionId = req.getParameter("cid");
        String transformationId = req.getParameter("tid");
        ApplicationContext context = ApplicationPage.getApplicationContext(req.getSession().getServletContext());
        if (context == null) {
            throw new RuntimeException("Unable to find the ApplicationContext!");
        }
        AuthorizationManager am =  (AuthorizationManager) context.getBean("authorizationManager");
        ConfigurationManager cm = (ConfigurationManager) context.getBean("configurationManager");
        ItemManager im = (ItemManager) context.getBean("itemManager");
        try {
            UserInfo currentUser = ((AuthenticationManager) context.getBean("authenticationManager")).getCurrentUser(req);
            if (am.canViewCollection(cm.getCollectionConfiguration(collectionId, false), currentUser)) {
                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                String xsltUrl = null;
                for (TransformationConfiguration tc : cm.getCollectionConfiguration(collectionId, false).getTransformationConfigurations()) {
                    if (tc.getId().equals(transformationId)) {
                        xsltUrl = tc.getXsltUrl();
                    }
                }
                Templates templates = transformerFactory.newTemplates(new StreamSource(new URL(xsltUrl).openStream()));
                Transformer transformer = templates.newTransformer();
                resp.setContentType("text/xml");
                resp.setCharacterEncoding("UTF-8");
                resp.setHeader("Content-disposition", "attachment; filename=\"" + id.substring(id.lastIndexOf('/') + 1) + ".xml\"");
                DOMSource source = new DOMSource(im.fetchItem(id).getMetadata().generateDocument());
                transformer.transform(source, new StreamResult(resp.getOutputStream()));
                resp.getOutputStream().close();
                return;
            } else {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            }
        } catch (Throwable t) {
            LOGGER.error("Error transforming record " + id + " (collection = " + collectionId + ", transformation = " + transformationId + ")", t);
            throw new ServletException(t);
        }
    }   
}
