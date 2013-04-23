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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import edu.indiana.dlib.catalog.accesscontrol.impl.CASAuthenticationManager;

/**
 * A servlet that takes a request with a path of
 * /collections/[collection/name]/resource.extension parses
 * out the "collection/name", puts it as a request attribute
 * called "collectionId" and forwards processing to the URL
 * without the /collections/[collection/name] part. 
 *
 * This allows for attractive URLs specifying the collection 
 * rather than requiring parameters or session-scoped attributes.
 * 
 * The current implementation only forwards POST and GET 
 * operations, not because there's any problem with the
 * others, simply because there was no need at implementation time.
 */
public class CollectionPathRedirectionServlet extends HttpServlet {

    private Logger LOGGER = Logger.getLogger(CollectionPathRedirectionServlet.class);
    
    public static final String COLLECTION_ID_REQUEST_ATTRIBUTE = "collectionId";
    
    public static final Pattern URL_PATTERN = Pattern.compile("/(.+?)(/[^/]*)");
    
    private String collectionPath;
    
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        this.collectionPath = config.getInitParameter("collection-path");
    }
    
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        forward(req, resp);
    }
    
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        forward(req, resp);
    }
    
    private void forward(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        Matcher m = URL_PATTERN.matcher(path);
        if (m.matches()) {
            String collectionId = m.group(1);
            String newPath = this.collectionPath + m.group(2);
            req.setAttribute(CASAuthenticationManager.PRE_FORWARD_REQUEST_ATTRIBUTE_NAME, req.getRequestURL().toString());
            req.setAttribute(COLLECTION_ID_REQUEST_ATTRIBUTE, collectionId);
            req.getRequestDispatcher(newPath).forward(req, resp);
            LOGGER.debug(path + " --> " + newPath + " (" + collectionId + ")");
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            LOGGER.info(path + " --> 404");
        }        
    }
    
    /**
     * Gets the path as it was before forwarding the request.  This excludes the
     * leading context path.
     */
    public static String getRequestPath(HttpServletRequest request) {
        Object originalRequestUrl = request.getAttribute(CASAuthenticationManager.PRE_FORWARD_REQUEST_ATTRIBUTE_NAME);
        try {
            if (originalRequestUrl != null) {
                return new URL((String) originalRequestUrl).getPath().substring(request.getContextPath().length());
            } else {
                return new URL(request.getRequestURL().toString()).getPath().substring(request.getContextPath().length());
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("Request URL wasn't a URL!", ex);
        }
    }
}
