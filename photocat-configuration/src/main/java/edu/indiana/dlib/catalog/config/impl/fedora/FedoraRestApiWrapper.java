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
package edu.indiana.dlib.catalog.config.impl.fedora;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.ByteArrayPartSource;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import edu.indiana.dlib.catalog.config.CollectionConfigurationData;
import edu.indiana.dlib.catalog.config.Item;
import edu.indiana.dlib.catalog.config.ItemMetadata;

/**
 * <p>
 *   A class that wraps the fedora REST API calls needed
 *   by this application.  This could easily be replaced
 *   with any other codebase that interacts with fedora.
 *   TODO: replace this with the mediashelf fedora client
 * </p>
 * <p>
 *   <strong>This class is thread safe</strong>
 * </p>
 * 
 */
public class FedoraRestApiWrapper {

    public static final Logger LOGGER = Logger.getLogger(FedoraRestApiWrapper.class);
    
    public static final String IS_COLLECTION_MEMBER = "info:fedora/fedora-system:def/relations-external#isMemberOfCollection";

    public static final String IS_PART_OF = "info:fedora/fedora-system:def/relations-external#isPartOf";
    
    public final static String HAS_MODEL = "info:fedora/fedora-system:def/model#hasModel";
    
    
    public static enum ControlGroup {
        /** Inline XML */
        X,
        
        /** Managed content */
        M,
        
        /** Redirect */
        R,
        
        /** External Reference */
        E;
    }
    
    /**
     * The date format used to parse and generate dates as represented in
     * fedora.
     */
    private static DateFormat FEDORA_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    static {
        FEDORA_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
    }
    
    /**
     * Converts a fedora date String (like "2010-10-01T19:55:00.808Z") to
     * a java Date object.
     */
    public static Date parseFedoraDate(String fedoraDateStr) throws ParseException {
        return FEDORA_DATE_FORMAT.parse(fedoraDateStr);
    }

    /**
     * Converts a java Date object into a fedora-formatted date String.
     */
    public static String printFedoraDateString(Date date) {
        return FEDORA_DATE_FORMAT.format(date);
    }
    
    /**
     * An underlying HttpClient that handles the REST calls.  This
     * client is initialized at construction time.
     */
    protected HttpClient client;

    /** 
     * A document builder for building Documents from XML.  This
     * variable is not initialized at construction time, but instead
     * serves as a cache for the fist DocumentBuilder instance
     * created by calls that require it.  All access to this member
     * variable should be mediated through getDocumentBuilder().
     */
    private DocumentBuilder documentBuilder;
    
    /**
     * An XPath implementation for dealing with Documents.  This
     * variable is not initialized at construction time, but instead
     * serves as a cache for the first XPath instance created by
     * calls that require it.  All access to this member variable
     * shoudl be mediated through getXPath().
     */
    private XPath xpath;
    
    /**
     * The base URL for fedora calls. 
     */
    protected String fedoraBaseUrl;
    
    /**
     * If set to true, all operations that would update the
     * repository are blocked and will throw an IllegalStateException.
     */
    protected boolean readOnly;
    
    /**
     * Instantiates an unauthenticated FedoraRestApiWrapper.
     * @param fedoraHost the hostname of the fedora server
     * @param fedoraContextName the fedora context name 
     * (likely "fedora")
     * @param port fedora's port
     */
    public FedoraRestApiWrapper(String fedoraHost, String fedoraContextName, int port, boolean readOnly) {
        this(null, null, fedoraHost, fedoraContextName, port, readOnly);
    }
    
    /**
     * Instantiates a potentially authenticated FedoraRestApiWrapper.
     * @param username the username (or null for anonymous access)
     * @param password the password (or null for anonymous access)
     * @param fedoraHost the hostname of the fedora server
     * @param fedoraContextName the fedora context name 
     * (likely "fedora")
     * @param port fedora's port
     */
    public FedoraRestApiWrapper(String username, String password, String fedoraHost, String fedoraContextName, int fedoraPort, boolean readOnly) {
        this.fedoraBaseUrl = "http://" + fedoraHost + ":" + fedoraPort + "/" + fedoraContextName;
        
        // Create an HTTP client for future REST calls
        MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
        HttpConnectionManagerParams params = new HttpConnectionManagerParams();
        params.setDefaultMaxConnectionsPerHost(10);
        params.setMaxTotalConnections(25);
        params.setConnectionTimeout(15000);
        params.setSoTimeout(15000);
        connectionManager.setParams(params);
        client = new HttpClient(connectionManager);
        if (username != null) {
            this.client.getParams().setAuthenticationPreemptive(true);
            Credentials credentials = new UsernamePasswordCredentials(username, password);
            this.client.getState().setCredentials(new AuthScope(fedoraHost, AuthScope.ANY_PORT), credentials);
        }

        this.readOnly = readOnly;
    }
    
    public MultiThreadedHttpConnectionManager getConnectionManager() {
        return (MultiThreadedHttpConnectionManager) client.getHttpConnectionManager();
    }
    
    /**
     * Gets or creates a DocumentBuilder.
     */
    protected synchronized DocumentBuilder getDocumentBuilder() throws ParserConfigurationException {
        if (documentBuilder != null) {
            return documentBuilder;
        } else {
            // create the document builder
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            this.documentBuilder = factory.newDocumentBuilder();
            return this.documentBuilder;
        }
    }
    
    /**
     * Determines whether this FedoraClient is read-only.
     */
    public boolean isReadOnly() {
        return this.readOnly;
    }
    
    /**
     * Gets the fedora server URL being used by this client instance.
     */
    public String getServerUrl() {
        return this.fedoraBaseUrl;
    }
    
    /**
     * Gets or creates and XPath configured with namespaces appropriate
     * for all other internal methods.
     */
    protected XPath getXPath() {
        if (this.xpath != null) {
            return this.xpath;
        } else {
            this.xpath = new net.sf.saxon.xpath.XPathFactoryImpl().newXPath();
            this.xpath.setNamespaceContext(this.createNamespaceContext());
            return this.xpath;
        }
    }
    
