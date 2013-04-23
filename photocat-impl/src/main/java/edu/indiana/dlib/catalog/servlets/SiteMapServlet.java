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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import edu.indiana.dlib.catalog.config.CollectionConfiguration;
import edu.indiana.dlib.catalog.config.ConfigurationManager;
import edu.indiana.dlib.catalog.config.Item;
import edu.indiana.dlib.catalog.pages.ApplicationPage;
import edu.indiana.dlib.catalog.search.SearchManager;
import edu.indiana.dlib.catalog.search.structured.DefaultPagingSpecification;
import edu.indiana.dlib.catalog.search.structured.DefaultStructuredSearchQuery;
import edu.indiana.dlib.catalog.search.structured.SearchConstraint;
import edu.indiana.dlib.catalog.search.structured.StructuredSearchResults;

/**
 * A servlet that generates a site map that contains every
 * jsp file in the root directory and the display item page
 * for every item that can be discovered using the search 
 * configuration. 
 */
public class SiteMapServlet extends HttpServlet {

    private static Log LOG = LogFactory.getLog(SiteMapServlet.class);
    
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        process(req, resp);
    }
    
    private void process(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        try {
            resp.setContentType("text/xml");
            resp.setCharacterEncoding("UTF-8");
            Document siteMapDoc = getSiteMapDoc(req);
            writeOutXML(siteMapDoc, resp.getOutputStream());
            resp.getOutputStream().close();
            LOG.info("sitemap.xml " + req.getRemoteAddr() + " took " + (System.currentTimeMillis() - start) + "ms to generate (or fetch from cache).");
            return;
        } catch (Exception ex) {
            LOG.error("Error generating sitemap, returned a 404!", ex);
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
    }

    
    private Document generateSiteMapDocumentFromSource(HttpServletRequest request) throws Exception {
        ApplicationContext context = ApplicationPage.getApplicationContext(request.getSession().getServletContext());
        if (context == null) {
            throw new RuntimeException("Unable to find the ApplicationContext!");
        }
        ConfigurationManager cm = (ConfigurationManager) context.getBean("configurationManager");
        SearchManager sm = (SearchManager) context.getBean("searchManager");
        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();
        Element documentEl = doc.createElementNS("http://www.sitemaps.org/schemas/sitemap/0.9", "urlset");
        doc.appendChild(documentEl);
        
        String requestUrl = request.getRequestURL().toString();
        String requestPath = requestUrl.substring(0, requestUrl.lastIndexOf('/') + 1);
        
        // Add the home page and all the splash pages
        List<String> paths = new ArrayList<String>();
        paths.add("public-index.htm");
        for (CollectionConfiguration u : cm.getCollectionConfigurations(ConfigurationManager.UNIT, true)) {
            paths.add("splash.htm?scope=" + u.getId());
            for (CollectionConfiguration c : cm.getChildren(u.getId(), ConfigurationManager.COLLECTION, true)) {
                paths.add("splash.htm?scope=" + c.getId());
            }
        }
        for (CollectionConfiguration c : cm.getOrphans(ConfigurationManager.COLLECTION, true)) {
            paths.add("splash.htm?scope=" + c.getId());
        }
        for (String path : paths) {
            Element urlEl = doc.createElementNS("http://www.sitemaps.org/schemas/sitemap/0.9", "url");
            Element locEl = doc.createElementNS("http://www.sitemaps.org/schemas/sitemap/0.9", "loc");
            locEl.appendChild(doc.createTextNode(requestPath + path));
            urlEl.appendChild(locEl);
            documentEl.appendChild(urlEl);
        }
        
        List<SearchConstraint> constraints = cm.getPublicRecordsSearchConstraints();
        StructuredSearchResults results = sm.search(new DefaultStructuredSearchQuery(new DefaultPagingSpecification().pageSize(100), constraints));
        do {
            for (Item item : results.getResults()) {
                Element urlEl = doc.createElementNS("http://www.sitemaps.org/schemas/sitemap/0.9", "url");
                Element locEl = doc.createElementNS("http://www.sitemaps.org/schemas/sitemap/0.9", "loc");
                locEl.appendChild(doc.createTextNode(requestPath + "item.htm?id=" + item.getId()));
                urlEl.appendChild(locEl);
                documentEl.appendChild(urlEl);   
            }
            DefaultStructuredSearchQuery nextPageQuery = DefaultStructuredSearchQuery.nextPageQuery(results);
            if (nextPageQuery == null) {
                break;
            }
            results = sm.search(nextPageQuery);
        } while (results.getResults().size() > 0);
        return doc;
    }
    
    private Document getSiteMapDoc(HttpServletRequest request) throws Exception {
        String cacheFilename = getInitParameter("cacheFilename");
        if (cacheFilename != null) {
            if (cacheFilename.startsWith("/")) {
                cacheFilename = request.getSession().getServletContext().getRealPath(cacheFilename);
            }
            File cacheFile = new File(cacheFilename);
            if (cacheFile.exists() && ((System.currentTimeMillis() - cacheFile.lastModified()) < 86400000)) {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(true);
                DocumentBuilder builder = factory.newDocumentBuilder();
                return builder.parse(cacheFile);
            } else {
                LOG.info("Cache was found, but more than 1 day old.");
                Document doc = generateSiteMapDocumentFromSource(request);
                writeOutXML(doc, new FileOutputStream(cacheFile));
                return doc;
            }
        } else {
            LOG.warn("No cache initialized for the sitemap.xml (cacheFilename parameter is missing)!");
            return generateSiteMapDocumentFromSource(request);
        }
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
