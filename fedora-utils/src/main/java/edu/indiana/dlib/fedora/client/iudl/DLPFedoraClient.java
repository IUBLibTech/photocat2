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
package edu.indiana.dlib.fedora.client.iudl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.httpclient.methods.GetMethod;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import edu.indiana.dlib.fedora.client.FedoraClient;
import edu.indiana.dlib.fedora.client.FedoraException;
import edu.indiana.dlib.fedora.client.FedoraIllegalAccessException;
import edu.indiana.dlib.fedora.client.FedoraResponseParsingException;
import edu.indiana.dlib.fedora.client.MapNamespaceContext;

/**
 * An extension of the FedoraClient that has higher level methods 
 * appropraite for interacting with objects in the IUDLP repository.
 * This includes encapsulation of rules such as our identifier 
 * conventions and our custom relationships.
 */
public class DLPFedoraClient extends FedoraClient {

    /**
     * Enumerates the statuses available for a fedora object.  This
     * is documented at https://wiki.dlib.indiana.edu/display/meta/Record+statuses
     * and a schema exists to validate this at 
     * http://fedora.dlib.indiana.edu:8080/fedora/get/xml:status/SCHEMA.
     */
    public static enum DLPStatus {
        AUTO_GENERATED("auto generated"),
        INITIAL_IMPORT("initial import"),
        IN_PROGRESS("in progress"),
        READY_FOR_ENHANCEMENT("ready for enhancement"),
        PENDING_APPROVAL("pending approval"),
        MINIMAL("minimal"),
        PENDING_COMPLETION("pending completion"),
        CATALOGED("cataloged"),
        UNSPECIFIED("");
        
        String value;
        
        DLPStatus(String value) {
            this.value = value;
        }
        
        public String getString() {
            return this.value;
        }
    }
    
    public static final String STATUS_DS_ID = "DLP_STATUS";
    
    public static final String METS_DS_ID = "METADATA";
    
    public static final String ITEM_METADATA_DS_ID = "ITEM-METADATA";
    
    public DLPFedoraClient(File f) throws IOException {
        this(loadProperties(f));
    }
    
    public static Properties loadProperties(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        try {
            Properties p = new Properties();
            p.load(fis);
            return p;
        } finally {
            fis.close();
        }
    }
    
    public DLPFedoraClient(Properties p) {
        super(p.getProperty("username"), p.getProperty("password"), p.getProperty("fedoraHost"), p.getProperty("contextName"), Integer.parseInt(p.getProperty("port")), "true".equals(p.getProperty("readOnly")));
    }
    
    public DLPFedoraClient(String fedoraHost, String fedoraContextName, int port, boolean readOnly) {
        super(fedoraHost, fedoraContextName, port, readOnly);
    }
    
    public DLPFedoraClient(String username, String password, String fedoraHost, String fedoraContextName, int port, boolean readOnly) {
        super(username, password, fedoraHost, fedoraContextName, port, readOnly);
    }

    /**
     * Overrides the superclass to include some extra required namespaces needed
     * by other methods of this class.
     */
    protected MapNamespaceContext createNamespaceContext() {
        MapNamespaceContext nsc = super.createNamespaceContext();
        nsc.setNamespace("iudl", "http://dlib.indiana.edu/lib/xml/infrastructure/rdfRelations#");
        nsc.setNamespace("iudl-status", "http://dlib.indiana.edu/lib/xml/infrastructure/status", "http://fedora.dlib.indiana.edu:8080/fedora/get/xml:status/SCHEMA");
        nsc.setNamespace("mets", "http://www.loc.gov/METS/", "http://www.loc.gov/standards/mets/version18/mets.xsd");
        nsc.setNamespace("m", "info:photocat/metadata");
        return nsc;
    }
    