    /**
     * Creates a MapNamespace context to be attached to the
     * XPath when it's generated.
     * 
     * Subclasses may override this method, but should ensure that the
     * MapNamespaceContext returned by this class has a superset of the
     * mappings to ensure that other methods will function as expected.
     */
    protected MapNamespaceContext createNamespaceContext() {
        // create the xpath with fedora namespaces built in
        MapNamespaceContext nsc = new MapNamespaceContext();
        nsc.setNamespace("fedora-types", "http://www.fedora.info/definitions/1/0/types/");
        nsc.setNamespace("sparql", "http://www.w3.org/2001/sw/DataAccess/rf1/result");
        nsc.setNamespace("foxml", "info:fedora/fedora-system:def/foxml#");
        nsc.setNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        nsc.setNamespace("fedora", "info:fedora/fedora-system:def/relations-external#");
        nsc.setNamespace("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        nsc.setNamespace("fedora-model", "info:fedora/fedora-system:def/model#");
        nsc.setNamespace("oai", "http://www.openarchives.org/OAI/2.0/");
        nsc.setNamespace("oai_dc", "http://www.openarchives.org/OAI/2.0/oai_dc/", "http://www.openarchives.org/OAI/2.0/oai_dc.xsd");
        nsc.setNamespace("dc", "http://purl.org/dc/elements/1.1/"); 
        nsc.setNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        nsc.setNamespace("fedora-management", "http://www.fedora.info/definitions/1/0/management/", "http://www.fedora.info/definitions/1/0/datastreamHistory.xsd");
        return nsc;
    }
    
    /**
     * Uses the Resource Index search to find the objects with the given
     * dc.identifier.
     * 
     * This method was added in version 2.0.2
     * 
     * @param identifier the value to be searched for
     * @returns a List (possibly empty, but never null) of pids with the
     * given identifier.
     */
    public List<String> dcIdentifierLookup(String identifier) throws FedoraException, IOException {
        String riSearchUrl = this.fedoraBaseUrl + (this.fedoraBaseUrl.endsWith("/") ? "" : "/") + "risearch?type=tuples&lang=itql&format=Sparql&query=select%20%24member%20from%20%3C%23ri%3E%20where%20%24member%20%3Cdc%3Aidentifier%3E%20'" + URLEncoder.encode(identifier, "UTF-8")+ "'";
        try {
            List<String> pids = new ArrayList<String>();
            Document doc = parseUrlAsXmlDocument(riSearchUrl);
            NodeList children = (NodeList) this.getXPath().evaluate("//sparql:sparql/sparql:results/sparql:result/sparql:member", doc, XPathConstants.NODESET);
            if (children.getLength() == 0) {
                LOGGER.warn("No object found with dc.identifier=\"" + identifier + "\"");
                return pids;
            } else  {
                
                for (int i = 0; i < children.getLength(); i ++) {
                    String pid = ((Element) children.item(i)).getAttribute("uri").replace("info:fedora/", "");
                    LOGGER.debug("PID, \"" + pid + "\", found object with dc.identifier=\"" + identifier + "\"");
                    pids.add(pid);
                }
                return pids;
            }
        } catch (MalformedURLException ex) {
            throw new FedoraException(ex);
        } catch (SAXException ex) {
            throw new FedoraResponseParsingException(ex);
        } catch (ParserConfigurationException ex) {
            throw new FedoraResponseParsingException(ex);
        } catch (XPathExpressionException ex) {
            throw new FedoraException(ex);
        }
    }
    
    public String getIdForPid(String pid) throws FedoraException, IOException {
        Document dcDoc = getXMLDatastreamAsDocument(pid, "DC");
        NodeList idNl = null;
        try {
            idNl = (NodeList) this.getXPath().evaluate("/oai_dc:dc/dc:identifier", dcDoc, XPathConstants.NODESET);
        } catch (XPathExpressionException ex) {
            // shouldn't happen because XPATH isn't variable
            throw new FedoraException(ex);
        }
        if (idNl == null) {
            return null;
        } else {
            for (int i = 0; i < idNl.getLength(); i ++) {
                Element id = (Element) idNl.item(i);
                String value = id.getFirstChild().getNodeValue();
                if (!value.equals(pid)) {
                    return value;
                }
            }
        }
        return null;
    }
    
    public Document getXMLDatastreamAsDocument(String pid, String dsName) throws FedoraException, IOException {
        return getXMLDatastreamAsDocument(pid, dsName, null);
    }
    
    public Document getXMLDatastreamAsDocument(String pid, String dsName, String date) throws FedoraException, IOException {
        try {
            String url = this.fedoraBaseUrl + "/objects/" + pid + "/datastreams/" + dsName + "/content" + (date != null ? "?asOfDateTime=" + URLEncoder.encode(date, "UTF-8") : "");
            GetMethod get = new GetMethod(url);
            try {
                client.executeMethod(get);
                int status = get.getStatusCode();
                if (status != HttpStatus.SC_CREATED && status != HttpStatus.SC_OK) {
                    throw new RuntimeException("REST action \"" + url + "\" failed: " + get.getStatusLine());
                }
                InputStream is = get.getResponseBodyAsStream();
                Document doc = null;
                DocumentBuilder parser = getDocumentBuilder();
                try {
                    synchronized (parser) {
                        doc = parser.parse(is);
                    }
                } finally {
                    is.close();
                }
                return doc;
            } finally {
                get.releaseConnection();
            }
        } catch (SAXException ex) {
            throw new FedoraResponseParsingException(ex);
        } catch (ParserConfigurationException ex) {
            throw new FedoraResponseParsingException(ex);
        }
    }
    
    /**
     * Replaces the given object's datastream with an XML serialized version of the
     * provided document.
     * @param pid the pid of the object to update
     * @param dsName the id of the datastream to replace or create
     * @param controlGroup the control group (used when a datasteram is created)
     * @param label the new datastream label
     * @param mimetype the mime type of the datastream
     * @param doc the Document for the XML datasteram
     * @param filename the filename from which the XML was read
     */
    public void addOrReplaceDatastreamWithDocument(String pid, String dsName, ControlGroup controlGroup, String label, String mimetype, Document doc, String filename) throws FedoraException {
        if (this.readOnly) {
            throw new FedoraIllegalAccessException("This FedoraClient is READ ONLY!");
        }
        try {
            DOMSource source = new DOMSource(doc);
            StringWriter sWriter = new StringWriter();
            StreamResult sResult = new StreamResult(sWriter);
            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer t = tFactory.newTransformer();
            t.setOutputProperty(OutputKeys.ENCODING, "utf-8");
            t.setOutputProperty(OutputKeys.METHOD, "xml");
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            t.transform(source, sResult);
            sResult.getWriter().flush();
            sWriter.close();
            byte[] bytes = sWriter.getBuffer().toString().getBytes("UTF-8"); 
            
            String md5hash = "";
            long start = System.currentTimeMillis();
            md5hash = XMLComparisonUtil.computeHash(bytes).toLowerCase();
            long end = System.currentTimeMillis();
            LOGGER.debug(this.fedoraBaseUrl + ": Computed checksum for new " + dsName + " datastream in " + (end - start) + "ms (MD5=" + md5hash + ")");
            
            String remoteMD5hash = hasDatastream(pid, dsName) ? getMD5Checksum(pid, dsName) : "null";
            if (md5hash.equals(remoteMD5hash)) {
                LOGGER.info(this.fedoraBaseUrl + ": Datastream " + dsName + " was unaltered.");
            } else {
                LOGGER.info(md5hash + " != " + remoteMD5hash);
                if (this.hasDatastream(pid, dsName)) {
                    // replace current copy
                    String url = this.fedoraBaseUrl + "/objects/" + pid + "/datastreams/" + dsName + "?versionable=true" + (label != null ? "&dsLabel=" + URLEncoder.encode(truncateLabel(label), "UTF-8") : "") + "&dsState=A&mimeType=" + mimetype + "&checksumType=MD5";
                    PostMethod filePost = new PostMethod(url);
                    Part[] parts = {
                            new FilePart(filename, new ByteArrayPartSource(filename, bytes))
                    };
                    filePost.setRequestEntity(
                            new MultipartRequestEntity(parts, filePost.getParams())
                        );
                    try {
                        client.executeMethod(filePost);
                        int status = filePost.getStatusCode();
                        if (status != HttpStatus.SC_CREATED && status != HttpStatus.SC_OK) {
                            throw new RuntimeException("REST action \"" + url + "\" failed: " + filePost.getStatusLine());
                        }
                        LOGGER.info(this.fedoraBaseUrl + ": Replaced datastream " + dsName + " on " + pid + ".");
                    } finally {
                        filePost.releaseConnection();
                    }
                } else {
                    // create a new copy
                    String url = this.fedoraBaseUrl + "/objects/" + pid + "/datastreams/" + dsName + "?controlGroup=" + controlGroup + (label != null ? "&dsLabel=" + URLEncoder.encode(truncateLabel(label), "UTF-8") : "") + "&versionable=true&dsState=A&mimeType=" + mimetype + "&checksumType=MD5";
                    PostMethod filePost = new PostMethod(url);
                    Part[] parts = {
                            new FilePart(filename, new ByteArrayPartSource(filename, bytes))
                        };
                    filePost.setRequestEntity(
                            new MultipartRequestEntity(parts, filePost.getParams())
                        );
                    try {
                        client.executeMethod(filePost);
                        int status = filePost.getStatusCode();
                        if (status != HttpStatus.SC_CREATED) {
                            throw new RuntimeException("REST action \"" + url + "\" failed: " + filePost.getStatusLine());
                        }
                        LOGGER.info(this.fedoraBaseUrl + ": Added datastream " + dsName + " on " + pid + ".");
                    } finally {
                        filePost.releaseConnection();
                    }
                }
            }
        } catch (Exception ex) {
            throw new FedoraException(ex);
        }
    }
    
    public void addRedirectDatastream(String pid, String dsName, String URL) throws FedoraException, IOException {
        if (readOnly) {
            throw new FedoraIllegalAccessException("This FedoraClient is READ ONLY!");
        }
        PostMethod post = new PostMethod(fedoraBaseUrl + "/objects/" + pid + "/datastreams/" + dsName
                + "?controlGroup=" + ControlGroup.R + "&dsLocation=" + URLEncoder.encode(URL, "UTF-8"));
        try {
            client.executeMethod(post);
            if (post.getStatusCode() != 201) {
                throw new FedoraException("Invalid HTTP Status code: " + post.getStatusLine());
            }
        } finally {
            post.releaseConnection();
        }
    }
    
    /**
     * Checks whether the object with the given PID has a datastream
     * with the given identifier (dsName).
     * @param pid the fedora persistent identifier for the object whose
     * datastreams are being queried
     * @param dsName the identifier for the datastream
     * @return true if the datastream exists, false otherwise
     * @throws IOException if an error occurs while accessing fedora
     * @throws FedoraException 
     */
    public boolean hasDatastream(String pid, String dsName) throws IOException, FedoraException {
        GetMethod get = new GetMethod(this.fedoraBaseUrl + "/objects/" + pid + "/datastreams?format=xml");
        try {
            client.executeMethod(get);
            InputStream is = get.getResponseBodyAsStream();
            boolean hasDs = (readStream(is, get.getResponseCharSet()).indexOf("dsid=\"" + dsName + "\"") != -1);
            is.close();
            return hasDs;
        } finally {
            get.releaseConnection();
        }
    }
    
    public void purgeDatastream(String pid, String dsName) throws FedoraException, IOException {
        if (readOnly) {
            throw new FedoraIllegalAccessException("This FedoraClient is READ ONLY!");
        }
        DeleteMethod delete = new DeleteMethod(this.fedoraBaseUrl + "/objects/" + pid + "/datastreams/" + dsName);
        try {
            client.executeMethod(delete);
            if (delete.getStatusCode() > 299 || delete.getStatusCode() < 200) {
                throw new FedoraException("Invalid HTTP Status code: " + delete.getStatusLine());
            }
        } finally {
            delete.releaseConnection();
        }
    }

    /**
     * A helper method to get the checksum for a given datastream.  If no checksum
     * is stored null is returned. 
     */
    public String getMD5Checksum(String pid, String dsName) throws FedoraException, IOException {
        try {
            return getDatastreamProperty(pid, dsName, DatastreamProfile.DatastreamProperty.DS_CHECKSUM);
        } catch (XPathExpressionException ex) {
            throw new FedoraException(ex);
        } catch (SAXException ex) {
            throw new FedoraException(ex);
        } catch (ParserConfigurationException ex) {
            throw new FedoraException(ex);
        }
    }
    

    /**
     * Gets a list of the datastreams for the object with the given
     * pid.
     * @param pid the fedora persistent identifier for the object whose
     * datastreams are being queried
     * @return a list containing all datastreams for the object
     * @throws FedoraException
     * @throws IOException
     */
    public List<String> listDatastreams(String pid) throws FedoraException, IOException {
        GetMethod get = new GetMethod(this.fedoraBaseUrl + "/objects/" + pid + "/datastreams?format=xml");
        try {
            client.executeMethod(get);
            try {
                InputStream is = get.getResponseBodyAsStream();
                Document dsDoc = null;
                try {
                    DocumentBuilder parser = getDocumentBuilder();
                    synchronized (parser) {
                        dsDoc = parser.parse(is);
                    }
                } finally {
                    is.close();
                }
                NodeList elements = dsDoc.getDocumentElement().getChildNodes();
                List<String> dsNames = new ArrayList<String>(elements.getLength());
                for (int i = 0; i < elements.getLength(); i ++) {
                    if (elements.item(i) instanceof Element) {
                        Element el = (Element) elements.item(i);
                        if (el.getNodeName().equals("datastream")) {
                            dsNames.add(el.getAttribute("dsid"));
                        }
                    }
                }
                return dsNames;
            } catch (SAXException ex) {
                throw new FedoraResponseParsingException(ex);
            } catch (ParserConfigurationException ex) {
                throw new FedoraResponseParsingException(ex);
            }
        } finally {
            get.releaseConnection();
        }
    }
    
    public List<String> listContentModelURIs(String objectPid) throws FedoraException, IOException {
        List<String> uris = new ArrayList<String>();
        try {
            String relsextUrl = fedoraBaseUrl + "/get/" + objectPid + "/RELS-EXT";
            GetMethod get = new GetMethod(relsextUrl);
            try {
                client.executeMethod(get);
                if (get.getStatusCode() == 200) {
                    InputStream response = get.getResponseBodyAsStream();
                    Document doc = null;
                    DocumentBuilder parser = getDocumentBuilder();
                    try {
                        synchronized (parser) {
                            doc = parser.parse(response);
                        }
                    } finally {
                        response.close();
                    }
                    NodeList descriptionNodeList = doc.getDocumentElement().getElementsByTagNameNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "Description");
                    NodeList cmodelNodeList = ((Element) descriptionNodeList.item(0)).getElementsByTagNameNS("info:fedora/fedora-system:def/model#", "hasModel");
                    for (int i = 0; i < cmodelNodeList.getLength(); i ++) {
                        uris.add(((Element) cmodelNodeList.item(i)).getAttributeNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "resource"));
                    }
                } else {
                    // no RELS-EXT, so no relationships
                }
            } finally {
                get.releaseConnection();
            }
        } catch (SAXException ex) {
            throw new FedoraResponseParsingException(ex);
        } catch (ParserConfigurationException ex) {
            throw new FedoraResponseParsingException(ex);
        }
        return uris;
    }
    
    /**
     * Fetches the object profile XML and parse out the property value 
     * for the given property.
     * @param pid the pid of the object to be queried
     * @param dsName the name of the datastream who's property is being
     * queried
     * @param prop the property to query.
     * @return the value of the property
     */
    public String getDatastreamProperty(String pid, String dsName, DatastreamProfile.DatastreamProperty prop) throws HttpException, IOException, SAXException, ParserConfigurationException, FedoraException, XPathExpressionException {
        String url = this.fedoraBaseUrl + "/objects/" + pid + "/datastreams/" + dsName + "?format=xml";
        GetMethod get = new GetMethod(url);
        try {
            client.executeMethod(get);
            if (get.getStatusCode() == 200) {
                InputStream is = get.getResponseBodyAsStream();
                Document xml = null;
                DocumentBuilder parser = getDocumentBuilder();
                try {
                    synchronized (parser) {
                        xml = parser.parse(is);
                    }
                } finally {
                    is.close();
                }
                
                // compatible with fedora 3.4
                String value = (String) this.getXPath().evaluate("/fedora-management:datastreamProfile/fedora-management:" + prop.getPropertyName(), xml, XPathConstants.STRING);
                if (value != null) {
                    return value;
                } else {
                    // compatible with fedora 3.2
                    return (String) this.getXPath().evaluate("/datastreamProfile/" + prop.getPropertyName(), xml, XPathConstants.STRING);
                }
    
            } else {
                throw new FedoraException("REST action \"" + url + "\" failed: " + get.getStatusLine());
            }
        } finally {
            get.releaseConnection();
        }
    }
    
    public List<DatastreamProfile> getDatastreamHistory(String pid, String dsId) throws HttpException, IOException, FedoraException, SAXException, ParserConfigurationException, XPathExpressionException {
        String url = this.fedoraBaseUrl + "/objects/" + pid + "/datastreams/" + dsId + "/history?format=xml";
        GetMethod get = new GetMethod(url);
        try {
            client.executeMethod(get);
            if (get.getStatusCode() == 200) {
                List<DatastreamProfile> history = new ArrayList<DatastreamProfile>();
                InputStream is = get.getResponseBodyAsStream();
                Document xml = null;
                DocumentBuilder parser = getDocumentBuilder();
                try {
                    synchronized (parser) {
                        xml = parser.parse(is);
                    }
                } finally {
                    is.close();
                }
                NodeList nl = (NodeList) this.getXPath().evaluate("/fedora-management:datastreamHistory/fedora-management:datastreamProfile", xml, XPathConstants.NODESET);
                for (int i = 0; i < nl.getLength(); i ++) {
                    Element el = (Element) nl.item(i);
                    history.add(new DatastreamProfile(this, el));
                }
                return history;
            } else {
                throw new FedoraException("REST action \"" + url + "\" failed: " + get.getStatusLine());
            }
        } finally {
            get.releaseConnection();
        }
    }
    
    /**
     * <p>
     *   Adds or replaces the given datastream with data stored in a file.  The
     *   current implementation does not take advantage of fedora 3.4 features that
     *   allows ingest of "file" urls, but instead uses the old uploader method.
     * </p>
     * <p>
     *   If the datastream has the same checksum as the file, this method does 
     *   nothing and returns false, whether or not any of the other parameters
     *   differ from the ingested version.
     * </p>
     * @param pid indicates the object whose datastream is to be replaced/added
     * @param dsName the name of the datastream to be modified/added
     * @param controlGroup the control group for the datastream (only used when
     *        modified)
     * @param label the label for the datastream
     * @param mimetype the mime type for the datastream
     * @param file the file to store to fedora
     */
    public void addOrReplaceDatastreamWithFile(String pid, String dsName, ControlGroup controlGroup, String label, String mimetype, boolean versionable, File file, String message) throws IOException, FedoraException {
        if (this.readOnly) {
            throw new FedoraIllegalAccessException("This FedoraClient is READ ONLY!");
        }
        long start = System.currentTimeMillis();
        String md5hash = getMD5Checksum(file);
        long end = System.currentTimeMillis();
        LOGGER.debug(this.fedoraBaseUrl + ": Computed checksum for new " + dsName + " datastream in " + (end - start) + "ms (MD5=" + md5hash + ")");
        
        String remoteMD5hash = this.hasDatastream(pid, dsName) ? this.getMD5Checksum(pid, dsName) : "null";
        if (md5hash.equalsIgnoreCase(remoteMD5hash)) {
            LOGGER.info(this.fedoraBaseUrl + ": Datastream " + dsName + " was unaltered.");
        } else {
            if (this.hasDatastream(pid, dsName)) {
                // replace current copy
                String url = this.fedoraBaseUrl + "/objects/" + pid + "/datastreams/" + dsName + "?controlGroup=" + controlGroup + "&dsLabel=" + URLEncoder.encode(truncateLabel(label), "UTF-8") + "&versionable=" + (versionable ? "true" : "false") + "&dsState=A&mimeType=" + mimetype + "&checksumType=MD5" + (message != null ? "&logMessage=" + URLEncoder.encode(message, "UTF-8") : "");
                PostMethod filePost = new PostMethod(url);
                Part[] parts = {
                        new FilePart(file.getName(), file)
                };
                filePost.setRequestEntity(
                        new MultipartRequestEntity(parts, filePost.getParams())
                    );
                try {
                    client.executeMethod(filePost);
                    int status = filePost.getStatusCode();
                    if (status != HttpStatus.SC_OK && status != HttpStatus.SC_CREATED) {
                        throw new RuntimeException("REST action \"" + url + "\" failed: " + filePost.getStatusLine());
                    }
                    LOGGER.info(this.fedoraBaseUrl + ": Replaced datastream " + dsName + " on " + pid + ".");
                } finally {
                    filePost.releaseConnection();
                }
            } else {
                // create a new copy
                String url = this.fedoraBaseUrl + "/objects/" + pid + "/datastreams/" + dsName + "?controlGroup=" + controlGroup + "&dsLabel=" + URLEncoder.encode(truncateLabel(label), "UTF-8") + "&versionable=" + (versionable ? "true" : "false") + "&dsState=A&mimeType=" + mimetype + "&checksumType=MD5" + (message != null ? "&logMessage=" + URLEncoder.encode(message, "UTF-8") : "");
                PostMethod filePost = new PostMethod(url);
                Part[] parts = {
                        new FilePart(file.getName(), file)
                    };
                filePost.setRequestEntity(
                        new MultipartRequestEntity(parts, filePost.getParams())
                    );
                try {
                    client.executeMethod(filePost);
                    int status = filePost.getStatusCode();
                    if (status != HttpStatus.SC_CREATED && status != HttpStatus.SC_OK) {
                        throw new RuntimeException("REST action \"" + url + "\" failed: " + filePost.getStatusLine());
                    }
                    LOGGER.info(this.fedoraBaseUrl + ": Added datastream " + dsName + " on " + pid + ".");
                } finally {
                    filePost.releaseConnection();
                }
            }
        }
    }
    
    /**
     * Uses the Resource Index search to find the object with the given
     * PURL as its identifier.  This method returns null if there is 
     * not exactly one item with the given PURL.  Be aware that for
     * repositories that are configured for asynchronous resource index
     * update this method may return null when an object with the given 
     * PURL actually exists and has been recently ingested.  Furthermore
     * this method fails when the resource index has not been enabled.
     */
    public String getPidForPURL(String PURL) throws FedoraException, IOException {
        String riSearchUrl = this.fedoraBaseUrl + (this.fedoraBaseUrl.endsWith("/") ? "" : "/") + "risearch?type=tuples&lang=itql&format=Sparql&query=select%20%24member%20from%20%3C%23ri%3E%20where%20%24member%20%3Cdc%3Aidentifier%3E%20'" + URLEncoder.encode(PURL, "UTF-8")+ "'";
        try {
            Document doc = parseUrlAsXmlDocument(riSearchUrl);
            NodeList children = (NodeList) this.getXPath().evaluate("//sparql:sparql/sparql:results/sparql:result/sparql:member", doc, XPathConstants.NODESET);
            if (children.getLength() == 0) {
                LOGGER.warn("No PID found for PURL: " + PURL);
                return null;
            } else if (children.getLength() == 1) {
                String pid = ((Element) children.item(0)).getAttribute("uri").replace("info:fedora/", "");
                LOGGER.debug("PID, \"" + pid + "\", found for PURL: " + PURL);
                return pid;
            } else {
                LOGGER.error(children.getLength() + " PIDs found for PURL: " + PURL);
                return null;
            }
        } catch (SAXException ex) {
            throw new FedoraResponseParsingException(ex);
        } catch (ParserConfigurationException ex) {
            throw new FedoraResponseParsingException(ex);
        } catch (XPathExpressionException ex) {
            throw new FedoraResponseParsingException(ex);
        }
    }
    
    /**
     * Creates a new object in the repository (not from an existing FOXML file).  Any
     * of the following parameters may be null.
     * @param pid the pid of the new object (or null to use an auto-generated pid).
     * This method will throw an exception if a pid is specified that already exists
     * in the repository.
     * @param label the label of the object
     * @param namespace the namespace for the created pid
     * @param ownerId the ownerId for the newly created object
     * @param logMessage a log message
     * @return the pid of the newly created object
     */
    public String createObject(String pid, String label, String namespace, String ownerId, String logMessage) throws FedoraIllegalAccessException, HttpException, IOException {
        if (this.readOnly) {
            throw new FedoraIllegalAccessException("This FedoraClient is READ ONLY!");
        }
        StringBuffer query = new StringBuffer();
        if (label != null) {
            query.append(query.length() == 0 ? "?" : "&");
            query.append("label=" + URLEncoder.encode(truncateLabel(label), "UTF-8"));
        }
        if (namespace != null) {
            query.append(query.length() == 0 ? "?" : "&");
            query.append("namespace=" + URLEncoder.encode(namespace, "UTF-8"));
        }
        if (ownerId != null) {
            query.append(query.length() == 0 ? "?" : "&");
            query.append("ownerId=" + URLEncoder.encode(ownerId, "UTF-8"));
        }
        if (logMessage != null) {
            query.append(query.length() == 0 ? "?" : "&");
            query.append("logMessage=" + URLEncoder.encode(logMessage, "UTF-8"));
        }
        String url = this.fedoraBaseUrl + "/objects/" + (pid == null ? "new" : pid) + query.toString();
        PostMethod method = new PostMethod(url);
        try {
            client.executeMethod(method);
            int statusCode = method.getStatusCode();
            if (statusCode != HttpStatus.SC_CREATED) {
                throw new RuntimeException("REST action \"" + url + "\" failed: " + method.getStatusLine());
            } else {
                pid = method.getResponseBodyAsString(1024);
                LOGGER.info(this.fedoraBaseUrl + ": Created object " + pid);
                return pid;
            }
        } finally {
            method.releaseConnection();
        }
    }
    
    public String createNewEmptyFedoraObject(Item item, String newFileItemId, String relationshipUriToItem) throws FedoraException {
        try {
            String imPid = getPidForPURL(item.getId());
            String collectionPid = getPidForPURL(item.getCollectionId());
            if (collectionPid == null) {
                throw new RuntimeException("Collection object not found!");
            }
            
            // locate or create object
            String pid = getPidForPURL(newFileItemId);
            if (pid == null) {
                pid = createObject(pid, null, null, null, null);
                System.out.println("Created object " + pid + ".");
             
                // set the dc identifier
                DCRecord dc = new DCRecord(getXMLDatastreamAsDocument(pid, "DC"));
                dc = dc.addIdentifier(newFileItemId);
                File tempFile = File.createTempFile("dublin-core-", ".xml");
                dc.writeOut(tempFile);
                addOrReplaceDatastreamWithFile(pid, "DC", ControlGroup.X, null, "text/xml", true, tempFile, null);
                tempFile.delete();
                
                // add collection membership
                addRelationship(pid, toUriString(pid), IS_COLLECTION_MEMBER, toUriString(collectionPid));
            } else {
                throw new IllegalStateException("An item with id " + newFileItemId + " already exists!");
            }
            
            // add relationship
            addRelationship(pid, "info:fedora/" + pid, relationshipUriToItem, "info:fedora/" + imPid);
            
            return pid;
        } catch (Throwable t) {
            throw new FedoraException(t);
        }
    }
    
    public String createNewItemMetadataFedoraObject(ItemMetadata im, String imCMURI) throws FedoraException {
    	try {
	    	String collectionPid = getPidForPURL(im.getCollectionId());
	    	if (collectionPid == null) {
	    		throw new RuntimeException("Collection object not found!");
	    	}
	    	// locate or create object
	        String pid = getPidForPURL(im.getId());
	        if (pid == null) {
	            pid = createObject(pid, null, null, null, null);
	            System.out.println("Created object " + pid + ".");
	         
	            // set the dc identifier
	            DCRecord dc = new DCRecord(getXMLDatastreamAsDocument(pid, "DC"));
	            dc = dc.addIdentifier(im.getId());
	            File tempFile = File.createTempFile("dublin-core-", ".xml");
	            dc.writeOut(tempFile);
	            addOrReplaceDatastreamWithFile(pid, "DC", ControlGroup.X, null, "text/xml", true, tempFile, null);
	            tempFile.delete();
	            
	            // add collection membership
	            addRelationship(pid, toUriString(pid), IS_COLLECTION_MEMBER, toUriString(collectionPid));
	        } else {
	        	throw new IllegalStateException("An item with id " + im.getId() + " already exists!");
	        }
	        
	        // add the content model
	        addRelationship(pid, toUriString(pid), HAS_MODEL, imCMURI);
	        
	        // write the ItemMetadata to a temp file (I know this is inefficient)
	        File imFile = File.createTempFile("item-metadata", ".xml");
	        imFile.deleteOnExit();
	        FileOutputStream fos = new FileOutputStream(imFile);
	        im.writeOutXML(fos);
	        fos.close();
	        
	        addOrReplaceDatastreamWithFile(pid, "ITEM-METADATA", ControlGroup.M, null, "text/xml", true, imFile, null);
	        return pid;
    	} catch (Throwable t) {
    		throw new FedoraException(t);
    	}
    }
    
    public String createNewCollectionFedoraObject(CollectionConfigurationData c, String parentId, String ccCMURI, String ccDSID, String parentRelationship) throws FedoraException {
        boolean created = false;
        try {
            String parentPid = null;
            if (parentId != null) {
                parentPid = getPidForPURL(parentId);
                if (parentPid == null) {
                    throw new RuntimeException("Parent object not found!");
                }
            }
            // locate or create object
            String pid = getPidForPURL(c.getId());
            if (pid == null) {
                pid = createObject(pid, c.getId(), null, null, "Created by photocat.");
                created = true;
                System.out.println("Created object " + pid + ".");
             
                // set the dc identifier
                DCRecord dc = new DCRecord(getXMLDatastreamAsDocument(pid, "DC"));
                dc = dc.addIdentifier(c.getId());
                File tempFile = File.createTempFile("dublin-core-", ".xml");
                dc.writeOut(tempFile);
                addOrReplaceDatastreamWithFile(pid, "DC", ControlGroup.X, null, "text/xml", true, tempFile, null);
                tempFile.delete();
                
                if (parentPid != null) {
                    // add relationship to parent
                    addRelationship(pid, toUriString(pid), parentRelationship, toUriString(parentPid));
                }
            } else {
                throw new IllegalStateException("An item with id " + c.getId() + " already exists!");
            }
            
            // add the content model
            addRelationship(pid, toUriString(pid), HAS_MODEL, ccCMURI);
            
            // add another just for ease of management (IUDLP specific)
            addRelationship(pid, toUriString(pid), HAS_MODEL, "info:fedora/cmodel:collection"); 
            
            // add the collection XML
            addOrReplaceDatastreamWithDocument(pid, ccDSID, ControlGroup.M, null, "text/xml", c.generateDocument(), ccDSID + ".xml");
            return pid;
        } catch (Throwable t) {
            if (created) {
                // roll back and delete the object
                try {
                    String pid = getPidForPURL(c.getId());
                    if (pid != null) {
                        purgeObject(pid);
                    }
                } catch (Throwable t2) {
                    throw new FedoraException(t2);
                }
            }
            throw new FedoraException(t);
        }
    }
    
    public String toUriString(String pidOrObject) {
        if (pidOrObject.startsWith("info:fedora/")) {
            return pidOrObject;
        } else {
            return "info:fedora/" + pidOrObject;
        }
    }
    
    /**
     * Queries the Resource Index to get a list of Fedora
     * Object PIDs for objects that are related to the object
     * whose pid is provided with the given relationship type.
     * @param pid the id of the object whose relationships are
     * being queried
     * @param relationshipType the RDF relationship between the
     * the object whose pid is provided and all of the objects
     * whose pids are returned
     * @return a list of PIDs
     * @throws FedoraException if an error occurs with fedora or in
     * parsing the fedora response
     * @throws IOException if an error occurs fetching data from
     * fedora
     */
    public List<String> getRelatedPids(String pid, String relationshipType) throws FedoraException, IOException {
        List<String> pids = new ArrayList<String>();
        String riSearchUrl = this.fedoraBaseUrl + (this.fedoraBaseUrl.endsWith("/") ? "" : "/") + "risearch?type=tuples&lang=itql&format=Sparql&query=select%20%24child%20from%20%3C%23ri%3E%20%0Awhere%20%24child%20%3C" + URLEncoder.encode(relationshipType, "UTF-8")+ "%3E%20%3Cinfo%3Afedora/" + URLEncoder.encode(pid, "UTF-8") + "%3E";
        //System.out.println(riSearchUrl);
        try {
            Document doc = parseUrlAsXmlDocument(riSearchUrl);
            NodeList children = doc.getDocumentElement().getElementsByTagName("child");
            for (int i = 0; i < children.getLength(); i ++) {
                pids.add(((Element) children.item(i)).getAttribute("uri").replace("info:fedora/", ""));
            }
        } catch (SAXException ex) {
            throw new FedoraResponseParsingException(ex);
        } catch (ParserConfigurationException ex) {
            throw new FedoraResponseParsingException(ex);
        }
        return pids;
    }
    
    public List<String> getPidsRelatedToThisPidWithRelationship(String pid, String relationshipType) throws FedoraException, IOException {
        List<String> pids = new ArrayList<String>();
        String riSearchUrl = this.fedoraBaseUrl + (this.fedoraBaseUrl.endsWith("/") ? "" : "/") + "risearch?type=tuples&lang=itql&format=Sparql&query=select%20%24parent%20from%20%3C%23ri%3E%20%0Awhere%20%3Cinfo%3Afedora/" + URLEncoder.encode(pid, "UTF-8") + "%3E%20%3C" + URLEncoder.encode(relationshipType, "UTF-8")+ "%3E%20%24parent";
        //System.out.println(riSearchUrl);
        try {
            Document doc = parseUrlAsXmlDocument(riSearchUrl);
            NodeList children = doc.getDocumentElement().getElementsByTagName("parent");
            for (int i = 0; i < children.getLength(); i ++) {
                pids.add(((Element) children.item(i)).getAttribute("uri").replace("info:fedora/", ""));
            }
        } catch (SAXException ex) {
            throw new FedoraResponseParsingException(ex);
        } catch (ParserConfigurationException ex) {
            throw new FedoraResponseParsingException(ex);
        }
        return pids;
    }
    
    /**
     * Queries the Resource Index to get a list of Fedora
     * Object PIDs for objects that are related to the object
     * whose pid is provided with the given relationship type
     * with the option to exclude those but which have the 
     * "DELETED" state.
     * @param pid the id of the object whose relationships are
     * being queried
     * @param relationshipType the RDF relationship between the
     * the object whose pid is provided and all of the objects
     * whose pids are returned
     * @param excludeDeleted if true, excludes deleted items
     * from the results.
     * @return a list of PIDs
     * @throws FedoraException if an error occurs with fedora or in
     * parsing the fedora response
     * @throws IOException if an error occurs fetching data from
     * fedora
     */
    public List<String> getRelatedPids(String pid, String relationshipType, boolean excludeDeleted) throws FedoraException, IOException {
        if (!excludeDeleted) {
            return getRelatedPids(pid, relationshipType);
        } else {
            List<String> pids = new ArrayList<String>();
            String riSearchUrl = this.fedoraBaseUrl + (this.fedoraBaseUrl.endsWith("/") ? "" : "/") + "risearch?type=tuples&lang=itql&format=Sparql&query=select%20%24child%20from%20%3C%23ri%3E%20%0Awhere%20%24child%20%3C" + URLEncoder.encode(relationshipType, "UTF-8")+ "%3E%20%3Cinfo%3Afedora/" + URLEncoder.encode(pid, "UTF-8") + "%3Eminus%20%24child%0A%20%20%20%20%20%20%20%20%3Cfedora-model%3Astate%3E%0A%20%20%20%20%20%20%20%20%3Cinfo%3Afedora%2Ffedora-system%3Adef%2Fmodel%23Deleted%3E";
            try {
                Document doc = parseUrlAsXmlDocument(riSearchUrl);
                NodeList children = doc.getDocumentElement().getElementsByTagName("child");
                for (int i = 0; i < children.getLength(); i ++) {
                    pids.add(((Element) children.item(i)).getAttribute("uri").replace("info:fedora/", ""));
                }
            } catch (SAXException ex) {
                throw new FedoraResponseParsingException(ex);
            } catch (ParserConfigurationException ex) {
                throw new FedoraResponseParsingException(ex);        }
            return pids;
        }
    }
    
    /**
     * Exposes the addRelationship REST API method.
     * @param pid persistent identifier of the digital object
     * @param subject URI of this object
     * @param predicate predicate of the relationship
     * @param object object of the relationship
     * @param isLiteral true if the object of the relationship is a literal, false if it is a URI
     * @param datatype if the object is a literal, the datatype of the literal (optional)
     * @throws FedoraException 
     */
    public void addRelationship(String pid, String subject, String predicate, String object) throws FedoraException, IOException {
        if (this.readOnly) {
            throw new FedoraIllegalAccessException("This FedoraClient is READ ONLY!");
        }
        PostMethod post = new PostMethod(this.fedoraBaseUrl + "/objects/" + pid + "/relationships/new"
                + "?subject=" + URLEncoder.encode(subject, "UTF-8")
                + "&predicate=" + URLEncoder.encode(predicate, "UTF-8")
                + "&object=" + URLEncoder.encode(object, "UTF-8")
                + "&isLiteral=" + URLEncoder.encode(String.valueOf(false), "UTF-8"));
        try {
            client.executeMethod(post);
            if (post.getStatusCode() != 200) {
                throw new FedoraException("Invalid HTTP Status code: " + post.getStatusLine());
            }
        } finally {
            post.releaseConnection();
        }
    }
    
    /**
     * Exposes the purgeRelationship REST API method.
     * @param pid persistent identifier of the digital object
     * @param subject URI of this object
     * @param predicate predicate of the relationship
     * @param object object of the relationship
     * @param isLiteral true if the object of the relationship is a literal, false if it is a URI
     * @param datatype if the object is a literal, the datatype of the literal (optional)
     * @throws FedoraException 
     */
    public void purgeRelationship(String pid, String subject, String predicate, String object) throws FedoraException, IOException {
        if (readOnly) {
            throw new FedoraIllegalAccessException("This FedoraClient is READ ONLY!");
        }
        DeleteMethod delete = new DeleteMethod(this.fedoraBaseUrl + "/objects/" + pid + "/relationships"
                + "?subject=" + URLEncoder.encode(subject, "UTF-8")
                + "&predicate=" + URLEncoder.encode(predicate, "UTF-8")
                + "&object=" + URLEncoder.encode(object, "UTF-8")
                + "&isLiteral=" + URLEncoder.encode(String.valueOf(false), "UTF-8"));
        try {
            client.executeMethod(delete);
            if (delete.getStatusCode() != 200) {
                throw new FedoraException("Invalid HTTP Status code: " + delete.getStatusLine());
            }
        } finally {
            delete.releaseConnection();
        }
    }
    
    public void purgeObject(String pid) throws IOException, FedoraException {
        if (readOnly) {
            throw new FedoraIllegalAccessException("This FedoraClient is READ ONLY!");
        }
        DeleteMethod delete = new DeleteMethod(this.fedoraBaseUrl + "/objects/" + pid);
        try {
            client.executeMethod(delete);
            if (delete.getStatusCode() != 204 && delete.getStatusCode() != 200) {
                throw new FedoraException("Invalid HTTP Status code: " + delete.getStatusLine());
            }
        } finally {
            delete.releaseConnection();
        }
    }
    
    /**
     * Fetches the object profile XML.
     * @param pid the pid of the object to be queried
     * @return an encapsulated version of the object profile
     */
    public ObjectProfile getObjectProfile(String pid) throws SAXException, IOException, ParserConfigurationException, FedoraException {
        String url = fedoraBaseUrl + "/objects/" + pid + "?format=xml";
        GetMethod get = new GetMethod(url);
        try {
            client.executeMethod(get);
            if (get.getStatusCode() == 200) {
                InputStream response = get.getResponseBodyAsStream();
                Document xml = null;
                DocumentBuilder parser = getDocumentBuilder();
                synchronized (parser) {
                    xml = parser.parse(response);
                }
                response.close();
                return new ObjectProfile(xml, getXPath());
            } else {
                throw new FedoraException("REST action \"" + url + "\" failed: " + get.getStatusLine());
            }
        } finally {
            get.releaseConnection();
        }
    }
        
    /**
     * This method is implemented just to bypass the warning log message 
     * written by the underlying HttpClient when you try to get a response
     * body as a string.  The warning is reasonable if the result is of
     * an unexpected length, but for calls when we know the length of the
     * response will be short, we can use this method to avoid the log 
     * message.
     * @param is the InputStream to be read into a String
     * @param charsetName the name of the characterset in which the InputStream
     * is encoded.
     * @return a string containing the data from the inputstream using the
     * given charset
     * @throws IOException if the stream cannot be read
     * @throws UnsupportedEncodingException if the given encoding isn't supported
     */
    private static String readStream(InputStream is, String charsetName) throws UnsupportedEncodingException, IOException {
        StringBuffer sb = new StringBuffer();
        byte[] buffer = new byte[1024];
        int length = 0;
        while ((length = is.read(buffer)) != -1) {
            sb.append(new String(buffer, 0, length, charsetName));
        }
        return sb.toString();
    }
    
    private static String truncateLabel(String label) {
        if (label == null || label.trim().length() == 0) {
            label = "";
        }
        if (label.length() > 255) {
            return label.substring(0, 250) + "...";
        } else {
            return label;
        }
    }
    
    public String getMD5Checksum(File file) throws IOException {
		RandomAccessFile raf = new RandomAccessFile(file, "r");
		int BUFFER_SIZE = 65536;
		byte[] buffer = new byte[BUFFER_SIZE];
	    MessageDigest complete = null;
	    try {
	    	complete = MessageDigest.getInstance("MD5");
	    } catch (Exception err) {
	    	err.printStackTrace();
	    	throw new IOException("Error generating MD5");
	    }
	    try {
	    	raf.seek(0);
	    	int numRead;
		    do {
		    	if (raf.getFilePointer() >= (file.length() - BUFFER_SIZE)) {
		    		buffer = new byte[(int)(file.length() - raf.getFilePointer())];
		    	}
		    	numRead = raf.read(buffer);
		    	if (numRead > 0) {
		    		complete.update(buffer, 0, numRead);
		        }
		    } while (numRead > 0);
		    raf.close();
		    byte[] inn = complete.digest();
		    byte ch = 0x00;
		    int i = 0;
		    String pseudo[] = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"};
		    StringBuffer out = new StringBuffer(inn.length * 2);
		    while (i < inn.length) {
		        ch = (byte) (inn[i] & 0xF0);
		        ch = (byte) (ch >>> 4);
		        ch = (byte) (ch & 0x0F);
		        out.append(pseudo[ (int) ch]);
		        ch = (byte) (inn[i] & 0x0F);
		        out.append(pseudo[ (int) ch]);
		        i++;
		    }
		    return new String(out);
	    } catch (IOException err) {
	    	raf.close();
	    	throw err;
	    }
    }
    
    
    public Document parseUrlAsXmlDocument(String url) throws HttpException, IOException, SAXException, ParserConfigurationException {
        GetMethod get = new GetMethod(url);
        try {
            client.executeMethod(get);
            if (get.getStatusCode() != 200) {
                throw new RuntimeException("Error fetching XML document, " + url + ", " + get.getStatusLine());
            }
            InputStream response = get.getResponseBodyAsStream();
            Document doc = null;
            DocumentBuilder parser = getDocumentBuilder();
            try {
                synchronized (parser) {
                    doc = parser.parse(response);
                }
            } finally {
                response.close();
            }
            return doc;
        } catch (RuntimeException ex) {
            LOGGER.error("Error parsing XML URL: " + url);
            throw ex;
        } finally {
            get.releaseConnection();
        }
    }

    public String getDSAccessUrl(String pid, String dsId) {
        return getServerUrl() + "/objects/" + pid + "/datastreams/" + dsId + "/content";
    }
    
}
