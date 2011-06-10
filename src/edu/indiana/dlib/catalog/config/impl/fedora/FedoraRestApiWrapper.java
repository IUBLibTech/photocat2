/**
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
package edu.indiana.dlib.catalog.config.impl.fedora;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
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

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.ByteArrayPartSource;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * <p>
 *   A class that wraps the fedora REST API calls needed
 *   by this application.  This could easily be replaced
 *   with any other codebase that interacts with fedora.
 *   TODO: replace this with the mediashelf fedora client
 * </p>
 * <p>
 *   <strong>This class is not thread-safe</strong>
 * </p>
 * 
 */
public class FedoraRestApiWrapper {

    public static final Logger LOGGER = Logger.getLogger(FedoraRestApiWrapper.class);
    
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
        this.client = new HttpClient();
        if (username != null) {
            this.client.getParams().setAuthenticationPreemptive(true);
            Credentials credentials = new UsernamePasswordCredentials(username, password);
            this.client.getState().setCredentials(new AuthScope(fedoraHost, fedoraPort), credentials);
        }

        this.readOnly = readOnly;
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
            Document doc = this.getDocumentBuilder().parse(new InputSource(new URL(riSearchUrl).openStream()));
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

    /**
     * Gets access InputStream access to a given datastream.
     * The current implementation uses the new REST API.
     * @param pid identifies the object whose datastream is to be retrieved
     * @param dsName the name of the datastream
     * @return an InputStream to access the resource
     * @throws IOException if an error occurs
     */
    public InputStream getDatastream(String pid, String dsName) throws IOException {
        return getDatastream(pid, dsName, null);
    }
    
    public InputStream getDatastream(String pid, String dsName, String asOfDateTime) throws IOException {
        String url = this.fedoraBaseUrl + "/objects/" + pid + "/datastreams/" + dsName + "/content" + (asOfDateTime != null ? "?asOfDateTime=" + URLEncoder.encode(asOfDateTime, "UTF-8") : "");
        GetMethod get = new GetMethod(url);
        this.client.executeMethod(get);
        int status = this.client.executeMethod(get);
        if (status != HttpStatus.SC_CREATED && status != HttpStatus.SC_OK) {
            throw new RuntimeException("REST action \"" + url + "\" failed: " + get.getStatusLine());
        }
        return get.getResponseBodyAsStream();
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
                    int status = this.client.executeMethod(filePost);
                    if (status != HttpStatus.SC_CREATED && status != HttpStatus.SC_OK) {
                        throw new RuntimeException("REST action \"" + url + "\" failed: " + filePost.getStatusLine());
                    }
                    LOGGER.info(this.fedoraBaseUrl + ": Replaced datastream " + dsName + " on " + pid + ".");
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
                    int status = this.client.executeMethod(filePost);
                    if (status != HttpStatus.SC_CREATED) {
                        throw new RuntimeException("REST action \"" + url + "\" failed: " + filePost.getStatusLine());
                    }
                    LOGGER.info(this.fedoraBaseUrl + ": Added datastream " + dsName + " on " + pid + ".");
                }
            }
        } catch (Exception ex) {
            throw new FedoraException(ex);
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
        this.client.executeMethod(get);
        return (readStream(get.getResponseBodyAsStream(), get.getResponseCharSet()).indexOf("dsid=\"" + dsName + "\"") != -1);
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
        this.client.executeMethod(get);
        try {
            Document dsDoc = this.getDocumentBuilder().parse(get.getResponseBodyAsStream());
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
        this.client.executeMethod(get);
        if (get.getStatusCode() == 200) {
            Document xml  = this.getDocumentBuilder().parse(get.getResponseBodyAsStream());
            
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
    }
    
    public List<DatastreamProfile> getDatastreamHistory(String pid, String dsId) throws HttpException, IOException, FedoraException, SAXException, ParserConfigurationException, XPathExpressionException {
        String url = this.fedoraBaseUrl + "/objects/" + pid + "/datastreams/" + dsId + "/history?format=xml";
        GetMethod get = new GetMethod(url);
        this.client.executeMethod(get);
        if (get.getStatusCode() == 200) {
            List<DatastreamProfile> history = new ArrayList<DatastreamProfile>();
            Document xml = this.getDocumentBuilder().parse(get.getResponseBodyAsStream());
            NodeList nl = (NodeList) this.getXPath().evaluate("/fedora-management:datastreamHistory/fedora-management:datastreamProfile", xml, XPathConstants.NODESET);
            for (int i = 0; i < nl.getLength(); i ++) {
                Element el = (Element) nl.item(i);
                history.add(new DatastreamProfile(this, el));
            }
            return history;
        } else {
            throw new FedoraException("REST action \"" + url + "\" failed: " + get.getStatusLine());
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
            Document doc = this.getDocumentBuilder().parse(new InputSource(new URL(riSearchUrl).openStream()));
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
            Document doc = this.getDocumentBuilder().parse(new InputSource( new URL(riSearchUrl).openStream()));
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
                Document doc = this.getDocumentBuilder().parse(new InputSource( new URL(riSearchUrl).openStream()));
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

}