    /**
     * Gets the PURL for the object with the given pid.
     * @param pid the pid of an object in fedora
     * @return the PURL for that object if one exists
     * @throws FedoraException if an error occurs with fedora or in
     * parsing the fedora response
     * @throws IOException if an error occurs fetching data from
     * fedora
     */
    public String getPURL(String pid) throws FedoraException, IOException {
        for (String dcIdentifier : getDCIdentifiers(pid)) {
            if (PURLLogic.couldBePURL(dcIdentifier)) {
                return dcIdentifier;
            }
        }
        return null;
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
                logger.warn("No PID found for PURL: " + PURL);
                return null;
            } else if (children.getLength() == 1) {
                String pid = ((Element) children.item(0)).getAttribute("uri").replace("info:fedora/", "");
                logger.debug("PID, \"" + pid + "\", found for PURL: " + PURL);
                return pid;
            } else {
                logger.error(children.getLength() + " PIDs found for PURL: " + PURL);
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
     * Uses the (SLOW) database search function to find the object with 
     * the given PURL as its identifier.  This method returns null if no
     * objects have the given PURL, but throws an exception of more than
     * one do.  This method should only be used when getPidForPURL isn't 
     * up to the task (perhaps because the resource index insn't enabled
     * or synchronous) because this method may be 10 to 100 times slower.
     */
    public String getPidForPURLUsingSearch(String PURL) throws FedoraException, IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        GetMethod get = new GetMethod(this.fedoraBaseUrl + "/objects?" + "pid=true" + "&terms=" + URLEncoder.encode(PURL, "UTF-8") + "&maxResults=2" + "&resultFormat=xml");
        try {
            client.executeMethod(get);
            if (get.getStatusCode() == 200) {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(true);
                DocumentBuilder builder = factory.newDocumentBuilder();
                InputStream response = get.getResponseBodyAsStream();
                Document doc = null;
                DocumentBuilder parser = getDocumentBuilder();
                synchronized (parser) {
                    doc = parser.parse(response);
                }
                response.close();
    
                MapNamespaceContext nsc = new MapNamespaceContext();
                nsc.setNamespace("fedora-types", "http://www.fedora.info/definitions/1/0/types/");
                XPath xpath = XPathFactory.newInstance().newXPath();
                xpath.setNamespaceContext(nsc);
                
                NodeList pidNL = (NodeList) xpath.evaluate("/fedora-types:result/fedora-types:resultList/fedora-types:objectFields/fedora-types:pid", doc, XPathConstants.NODESET);
                if (pidNL.getLength() == 0) {
                    logger.warn("No PID found for PURL: " + PURL);
                    return null;
                } else if (pidNL.getLength() == 1) {
                    String pid = pidNL.item(0).getFirstChild().getNodeValue();
                    logger.debug("PID, \"" + pid + "\", found for PURL: " + PURL);
                    return pid;
                } else {
                    logger.error(pidNL.getLength() + " PIDs found for PURL: " + PURL);
                    throw new FedoraException("REPOSITORY ERROR: duplicate items");
                }
            } else {
                throw new FedoraException(get.getStatusLine().getReasonPhrase());
            }
        } finally {
            get.releaseConnection();
        }
    }
    
    /**
     * Creates a new object in fedora which will be addressable by the given
     * PURL.  Essentially this is TWO operations, one to create an object, a
     * second to add an additional DC identifier containing the PURL.  This 
     * method will fail if an object already exists with the given PURL.  
     * @param purl the PURL for the object
     * @return the pid of the newly created object
     */
    public String createDLPObject(String purl, String label, String pidNamespace, String logMessage) throws FedoraException, IOException {
        return createDLPObject(null, purl, label, pidNamespace, logMessage);
    }
    
    public String createDLPObject(String pid, String purl, String label, String pidNamespace, String logMessage) throws FedoraException, IOException {
        if (this.readOnly) {
            throw new FedoraIllegalAccessException("This FedoraClient is READ ONLY!");
        }
        if (!PURLLogic.couldBePURL(purl)) {
            throw new IllegalArgumentException(purl + " does not appear to be a valid PURL.");
        }
        String existingPid = this.getPidForPURL(purl);
        if (existingPid != null) {
            throw new IllegalStateException(existingPid + " already has the given PURL, \"" + purl + "\".");
        }
        pid = createObject(pid, label, pidNamespace, null, logMessage);
        try {
            generateAndAddDC(pid, label, purl);
            return pid;
        } catch (ParserConfigurationException ex) {
            this.purgeObject(pid);
            throw new FedoraException("Error creating DLP Object!", ex);
        }
    }
    
