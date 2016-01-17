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
package edu.indiana.dlib.fedora.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import edu.indiana.dlib.fedora.client.MapNamespaceContext;
import edu.indiana.dlib.fedora.client.iudl.DLPFedoraClient;
import edu.indiana.dlib.fedora.client.iudl.PURLLogic;

/**
 * A command line utility that fetches all old versions of a given
 * datastream for a fedora object identified by a given PID.
 */
public class FetchHistoricalVersions {

    private static DateFormat FEDORA_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    static {
        FEDORA_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
    }
    private static DateFormat FILE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HHmm");
    
    public static void main(String[] args) throws Exception {
        System.out.println("FetchHistoricalVersions");
        if (args.length != 7) {
            System.out.println("The following 7 arguments are expected:");
            System.out.println("  fedora host name (ie. localhost)");
            System.out.println("  port (ie. 8080)");
            System.out.println("  appName (ie. fedora)");
            System.out.println("  pid or fullItemId");
            System.out.println("  dsName");
            System.out.println("  username");
            System.out.println("  password");
            return;
        }
        String serverName = args[0];
        int port = Integer.parseInt(args[1]);
        String appName = args[2];
        String pid = null;
        String fullItemId = null;
        String itemId = null;
        String dsName = args[4];
        String username = args[5];
        String password = args[6];
        
        // Parse FOXML
        DocumentBuilderFactory dbFac = DocumentBuilderFactory.newInstance();
        dbFac.setNamespaceAware(true);
        DocumentBuilder parser = dbFac.newDocumentBuilder();
        
        DLPFedoraClient fc = new DLPFedoraClient(username, password, serverName, appName, port, true);
        
        if (args[3].contains(":")) {
            pid = args[3];
            try {
                fullItemId = PURLLogic.getFullItemIdFromDefaultPURL(fc.getPURL(pid));
            } catch (Throwable t) {
                fullItemId = fc.getPURL(pid);
            }
        } else {
            fullItemId = args[3];
            pid = fc.getPidForPURL(PURLLogic.getDefaultPURL(fullItemId));
        }
        try {
            itemId = PURLLogic.getItemIdFromDefaultPURL(fc.getPURL(pid));
        } catch (Throwable t) {
            itemId = "unknown";
        }
        
        Document foxmlDoc = fc.exportObjectAsDocument(pid);
        
        // Extract ds versions
        MapNamespaceContext nsc = new MapNamespaceContext();
        nsc.setNamespace("foxml", "info:fedora/fedora-system:def/foxml#");
        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(nsc);
        
        NodeList nl = (NodeList) xpath.evaluate("foxml:digitalObject/foxml:datastream[@ID='" + dsName + "']/foxml:datastreamVersion", foxmlDoc, XPathConstants.NODESET);
        if (nl != null) {
            for (int i = 0; i < nl.getLength(); i ++) {
                Element dsVerEl = (Element) nl.item(i);
                String dateStr = dsVerEl.getAttribute("CREATED");
                Date date = FEDORA_DATE_FORMAT.parse(dateStr);
                File versionFile = new File(itemId + "_" + dsName + "_" + FILE_DATE_FORMAT.format(date) + ".xml");
                FileOutputStream fos = new FileOutputStream(versionFile);
                fc.pipeDatastream(pid, dsName, dateStr, fos);
                fos.close();
                System.out.println(versionFile.getName());
            }
        }
    }
    
}
