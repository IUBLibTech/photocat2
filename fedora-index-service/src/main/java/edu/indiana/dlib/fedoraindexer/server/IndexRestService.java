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
package edu.indiana.dlib.fedoraindexer.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Properties;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import fedora.client.messaging.JmsMessagingClient;
import fedora.client.messaging.MessagingClient;
import fedora.client.messaging.MessagingListener;
import fedora.server.errors.MessagingException;
import fedora.server.messaging.AtomAPIMMessage;
import fedora.server.utilities.DateUtility;

public class IndexRestService extends HttpServlet implements MessagingListener {

    public static Logger LOGGER = Logger.getLogger(IndexRestService.class);
    
    private IndexManager im;
    
    private MessagingClient messagingClient;
    
    private IndexerStats indexerStats;
    
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        this.indexerStats = new IndexerStats();
        String configFilename = config.getInitParameter("indexManagerProperties");
        if (configFilename == null) {
            LOGGER.fatal("Requred parameter: 'indexManagerProperties' missing!");
            throw new ServletException("Requred parameter: 'indexManagerProperties' missing!");
        }
        // To allow this to be run in the same tomcat instance as FEDORA
        // and to perhaps start before fedora loads, requires that 
        // this initialization be a non-blocking action.
        Thread t = new Thread(new Runnable() {
            public void run() {
                startFedoraDependantOperations();
            }});
        t.start();
    }
    
    public void destroy() {
        if (this.messagingClient != null) {
            LOGGER.info("Stopping messaging client...");
            try {
                this.messagingClient.stop(false);
                LOGGER.info("Messaging client stopped.");
            } catch (MessagingException ex) {
                LOGGER.error("Unable to stop messaging client!", ex);
            }
        }
        super.destroy();
    }
    
    /**
     * Restarts the messaging client.
     * @throws MessagingException 
     */
    private void restartMessagingClient() {
        try {
            if (this.messagingClient != null) {
                LOGGER.info("Stopping messaging client...");
                this.messagingClient.stop(false);
                LOGGER.info("Messaging client stopped.");
                LOGGER.info("Starting messaging client...");
                this.messagingClient.start();
                LOGGER.info("Messaging client started!");
            } else {
                LOGGER.warn("Request to restart messaging client ignored because no messaging client is configured for this instance!");
            }
        } catch (Throwable t) {
            LOGGER.error("Error restarting messaging client!", t);
        }
    }
    
    
    /**
     * <p>
     *   Processes a URL/based request for this index service.  The
     *   following parameters are expected:
     *   <ul>
     *     <li>operation - either add, update, remove, reload or status</li>
     *     <li>pid - the fedora pid of the object to be added, removed or updated</li>
     *     <li>lastModificationDate - the date the object was last modified</li>
     *   </ul>
     * </p>
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        long start = System.currentTimeMillis();
        String operation = request.getParameter("operation");
        if (operation != null) {
            this.indexerStats.restCallCount ++;
        }
        if (operation != null && operation.equalsIgnoreCase("optimize")) {
            this.im.optimize();
        } else if (operation != null && operation.equalsIgnoreCase("add")) {
            this.im.indexObject(Index.Operation.ADD, request.getParameter("pid"), request.getParameter("lastModificationDate"));
            this.indexerStats.addCount ++;
        } else if (operation != null && operation.equalsIgnoreCase("remove")) {
            this.im.indexObject(Index.Operation.REMOVE, request.getParameter("pid"), request.getParameter("lastModificationDate"));
            this.indexerStats.removeCount ++;
        } else if (operation != null && operation.equalsIgnoreCase("update")) {
            this.im.indexObject(Index.Operation.UPDATE, request.getParameter("pid"), request.getParameter("lastModificationDate"));
            this.indexerStats.updateCount ++;
        } else if (operation != null && operation.equalsIgnoreCase("reload")) {
            this.restartMessagingClient();
        } else if (operation != null && operation.equalsIgnoreCase("status")) {
            response.setContentType("text/xml");
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(response.getOutputStream()));
            writer.println("<indexer-status>");
            writer.println("  <updates>" + this.indexerStats.updateCount + "</updates>");
            writer.println("  <adds>" + this.indexerStats.addCount + "</adds>");
            writer.println("  <removes>" + this.indexerStats.removeCount + "</removes>");
            writer.println("  <updates>" + this.indexerStats.updateCount + "</updates>");
            writer.println("  <rest-api-calls>" + this.indexerStats.restCallCount + "</rest-api-calls>");
            writer.println("  <jms-messages-received>" + this.indexerStats.messagesReceivedCount + "</jms-messages-received>");
            writer.println("  <jms-last-message-date>" + this.indexerStats.lastMessageDate + "</jms-last-message-date>");
            writer.println("</indexer-status>");
            writer.flush();
            writer.close();
            response.setStatus(HttpServletResponse.SC_OK);
        } else {
            LOGGER.warn("An invalid operation \"" + operation + "\", made by " + (request.getRemoteUser() == null ? "an unauthenticated user" : request.getRemoteUser()) + " from " + request.getRemoteAddr() + " was ignored.");
        }
        long end = System.currentTimeMillis();
        long time = end - start;
        LOGGER.info("Operation \"" + operation + "\", made by " + (request.getRemoteUser() == null ? "an unauthenticated user" : request.getRemoteUser()) + " from " + request.getRemoteAddr() + " was processed in " + time + "ms.");
    }
    
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        this.doGet(request, response);
    }

    public void onMessage(String clientId, Message message) {
        LOGGER.debug("onMessage [start]");
        try {
            this.indexerStats.lastMessageDate = new Date();
            this.indexerStats.messagesReceivedCount ++;
            try {
                String messageText = "";
                if (message instanceof TextMessage) {
                    try {
                        messageText = ((TextMessage) message).getText();
                    } catch (JMSException jmse) {
                        LOGGER.error("Unable to retrieve text from update message, "
                                + "message cannot be processed:" + message. toString());
                        return;
                    }
                } else {
                    LOGGER.warn("Receieved non-text message in UpdateListener, "
                            + "message was of type " + message.getClass());
                    return;
                }
        
                AtomAPIMMessage apimMessage = new AtomAPIMMessage(messageText);
                if (apimMessage.getMethodName().contains("purgeObject")) {
                    // purge
                    this.im.indexObject(Index.Operation.REMOVE, apimMessage.getPID(), DateUtility.convertDateToString(apimMessage.getDate()));
                    this.indexerStats.removeCount ++;
                } else if (apimMessage.getMethodName().startsWith("modify")) {
                    // update
                    this.im.indexObject(Index.Operation.UPDATE, apimMessage.getPID(), DateUtility.convertDateToString(apimMessage.getDate()));
                    this.indexerStats.updateCount ++;
                } else if (apimMessage.getMethodName().startsWith("addDatastream")) {
                    // add (datastream)
                    this.im.indexObject(Index.Operation.UPDATE, apimMessage.getPID(), DateUtility.convertDateToString(apimMessage.getDate()));
                    this.indexerStats.updateCount ++;
                } else if (apimMessage.getMethodName().toLowerCase().indexOf("relationship") != -1) {
                    // add/remove relationship
                    this.im.indexObject(Index.Operation.UPDATE, apimMessage.getPID(), DateUtility.convertDateToString(apimMessage.getDate()));
                    this.indexerStats.updateCount ++;
                } else if (apimMessage.getMethodName().startsWith("ingest")) {
                    // add
                    this.im.indexObject(Index.Operation.ADD, apimMessage.getPID(), DateUtility.convertDateToString(apimMessage.getDate()));
                    this.indexerStats.addCount ++;
                } else {
                    LOGGER.debug("Unprocessed message " + apimMessage.getMethodName() + " for " + apimMessage.getPID() + " at " + DateUtility.convertDateToString(apimMessage.getDate()) + " received.");
                }
            } catch (Throwable t) {
                LOGGER.error("Exception while processing message! (message skipped)", t);
            }
        } finally {
             LOGGER.debug("onMessage [end]");
        }
    }
    
    private void startFedoraDependantOperations() {
        ServletConfig config = super.getServletConfig();
        String configFilename = config.getInitParameter("indexManagerProperties");
        FileInputStream fis = null;
        try {
            File configFile = new File(config.getServletContext().getRealPath(configFilename));
            if (!configFile.exists()) {
                configFile = new File(configFilename);
            }
            fis = new FileInputStream(configFile);
            final Properties p = new Properties();
            p.load(fis);
            LOGGER.info("Starting IndexManager...");
            this.im = new IndexManager(p, configFile.getParentFile());
            LOGGER.info("IndexManager started!");
            if (p.containsKey("java.naming.factory.initial")) {
                String name = p.containsKey("messaging.client.name") ? p.getProperty("messaging.client.name") : "IndesServiceMessagingClient-" + (int) (Math.random() * 10000);
                LOGGER.info("Starting Messaging Client... [" + name + "]");
                this.messagingClient = new JmsMessagingClient(name, this, p, false);
                this.messagingClient.start();
                LOGGER.info("Messaging Client started!");
            } else {
                LOGGER.warn("Messaging Client not configured!  This fedora-index-service will only respond to REST calls!");
            }
        } catch (FileNotFoundException ex) {
            LOGGER.fatal("Unable to locate file: " + new File(configFilename).getAbsolutePath(), ex);
        } catch (IOException ex) {
            LOGGER.fatal("Unable to load file: " + new File(configFilename).getAbsolutePath(), ex);
        } catch (MessagingException ex) {
            LOGGER.error("Unable to instantiate a messaging client!", ex);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ex) {
                    LOGGER.fatal("Unable to close file: " + new File(configFilename).getAbsolutePath(), ex);
                }
            }
        }
    }
}