    /**
     * Gets the DLP status of the given object in the repository.  This status
     * has been traditionally stored in different locations, so this method
     * checks all and returns the status from the preferred location if possible
     * then older deprecated locations, and if none is found, returns null.
     * @param pid the pid that identifies the object whose DLP status is being
     * queried.

     */
    public DLPStatus getDLPStatus(String pid) throws FedoraException, IOException {
        List<String> datastreams = this.listDatastreams(pid);
        
        // check the ITEM-METADATA status field
        if (datastreams.contains(ITEM_METADATA_DS_ID)) {
            Document imDoc = getXMLDatastreamAsDocument(pid, ITEM_METADATA_DS_ID);
            try {
                String statusStr = (String) this.getXPath().evaluate("m:itemMetadata/m:field[@fieldType='STATUS']/m:values/m:value/m:part[@property='status']", imDoc, XPathConstants.STRING);
                if ("NotCataloged".equalsIgnoreCase(statusStr)) {
                    return DLPStatus.AUTO_GENERATED;
                } else if ("not cataloged".equalsIgnoreCase(statusStr)) {
                    return DLPStatus.AUTO_GENERATED;
                } else if ("Cataloged".equalsIgnoreCase(statusStr)) {
                    return DLPStatus.CATALOGED;
                } else if ("needs attention".equalsIgnoreCase(statusStr)) {
                    return DLPStatus.PENDING_APPROVAL;
                } else if ("minimal".equalsIgnoreCase(statusStr)) {
                    return DLPStatus.MINIMAL;
                } else if ("in progress".equalsIgnoreCase(statusStr)) {
                    return DLPStatus.IN_PROGRESS;
                } else if ("auto-generated".equalsIgnoreCase(statusStr)) {
                    return DLPStatus.AUTO_GENERATED;
                } else if (statusStr == null || "".equalsIgnoreCase(statusStr)) {
                    return DLPStatus.UNSPECIFIED;
                } else {
                    throw new FedoraException("Unknown Status, \"" + statusStr + "\" found for " + pid + ".");
                }
            } catch (XPathExpressionException ex) {
                throw new FedoraException(ex);
            }
        }
        
        // next check the old "right" location, which is the DLP_STATUS datastream.
        if (datastreams.contains(STATUS_DS_ID)) {
            Document doc = this.getXMLDatastreamAsDocument(pid, STATUS_DS_ID);
            try {
                String statusStr = (String) this.getXPath().evaluate("/iudl-status:status/iudl-status:recordStatus", doc, XPathConstants.STRING);
                for (DLPStatus status : DLPStatus.values()) {
                    if (status.value.equals(statusStr)) {
                        return status;
                    }
                }
                throw new FedoraException("Unknown status: " + statusStr);
            } catch (XPathExpressionException ex) {
                throw new FedoraException(ex);
            }
        }
        
        // next check the "wrong" location, in the METADATA datastream
        if (datastreams.contains(METS_DS_ID)) {
            Document metsDoc = this.getXMLDatastreamAsDocument(pid, METS_DS_ID);
            try {
                String statusStr = (String) this.getXPath().evaluate("/mets:mets/mets:dmdSec[@ID='dmdSec-iudlAdmin']/mets:mdWrap/mets:xmlData/iudlAdmin/status", metsDoc, XPathConstants.STRING);
                if ("NotCataloged".equalsIgnoreCase(statusStr)) {
                    return DLPStatus.AUTO_GENERATED;
                } else if ("not cataloged".equalsIgnoreCase(statusStr)) {
                    return DLPStatus.AUTO_GENERATED;
                } else if ("Cataloged".equalsIgnoreCase(statusStr)) {
                    return DLPStatus.CATALOGED;
                } else if ("needs attention".equalsIgnoreCase(statusStr)) {
                    return DLPStatus.PENDING_APPROVAL;
                } else if ("minimal".equalsIgnoreCase(statusStr)) {
                    return DLPStatus.MINIMAL;
                } else if ("in progress".equalsIgnoreCase(statusStr)) {
                    return DLPStatus.IN_PROGRESS;
                } else if ("auto-generated".equalsIgnoreCase(statusStr)) {
                    return DLPStatus.AUTO_GENERATED;
                } else if (statusStr == null || "".equalsIgnoreCase(statusStr)) {
                    return DLPStatus.UNSPECIFIED;
                } else {
                    throw new FedoraException("Unknown Status, \"" + statusStr + "\" found for " + pid + ".");
                }
            } catch (XPathExpressionException ex) {
                throw new FedoraException(ex);
            }
        }
        
        return DLPStatus.UNSPECIFIED;
    }
    
