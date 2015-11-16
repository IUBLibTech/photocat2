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
package edu.indiana.dlib.fedora.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
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
import javax.xml.xpath.XPathFactory;

import melcoe.fedora.util.DataUtils;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.FileRequestEntity;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.multipart.ByteArrayPartSource;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import edu.indiana.dlib.fedora.utils.XMLComparisonUtil;
import edu.indiana.dlib.robusta.file.SmartFile;


/**
 * <p>
 *   A simple client wrapping useful functionality associated with 
 *   a fedora repository.  This class was written using the REST API
 *   and targeted for Fedora 3.2 and Fedora 3.4 simultaneously.  This
 *   means that some methods have two implementations.  Some methods
 *   require certain features to be enabled on the fedora repository
 *   in order to work properly and almost all methods require 
 *   credentials with access to perform the underlying operation.
 * </p>
 */
public class FedoraClient {
    
    public Logger logger;
    
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
     * Also, any call to "parse() or other methods should synchornize
     * on the the documentBuilder object because it may not be thread-safe.
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
     * Instantiates an unauthenticated FedoraClient.
     * @param fedoraHost the hostname of the fedora server
     * @param fedoraContextName the fedora context name 
     * (likely "fedora")
     * @param port fedora's port
     */
    public FedoraClient(String fedoraHost, String fedoraContextName, int port, boolean readOnly) {
        this(null, null, fedoraHost, fedoraContextName, port, readOnly);
    }
    
    /**
     * Instantiates a potentially authenticated FedoraClient.
     * @param username the username (or null for anonymous access)
     * @param password the password (or null for anonymous access)
     * @param fedoraHost the hostname of the fedora server
     * @param fedoraContextName the fedora context name 
     * (likely "fedora")
     * @param port fedora's port
     */
    public FedoraClient(String username, String password, String fedoraHost, String fedoraContextName, int fedoraPort, boolean readOnly) {
        logger = Logger.getLogger(FedoraClient.class);
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
            this.client.getState().setCredentials(new AuthScope(fedoraHost, fedoraPort), credentials);
        }