    /**
     * Sets the status for the given object.  The current
     * implementation simply replaces the DLP_STATUS datastream
     * with a new one containing the new status.  
     * @throws ParserConfigurationException 
     * @throws FedoraException 
     */
    public void setDLPStatus(String pid, DLPStatus status) throws ParserConfigurationException, FedoraException {
        if (status == DLPStatus.UNSPECIFIED) {
            throw new IllegalArgumentException();
        }
        Document statusDoc = null;
        DocumentBuilder parser = getDocumentBuilder();
        synchronized (parser) {
            statusDoc = parser.newDocument();
        }
        MapNamespaceContext nsc = this.getMapNamespaceContext();
        
        String statusNS = "http://dlib.indiana.edu/lib/xml/infrastructure/status";
        
        // Insert status root element
        Element statusRoot = (Element) statusDoc.createElementNS(statusNS, "status:status");
        statusRoot.setAttributeNS(nsc.getNamespaceURI("xsi"), "xsi:schemaLocation", statusNS + " " + nsc.getSchemaLocation(statusNS));
        statusDoc.appendChild(statusRoot);
        
        Element recordStatus = (Element) statusDoc.createElementNS(statusNS, "status:recordStatus");
        recordStatus.appendChild(statusDoc.createTextNode(status.value));
        statusRoot.appendChild(recordStatus);
        
        this.addOrReplaceDatastreamWithDocument(pid, STATUS_DS_ID, ControlGroup.M, "DLP Status", "text/xml", statusDoc, "iudl-status.xml");
    }
    
    /**
     * Generates a DC record for the given object and stores
     * it (if different) in the object specified by the given
     * pid.
     */
    private void generateAndAddDC(String pid, String label, String purl) throws ParserConfigurationException, FedoraException {
        if (this.readOnly) {
            throw new FedoraIllegalAccessException("This FedoraClient is READ ONLY!");
        }
        if (!PURLLogic.couldBePURL(purl)) {
            throw new IllegalArgumentException(purl + " does not appear to be a valid PURL.");
        }
        
        // update or add the DC datastream
        Document dcDoc = null;
        DocumentBuilder parser = getDocumentBuilder();
        synchronized (parser) {
            dcDoc = parser.newDocument();
        }

        MapNamespaceContext nsc = this.getMapNamespaceContext();
        
        // Insert dc root element
        Element dcRoot = (Element) dcDoc.createElementNS(nsc.getNamespaceURI("oai_dc"), "oai_dc:dc");
        dcRoot.setAttributeNS(nsc.getNamespaceURI("xsi"), "xsi:schemaLocation", nsc.getNamespaceURI("oai_dc") + " " + nsc.getSchemaLocation(nsc.getNamespaceURI("oai_dc")));
        dcDoc.appendChild(dcRoot);
        
        // add the dc title
        if (label != null) {
            Element dcTitle = (Element) dcDoc.createElementNS(nsc.getNamespaceURI("dc"), "dc:title");
            dcTitle.appendChild(dcDoc.createTextNode(label));
            dcRoot.appendChild(dcTitle);
        }
        
        // add the dc identifer containing the PURL (needed for search functionality)
        Element dcIdPurl = (Element) dcDoc.createElementNS(nsc.getNamespaceURI("dc"), "dc:identifier");
        dcIdPurl.appendChild(dcDoc.createTextNode(purl));
        dcRoot.appendChild(dcIdPurl);
        
        // add the dc identifier containing the PID (needed for search functionality)
        Element dcIdPID = (Element) dcDoc.createElementNS(nsc.getNamespaceURI("dc"), "dc:identifier");
        dcIdPID.appendChild(dcDoc.createTextNode(pid));
        dcRoot.appendChild(dcIdPID);

        this.addOrReplaceDatastreamWithDocument(pid, "DC", ControlGroup.M, "Dublin Core Metadata", "text/xml", dcDoc, "dc.xml");
    }
}