        this.readOnly = readOnly;
    }
    
    public MultiThreadedHttpConnectionManager getConnectionManager() {
        return (MultiThreadedHttpConnectionManager) client.getHttpConnectionManager();
    }
    
    
    /**
     * Gets or creates a DocumentBuilder.
     */
    protected DocumentBuilder getDocumentBuilder() throws ParserConfigurationException {
        if (this.documentBuilder != null) {
            return this.documentBuilder;
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
     * Gets or creates and XPath configured with namespaces appropriate
     * for all other internal methods.
     */
    protected XPath getXPath() {
        if (this.xpath != null) {
            return this.xpath;
        } else {
            this.xpath = XPathFactory.newInstance().newXPath();
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
     * Gets the MapNamespaceContext associated with the XPath.
     */
    public MapNamespaceContext getMapNamespaceContext() {
        return (MapNamespaceContext) this.getXPath().getNamespaceContext();
    }
    
    /**
     * Gets the fedora server URL being used by this client instance.
     */
    public String getServerUrl() {
        return this.fedoraBaseUrl;
    }

    /**
     * Gets access InputStream access to a given XML disseminator.
     * @param pid identifies the object whose disseminator is to be retrieved
     * @param servicePid the name of the pid for the service definition
     */
    public Document getXMLDissemintationAsDocument(String pid, String servicePid, String function) throws IOException, FedoraException {
        String url = fedoraBaseUrl + "/objects/" + pid + "/methods/" + servicePid + "/" + function;
        GetMethod get = new GetMethod(url);
        try {
            int status = client.executeMethod(get);
            if (status != HttpStatus.SC_OK) {
                throw new RuntimeException("REST action \"" + url + "\" failed: " + get.getStatusLine());
            }
            InputStream response = get.getResponseBodyAsStream();
            try {
                DocumentBuilder parser = getDocumentBuilder();
                synchronized (parser) {
                    Document doc = parser.parse(response);
                    return doc;
                }
            } catch (SAXException ex) {
                throw new FedoraException(ex);
            } catch (ParserConfigurationException ex) {
                throw new FedoraException(ex);
            } finally {
                response.close();
            }
        } finally {
            get.releaseConnection();
        }
    }
    
    /**
     * Gets access InputStream access to a given datastream.
     * The current implementation uses the new REST API.
     * @param pid identifies the object whose datastream is to be retrieved
     * @param dsName the name of the datastream
     * @return an InputStream to access the resource
     * @throws IOException if an error occurs
     */
    public void pipeDatastream(String pid, String dsName, String asOfDateTime, OutputStream os) throws IOException {
        String url = this.fedoraBaseUrl + "/objects/" + pid + "/datastreams/" + dsName + "/content" + (asOfDateTime != null ? "?asOfDateTime=" + URLEncoder.encode(asOfDateTime, "UTF-8") : "");
        GetMethod get = new GetMethod(url);
        try {
            client.executeMethod(get);
            int status = get.getStatusCode();
            if (status != HttpStatus.SC_CREATED && status != HttpStatus.SC_OK) {
                throw new RuntimeException("REST action \"" + url + "\" failed: " + get.getStatusLine());
            }
            ReadableByteChannel inputChannel = Channels.newChannel(get.getResponseBodyAsStream());  
            WritableByteChannel outputChannel = Channels.newChannel(os);
            ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);  
            while (inputChannel.read(buffer) != -1) {  
                buffer.flip();  
                outputChannel.write(buffer);  
                buffer.compact();  
            }  
            buffer.flip();  
            while (buffer.hasRemaining()) {  
                outputChannel.write(buffer);  
            }  
            inputChannel.close();  
            outputChannel.close();
        } finally {
            get.releaseConnection();
        }
    }
    
    /**
     * Reverts a datastream to the previous version.  This method requires
     * that the FedoraClient not be ReadOnly, that the given datastream has
     * more than one version and that the required credentials allow access
     * to export objects, access old versions and modify datastreams.  This
     * method should be used with EXTREME CAUTION as there are no checks to
     * verify that it is doing what is expected.
     */
    public void revertDatastream(String pid, String dsName) throws IOException, FedoraException {
        if (this.readOnly) {
            throw new FedoraIllegalAccessException("This FedoraClient is READ ONLY!");
        }
        
        try {
            // export the foxml
            Document foxml  = exportObjectAsDocument(pid);
            
            NodeList nl = (NodeList) this.getXPath().evaluate("/foxml:digitalObject/foxml:datastream[@ID='" + dsName + "']/foxml:datastreamVersion", foxml, XPathConstants.NODESET);
            if (nl != null) {
                if (nl.getLength() > 1) {
                    Element datastreamVersionEl = (Element) nl.item(nl.getLength() - 2);
                    String url = (String) this.getXPath().evaluate("foxml:contentLocation/@REF", datastreamVersionEl, XPathConstants.STRING);
                    String digest = (String) this.getXPath().evaluate("foxml:contentDigest[@TYPE='MD5']/@DIGEST", datastreamVersionEl, XPathConstants.STRING);
                    String mimeType = (String) this.getXPath().evaluate("@MIMETYPE", datastreamVersionEl, XPathConstants.STRING);
                    logger.debug("Fetching last version of " + dsName + " on " + pid + " at " + url);
                    
                    GetMethod get = new GetMethod(url);
                    try {
                        client.executeMethod(get);
                        InputStream is = get.getResponseBodyAsStream();
                        
                        // replace the datastream
                        String restUrl = this.fedoraBaseUrl + "/objects/" + pid + "/datastreams/" + dsName + "?versionable=true&dsState=A&mimeType=" + mimeType + "&checksumType=MD5&checksum=" + URLEncoder.encode(digest, "UTF-8");
                        PostMethod filePost = new PostMethod(restUrl);
                        Part[] parts = {
                            new FilePart("mets.xml",  new ByteArrayPartSource("mets.xml", readStream(is, "UTF-8").getBytes("UTF-8")))
                        };
                        filePost.setRequestEntity(
                            new MultipartRequestEntity(parts, filePost.getParams())
                            );
                        try {
                            int status = client.executeMethod(filePost);
                            if (status != HttpStatus.SC_OK) {
                                throw new RuntimeException("REST action \"" + restUrl + "\" failed: " + filePost.getStatusLine());
                            }
                        } finally {
                            filePost.releaseConnection();
                        }
                        is.close();
                        logger.info(this.fedoraBaseUrl + ": Replaced datastream " + dsName + " on " + pid + " with the previous version.");
                    } finally {
                        get.releaseConnection();
                    }
                } else {
                    throw new FedoraException("Only " + nl.getLength() + " version of " + dsName + " on " + pid + "!");
                }
            } else {
                throw new FedoraException("No previous version of " + dsName + " on " + pid + "!");
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
     * Gets a URL at which a given (public) datastream's content may be accessed.
     * 
     * This method was added for version 2.0.2
     */
    public String getDatastreamAccessURL(String pid, String dsId) {
        return this.fedoraBaseUrl + "/objects/" + pid + "/datastreams/" + dsId + "/content";
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
            t.transform(source, sResult);
            sResult.getWriter().flush();
            String string = sWriter.toString();
            sWriter.close();
            byte[] bytes = sWriter.getBuffer().toString().getBytes("UTF-8"); 
            
            String md5hash = "";
            long start = System.currentTimeMillis();
            if (ControlGroup.X.equals(controlGroup)) {
                md5hash = XMLComparisonUtil.computeHash(DataUtils.fedoraXMLHashFormat(bytes)).toLowerCase();
            } else {
                md5hash = XMLComparisonUtil.computeHash(bytes).toLowerCase();
            }
            long end = System.currentTimeMillis();
            logger.debug(this.fedoraBaseUrl + ": Computed checksum for new " + dsName + " datastream in " + (end - start) + "ms (MD5=" + md5hash + ")");
            
            String remoteMD5hash = this.hasDatastream(pid, dsName) ? this.getMD5Checksum(pid, dsName) : "null";
            if (md5hash.equals(remoteMD5hash)) {
                logger.info(this.fedoraBaseUrl + ": Datastream " + dsName + " was unaltered.");
            } else {
                logger.info(md5hash + " != " + remoteMD5hash);
                if (this.hasDatastream(pid, dsName)) {
                    // replace current copy
                    String url = this.fedoraBaseUrl + "/objects/" + pid + "/datastreams/" + dsName + "?versionable=true" + (label != null ? "&dsLabel=" + URLEncoder.encode(truncateLabel(label), "UTF-8") : "") + "&dsState=A&mimeType=" + mimetype + "&checksumType=MD5";
                    PostMethod filePost = new PostMethod(url);
                    try {
                        Part[] parts = {
                                new FilePart(filename, new ByteArrayPartSource(filename, bytes))
                        };
                        filePost.setRequestEntity(
                                new MultipartRequestEntity(parts, filePost.getParams())
                            );
                        int status = client.executeMethod(filePost);
                        if (status != HttpStatus.SC_CREATED && status != HttpStatus.SC_OK) {
                            throw new RuntimeException("REST action \"" + url + "\" failed: " + filePost.getStatusLine());
                        }
                    } finally {
                        filePost.releaseConnection();
                    }
                    logger.info(this.fedoraBaseUrl + ": Replaced datastream " + dsName + " on " + pid + ".");
                } else {
                    // create a new copy
                    String url = this.fedoraBaseUrl + "/objects/" + pid + "/datastreams/" + dsName + "?controlGroup=" + controlGroup + (label != null ? "&dsLabel=" + URLEncoder.encode(truncateLabel(label), "UTF-8") : "") + "&versionable=true&dsState=A&mimeType=" + mimetype + "&checksumType=MD5";
                    PostMethod filePost = new PostMethod(url);
                    try {
                        Part[] parts = {
                                new FilePart(filename, new ByteArrayPartSource(filename, bytes))
                            };
                        filePost.setRequestEntity(
                                new MultipartRequestEntity(parts, filePost.getParams())
                            );
                        int status = client.executeMethod(filePost);
                        if (status != HttpStatus.SC_CREATED) {
                            throw new RuntimeException("REST action \"" + url + "\" failed: " + filePost.getStatusLine());
                        }
                        logger.info(this.fedoraBaseUrl + ": Added datastream " + dsName + " on " + pid + ".");
                    } finally {
                        filePost.releaseConnection();
                    }
                }
            }
        } catch (Exception ex) {
            throw new FedoraException(ex);
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
        String md5hash = new SmartFile(file).getMD5Checksum();
        long end = System.currentTimeMillis();
        logger.debug(this.fedoraBaseUrl + ": Computed checksum for new " + dsName + " datastream in " + (end - start) + "ms (MD5=" + md5hash + ")");
        
        String remoteMD5hash = this.hasDatastream(pid, dsName) ? this.getMD5Checksum(pid, dsName) : "null";
        if (md5hash.equalsIgnoreCase(remoteMD5hash)) {
            logger.info(this.fedoraBaseUrl + ": Datastream " + dsName + " was unaltered.");
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
                    int status = client.executeMethod(filePost);
                    if (status != HttpStatus.SC_OK && status != HttpStatus.SC_CREATED) {
                        throw new RuntimeException("REST action \"" + url + "\" failed: " + filePost.getStatusLine());
                    }
                    logger.info(this.fedoraBaseUrl + ": Replaced datastream " + dsName + " on " + pid + ".");
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
                    int status = client.executeMethod(filePost);
                    if (status != HttpStatus.SC_CREATED && status != HttpStatus.SC_OK) {
                        throw new RuntimeException("REST action \"" + url + "\" failed: " + filePost.getStatusLine());
                    }
                    logger.info(this.fedoraBaseUrl + ": Added datastream " + dsName + " on " + pid + ".");
                } finally {
                    filePost.releaseConnection();
                }
            }
        }
    }

    /**
     * Gets the number of versions of the datastream that are stored 
     * before the version that was written with the given lastModDate.
     * If lastModDate is null, returns the total number of revisions - 1.
     * @param dsId the name of the datastream
     * @param lastModDate the last modification date, or null
     * @throws ParserConfigurationException 
     */
    public int getDatastreamRevisionNumber(String pid, String dsId, String lastModDateStr) throws IOException, FedoraException, ParserConfigurationException {
        String url = this.fedoraBaseUrl + "/objects/" + pid + "/datastreams/" + dsId + "/history?format=xml";
        try {
            GetMethod get = new GetMethod(url);
            try {
                client.executeMethod(get);
                if (get.getStatusCode() == HttpStatus.SC_OK) {
                    InputStream response = get.getResponseBodyAsStream();
                    DocumentBuilder parser = getDocumentBuilder();
                    Document doc = null;
                    synchronized (parser) {
                        doc = parser.parse(response);
                    }
                    response.close();
                    NodeList profiles = (NodeList) this.getXPath().evaluate("fedora-management:datastreamHistory/fedora-management:datastreamProfile", doc, XPathConstants.NODESET);
                    if (lastModDateStr == null) {
                        return profiles.getLength() - 1;
                    } else {
                        Date lastModDate = FEDORA_DATE_FORMAT.parse(lastModDateStr);
                        for (int i = 0; i < profiles.getLength(); i ++) {
                            Element profileEl = (Element) profiles.item(i);
                            String dsCreateDate = (String) this.getXPath().evaluate("fedora-management:dsCreateDate", profileEl, XPathConstants.STRING);
                            Date date = FEDORA_DATE_FORMAT.parse(dsCreateDate);
                            if (!date.after(lastModDate)) {
                                return profiles.getLength() - i - 1;
                            }
                        }
                        throw new FedoraException("There is no revision number associated with the date: " + lastModDateStr);
                    }
                } else {
                    throw new RuntimeException("REST action \"" + url + "\" failed: " + get.getStatusLine());
                }
            } finally {
                get.releaseConnection();
            }
        } catch (SAXException ex) {
            throw new FedoraResponseParsingException(ex);
        } catch (XPathExpressionException ex) {
           throw new FedoraException(ex);
        } catch (ParseException ex) {
            throw new FedoraException(ex);
        }
    }
    
    /**
     * Gets the modification date for the datastream with the given version
     * number.  This version number is not something stored in fedora but
     * instead an index (starting at zero) of all the extant versions of the
     * datastream. 
     * @throws ParserConfigurationException 
     */
    public String getLastModDateForDatastreamRevision(String pid, String dsId, int revisionNumber) throws IOException, FedoraException, ParserConfigurationException {
        String url = this.fedoraBaseUrl + "/objects/" + pid + "/datastreams/" + dsId + "/history?format=xml";
        try {
            GetMethod get = new GetMethod(url);
            try {
                client.executeMethod(get);
                if (get.getStatusCode() == HttpStatus.SC_OK) {
                    DocumentBuilder parser = getDocumentBuilder();
                    Document doc = null;
                    synchronized (parser) {
                        doc = parser.parse(get.getResponseBodyAsStream());
                    }
                    get.getResponseBodyAsStream().close();
                    NodeList profiles = (NodeList) this.getXPath().evaluate("fedora-management:datastreamHistory/fedora-management:datastreamProfile", doc, XPathConstants.NODESET);
                    return (String) this.getXPath().evaluate("fedora-management:dsCreateDate", profiles.item(profiles.getLength() - revisionNumber - 1), XPathConstants.STRING);
                } else {
                    throw new RuntimeException("REST action \"" + url + "\" failed: " + get.getStatusLine());
                }
            } finally {
                get.releaseConnection();
            }
        } catch (SAXException ex) {
            throw new FedoraResponseParsingException(ex);
        } catch (XPathExpressionException ex) {
           throw new FedoraException(ex);
        }
    }

    /**
     * Exports the given object's FOXML for migration.
     * @param pid the PID for the object
     * @return the DOM Document
     * @throws ParserConfigurationException 
     * @throws SAXException 
     */
    public Document exportObjectAsDocument(String pid) throws FedoraException, IOException, SAXException, ParserConfigurationException {
        GetMethod get = new GetMethod(this.fedoraBaseUrl + "/objects/" + pid + "/export");
        try {
            client.executeMethod(get);
            if (get.getStatusCode() == 200) {
                InputStream response = get.getResponseBodyAsStream();
                Document doc = null;
                DocumentBuilder parser = getDocumentBuilder();
                synchronized (parser) {
                    doc = parser.parse(response);
                }
                response.close();
                return doc;
            } else {
                throw new FedoraException("Invalid HTTP Status code: " + get.getStatusLine());
            }
        } finally {
            get.releaseConnection();
        }
    }
    
    /**
     * Imports the given FOXML file to this repository.
     * @param foxmlFile the FOXML for the new object
     * @throws FedoraException if an error occurs with the REST call
     * @throws IOException if an error occurs while reading or writing
     * data.
     */
    public void importObject(File foxmlFile) throws FedoraException, IOException {
        if (this.readOnly) {
            throw new FedoraIllegalAccessException("This FedoraClient is READ ONLY!");
        }
        String url = this.fedoraBaseUrl + "/objects/new?format=" + URLEncoder.encode("info:fedora/fedora-system:FOXML-1.1", "UTF-8");
        PostMethod filePost = new PostMethod(url);
        filePost.setRequestEntity(new FileRequestEntity(foxmlFile, "text/xml"));
        try {
            int status = client.executeMethod(filePost);
            if (status != HttpStatus.SC_CREATED && status != HttpStatus.SC_CREATED) {
                throw new RuntimeException("REST action \"" + url + "\" failed: " + filePost.getStatusLine());
            }
            logger.info(this.fedoraBaseUrl + ": Ingested object from FOXML");
        } finally {
            filePost.releaseConnection();
        }
    }

    /**
     * Gets the given datastream and parses it as an XML document 
     * which is returned.
     * @throws IOException 
     * @throws FedoraException 
     * @throws IOException if an error occurs while reading data from
     * the fedora server
     * @throws FedoraException if an error occurs parsing the response
     */
    public Document getXMLDatastreamAsDocument(String pid, String dsName) throws FedoraException, IOException {
        return getXMLDatastreamAsDocument(pid, dsName, null);
    }
    
    public Document getXMLDatastreamAsDocument(String pid, String dsName, String date) throws FedoraException, IOException {
        try {
            String url = this.fedoraBaseUrl + "/objects/" + pid + "/datastreams/" + dsName + "/content" + (date != null ? "?asOfDateTime=" + URLEncoder.encode(date, "UTF-8") : "");
            GetMethod get = new GetMethod(url);
            try {
                int status = client.executeMethod(get);
                if (status != HttpStatus.SC_CREATED && status != HttpStatus.SC_OK) {
                    throw new RuntimeException("REST action \"" + url + "\" failed: " + get.getStatusLine());
                }
                InputStream response = get.getResponseBodyAsStream();
                Document doc = null;
                DocumentBuilder parser = getDocumentBuilder();
                synchronized (parser) {
                    doc = parser.parse(response);
                }
                response.close();
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
                InputStream response = get.getResponseBodyAsStream();
                Document dsDoc = null;
                DocumentBuilder parser = getDocumentBuilder();
                synchronized (parser) {
                    dsDoc = parser.parse(response);
                }
                response.close();
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
            InputStream response = get.getResponseBodyAsStream();
            boolean hasDatastream = (readStream(response, get.getResponseCharSet()).indexOf("dsid=\"" + dsName + "\"") != -1);
            response.close();
            return hasDatastream;
        } finally {
            get.releaseConnection();
        }
    }
    
    public void addRedirectDatastream(String pid, String dsName, String URL) throws FedoraException, IOException {
        if (this.readOnly) {
            throw new FedoraIllegalAccessException("This FedoraClient is READ ONLY!");
        }
        PostMethod post = new PostMethod(this.fedoraBaseUrl + "/objects/" + pid + "/datastreams/" + dsName
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
    
    public void addReferenceDatastream(String pid, String dsName, String URL) throws FedoraException, IOException {
        if (this.readOnly) {
            throw new FedoraIllegalAccessException("This FedoraClient is READ ONLY!");
        }
        PostMethod post = new PostMethod(this.fedoraBaseUrl + "/objects/" + pid + "/datastreams/" + dsName
                + "?controlGroup=" + ControlGroup.E
                + "&dsLocation=" + URLEncoder.encode(URL, "UTF-8")
                + "&dsLabel=" + URLEncoder.encode("Access Control Policy", "UTF-8")
                + "&formatURI=" + URLEncoder.encode("urn:oasis:names:tc:xacml:1.0:policy", "UTF-8")
                + "&mimeType=" + URLEncoder.encode("text/xml", "UTF-8"));
        try {
            client.executeMethod(post);
            if (post.getStatusCode() != 201) {
                throw new FedoraException("Invalid HTTP Status code: " + post.getStatusLine());
            }
        } finally {
            post.releaseConnection();
        }
    }
    
    
    public void purgeDatastream(String pid, String dsName) throws FedoraException, IOException {
        if (this.readOnly) {
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
    
    public void purgeObject(String pid) throws IOException, FedoraException {
        if (this.readOnly) {
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
            int statusCode = client.executeMethod(method);
            if (statusCode != HttpStatus.SC_CREATED) {
                throw new RuntimeException("REST action \"" + url + "\" failed: " + method.getStatusLine());
            } else {
                pid = method.getResponseBodyAsString(1024);
                logger.info(this.fedoraBaseUrl + ": Created object " + pid);
                return pid;
            }
        } finally {
            method.releaseConnection();
        }
    }
    
    public void setStatus(String pid, String status, String logMessage) throws HttpException, IOException, FedoraException {
        if (this.readOnly) {
            throw new FedoraIllegalAccessException("This FedoraClient is READ ONLY!");
        }
        if (status.equals("A") || status.equals("D") || status.equals("I")) {
            String url = this.fedoraBaseUrl + "/objects/" + pid + "?state=" + status + (logMessage != null ? "&logMessage=" + URLEncoder.encode(logMessage, "UTF-8") : "");
            PutMethod method = new PutMethod(url);
            try {
                int statusCode = client.executeMethod(method);
                if (statusCode != HttpStatus.SC_OK) {
                    throw new RuntimeException("REST action \"" + url + "\" failed: " + method.getStatusLine());
                } else {
                    logger.info(this.fedoraBaseUrl + ": Set status to \"" + status + "\" for object " + pid + " with message \"" + logMessage + "\".");
                    return;
                }
            } finally {
                method.releaseConnection();
            }
        } else {
            throw new FedoraException("Unknown state \"" + status + "\".");
        }
    }
    
    public List<String> getParts(String parentPid) throws FedoraException, IOException {
        return getRelatedPids(parentPid, "info:fedora/fedora-system:def/relations-external#isPartOf");
    }

    public List<String> getCollectionMembers(String collectionPid) throws FedoraException, IOException {
        return getCollectionMembers(collectionPid, false);
    }
    
    public List<String> getCollectionMembers(String collectionPid, boolean excludeDeleted) throws FedoraException, IOException {
        return getRelatedPids(collectionPid, "info:fedora/fedora-system:def/relations-external#isMemberOfCollection", excludeDeleted);
    }
    
    public List<String> getContentModelURIs(String objectPid) throws FedoraException, IOException {
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
                    synchronized (parser) {
                        doc = parser.parse(response);
                    }
                    response.close();
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
     * A helper method to get the checksum for a given datastream.  If no checksum
     * is stored null is returned. 
     */
    public String getMD5Checksum(String pid, String dsName) throws FedoraException, IOException {
        try {
            return this.getDatastreamProperty(pid, dsName, DatastreamProfile.DatastreamProperty.DS_CHECKSUM);
        } catch (XPathExpressionException ex) {
            throw new FedoraException(ex);
        } catch (SAXException ex) {
            throw new FedoraException(ex);
        } catch (ParserConfigurationException ex) {
            throw new FedoraException(ex);
        }
    }

    /**
     * Fetches the object profile XML.
     * @param pid the pid of the object to be queried
     * @return an encapsulated version of the object profile
     */
    public ObjectProfile getObjectProfile(String pid, String dateTime) throws SAXException, IOException, ParserConfigurationException, FedoraException {
        String url = this.fedoraBaseUrl + "/objects/" + pid + "?format=xml" + (dateTime != null ? "&asOfDateTime=" + dateTime : "");
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
     * Fetches the object profile XML and parse out the property value 
     * for the given property.
     * @param pid the pid of the object to be queried
     * @param dsName the name of the datastream who's property is being
     * queried
     * @param prop the property to query.
     * @return the value of the property
     * 
     */
    public String getDatastreamProperty(String pid, String dsName, DatastreamProfile.DatastreamProperty prop) throws HttpException, IOException, SAXException, ParserConfigurationException, FedoraException, XPathExpressionException {
        String url = this.fedoraBaseUrl + "/objects/" + pid + "/datastreams/" + dsName + "?format=xml";
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
                return new DatastreamProfile(this, xml.getDocumentElement()).getProperty(prop);
            } else {
                throw new FedoraException("REST action \"" + url + "\" failed: " + get.getStatusLine());
            }
        } finally {
            get.releaseConnection();
        }
    }
    
    public DatastreamProfile getDatastreamProfile(String pid, String dsId, Date date) throws HttpException, IOException, FedoraException, SAXException, ParserConfigurationException {
        String url = this.fedoraBaseUrl + "/objects/" + pid + "/datastreams/" + dsId + "?format=xml" + (date != null ? "&asOfDateTime=" + printFedoraDateString(date) : "");
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
                return new DatastreamProfile(this, xml.getDocumentElement());
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
                InputStream response = get.getResponseBodyAsStream();
                Document xml = null;
                DocumentBuilder parser = getDocumentBuilder();
                synchronized (parser) {
                    xml = parser.parse(response);
                }
                response.close();
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
     * Exposes the addRelationship REST API method.
     * @param pid persistent identifier of the digital object
     * @param subject URI of this object
     * @param predicate predicate of the relationship
     * @param object object of the relationship
     * @param isLiteral true if the object of the relationship is a literal, false if it is a URI
     * @param datatype if the object is a literal, the datatype of the literal (optional)
     * @throws FedoraException 
     */
    public void addRelationship(String pid, String subject, String predicate, String object, boolean isLiteral, String datatype) throws FedoraException, IOException {
        if (this.readOnly) {
            throw new FedoraIllegalAccessException("This FedoraClient is READ ONLY!");
        }
        PostMethod post = new PostMethod(this.fedoraBaseUrl + "/objects/" + pid + "/relationships/new"
                + "?subject=" + URLEncoder.encode(subject, "UTF-8")
                + "&predicate=" + URLEncoder.encode(predicate, "UTF-8")
                + "&object=" + URLEncoder.encode(object, "UTF-8")
                + "&isLiteral=" + URLEncoder.encode(String.valueOf(isLiteral), "UTF-8")
                + (datatype != null ? "&datatype=" + URLEncoder.encode(datatype, "UTF-8") : ""));
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
    public void purgeRelationship(String pid, String subject, String predicate, String object, boolean isLiteral, String datatype) throws FedoraException, IOException {
        if (this.readOnly) {
            throw new FedoraIllegalAccessException("This FedoraClient is READ ONLY!");
        }
        DeleteMethod delete = new DeleteMethod(this.fedoraBaseUrl + "/objects/" + pid + "/relationships"
                + "?subject=" + URLEncoder.encode(subject, "UTF-8")
                + "&predicate=" + URLEncoder.encode(predicate, "UTF-8")
                + "&object=" + URLEncoder.encode(object, "UTF-8")
                + "&isLiteral=" + URLEncoder.encode(String.valueOf(isLiteral), "UTF-8")
                + (datatype != null ? "&datatype=" + URLEncoder.encode(datatype, "UTF-8") : ""));
        try {
            client.executeMethod(delete);
            if (delete.getStatusCode() != 200) {
                throw new FedoraException("Invalid HTTP Status code: " + delete.getStatusLine());
            }
        } finally {
            delete.releaseConnection();
        }
    }
    
    /**
     * Adds the given content model to the given pid.
     * @param pid the pid to be updated (which conforms to the given content model)
     * @param contentModelPid the pid for the content model object
     */
    public void addContentModel(String pid, String cmodelPid) throws FedoraException, IOException {
        this.addRelationship(pid, "info:fedora/" + pid, "info:fedora/fedora-system:def/model#hasModel", "info:fedora/" + cmodelPid, false, null);
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
            } catch (ParserConfigurationException ex) {
                throw new FedoraResponseParsingException(ex);
            } catch (SAXException ex) {
                throw new FedoraResponseParsingException(ex);
            }
            return pids;
        }
    }
    
    public List<String> getPidsResultingFromQuery(String pidName, String query) throws FedoraException, IOException {
        List<String> pids = new ArrayList<String>();
        String riSearchUrl = this.fedoraBaseUrl + (this.fedoraBaseUrl.endsWith("/") ? "" : "/") + "risearch?type=tuples&lang=itql&format=Sparql&query=" + URLEncoder.encode(query, "UTF-8");
        try {
            Document doc = parseUrlAsXmlDocument(riSearchUrl);
            NodeList children = doc.getDocumentElement().getElementsByTagName(pidName);
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
     * Gets the number of objects in a collection through a ResourceIndex 
     * query.  This is *MUCH* faster than getting a List of collection 
     * members and computing the size.
     * @param collectionPid the pid for the collection object whose member
     * count will be queried and returend.
     * @return the number of objects with an isMemberOfCollection relationship
     * to the object referenced by the given pid as reported by the fedora
     * resource index.
     */
    public int getCollectionMemberCount(String collectionPid) throws IOException, FedoraException {
        String riSearchUrl = this.fedoraBaseUrl + (this.fedoraBaseUrl.endsWith("/") ? "" : "/") + "risearch?type=tuples&lang=itql&format=CSV&query=select%20count(%0A%20%20select%20%20%24object%0A%20%20from%20%20%20%20%3C%23ri%3E%0A%20%20where%20%20%20%24object%0A%20%20%20%20%20%20%20%20%20%20%3Cinfo%3Afedora%2Ffedora-system%3Adef%2Frelations-external%23isMemberOfCollection%3E%0A%20%20%20%20%20%20%20%20%20%20%3Cinfo%3Afedora%2F" + URLEncoder.encode(collectionPid, "UTF-8") + "%3E%0A%20%20)%0Afrom%20%3C%23ri%3E%0Awhere%20%24a%20%24b%20%24c%0A";
        GetMethod get = new GetMethod(riSearchUrl);
        try {
            client.executeMethod(get);
            if (get.getStatusCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(get.getResponseBodyAsStream()));
                try {
                    String label = reader.readLine();
                    String value = reader.readLine();
                    return Integer.parseInt(value);
                } catch (Throwable t) {
                    throw new FedoraResponseParsingException(t);
                } finally {
                    reader.close();
                }
            } else {
                throw new FedoraException("REST action \"" + riSearchUrl + "\" failed: " + get.getStatusLine());
            }
        } finally {
            get.releaseConnection();
        }
    }
    
    /**
     * Updates the fedora DC datastream to include the given identifier.
     * If the given identifier is already present, this method does not
     * alter the object, otherwise it adds a single "dc.identifier" element
     * with the given value.  Other DC elements (even other dc.identifier) 
     * elements are retained.
     * 
     * This method was added in version 2.0.2
     * 
     * @param pid the pid of the object to update
     * @param id the identifier to add
     * @throws IOException 
     * @throws FedoraException 
     */
    public void addDCIdentifier(String pid, String id) throws FedoraException, IOException {
        // fetch the current DC record
        Document dcDoc = this.getXMLDatastreamAsDocument(pid, "DC");
        
        try {
            // check for existing identifier
            NodeList idNl = (NodeList) getXPath().evaluate("oai_dc:dc/dc:identifier", dcDoc, XPathConstants.NODESET);
            for (int i = 0; i < idNl.getLength(); i ++) {
                if (id.equals(getXPath().evaluate("text()", idNl.item(i), XPathConstants.STRING))) {
                    logger.debug("There is already a dc.identifier element with value \"" + id + "\" on " + pid + ".");
                    return;
                }
            }
            
            // add identifier
            Element oaiDcEl = (Element) getXPath().evaluate("oai_dc:dc", dcDoc, XPathConstants.NODE);
            Element idEl = dcDoc.createElementNS(this.getMapNamespaceContext().getNamespaceURI("dc"), "dc:identifier");
            idEl.appendChild(dcDoc.createTextNode(id));
            oaiDcEl.appendChild(idEl);
            
            // write it back
            this.addOrReplaceDatastreamWithDocument(pid, "DC", ControlGroup.X, "Fedora Dublin Core", "text/xml", dcDoc, "dc.xml");
        } catch (XPathExpressionException ex) {
            // won't happen because the xpaths are hard-coded
            throw new FedoraException(ex);
        }
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
                logger.warn("No object found with dc.identifier=\"" + identifier + "\"");
                return pids;
            } else  {
                
                for (int i = 0; i < children.getLength(); i ++) {
                    String pid = ((Element) children.item(i)).getAttribute("uri").replace("info:fedora/", "");
                    logger.debug("PID, \"" + pid + "\", found object with dc.identifier=\"" + identifier + "\"");
                    pids.add(pid);
                }
                return pids;
            }
        } catch (ParserConfigurationException ex) {
            throw new FedoraException(ex);
        } catch (MalformedURLException ex) {
            throw new FedoraException(ex);
        } catch (SAXException ex) {
            throw new FedoraResponseParsingException(ex);
        } catch (XPathExpressionException ex) {
            throw new FedoraException(ex);
        }
    }
    
    /**
     * Gets all DC identifiers for the given object except the pid.
     * @param pid the pid of the object whose DC identifiers are being queried
     * @return a list (never null, but sometimes empty) of all the DC identifiers
     * except the pid for that object.
     * @throws FedoraException
     * @throws IOException
     */
    public List<String> getDCIdentifiers(String pid) throws FedoraException, IOException {
        Document dcDoc = getXMLDatastreamAsDocument(pid, "DC");
        List<String> nonPidIds = new ArrayList<String>();
        NodeList idNl = null;
        try {
            idNl = (NodeList) getXPath().evaluate("/oai_dc:dc/dc:identifier", dcDoc, XPathConstants.NODESET);
        } catch (XPathExpressionException ex) {
            // shouldn't happen because XPATH isn't variable
            throw new FedoraException(ex);
        }
        if (idNl != null) {
            for (int i = 0; i < idNl.getLength(); i ++) {
                Element id = (Element) idNl.item(i);
                String value = id.getFirstChild().getNodeValue();
                if (!value.equals(pid)) {
                    nonPidIds.add(value);
                }
            }
        }
        return nonPidIds;
    }
    
    /**
     * Uses the Resource Index search to find the object with the given
     * dc.title.
     */
    public String titleLookup(String title) throws FedoraException, IOException {
        String riSearchUrl = this.fedoraBaseUrl + (this.fedoraBaseUrl.endsWith("/") ? "" : "/") + "risearch?type=tuples&lang=itql&format=Sparql&query=select%20%24member%20from%20%3C%23ri%3E%20where%20%24member%20%3Cdc%3Atitle%3E%20'" + URLEncoder.encode(title, "UTF-8")+ "'";
        try {
            Document doc = parseUrlAsXmlDocument(riSearchUrl);
            NodeList children = (NodeList) this.getXPath().evaluate("//sparql:sparql/sparql:results/sparql:result/sparql:member", doc, XPathConstants.NODESET);
            if (children.getLength() == 0) {
                logger.warn("No object found with dc.title=\"" + title + "\"");
                return null;
            } else if (children.getLength() == 1) {
                String pid = ((Element) children.item(0)).getAttribute("uri").replace("info:fedora/", "");
                logger.debug("PID, \"" + pid + "\", found object with dc.title=\"" + title + "\"");
                return pid;
            } else {
                logger.error(children.getLength() + " PIDs found objects with dc.title=\"" + title + "\"");
                return null;
            }
        } catch (ParserConfigurationException ex) {
            throw new FedoraException(ex);
        } catch (SAXException ex) {
            throw new FedoraResponseParsingException(ex);
        } catch (XPathExpressionException ex) {
            throw new FedoraResponseParsingException(ex);
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
            synchronized (parser) {
                doc = parser.parse(response);
            }
            response.close();
            return doc;
        } catch (RuntimeException ex) {
            logger.error("Error parsing XML URL: " + url);
            throw ex;
        } finally {
            get.releaseConnection();
        }
    }
}
