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
package edu.indiana.dlib.catalog.dataimport.operations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;

import edu.indiana.dlib.catalog.accesscontrol.UserInfo;
import edu.indiana.dlib.catalog.asynchronous.Dialog;
import edu.indiana.dlib.catalog.asynchronous.Operation;
import edu.indiana.dlib.catalog.config.CollectionConfiguration;
import edu.indiana.dlib.catalog.config.Item;
import edu.indiana.dlib.catalog.config.ItemManager;
import edu.indiana.dlib.catalog.dataimport.FieldMapping;
import edu.indiana.dlib.catalog.dataimport.Record;
import edu.indiana.dlib.catalog.dataimport.Records;

public class RecordImportOperation implements Operation {

    private Logger LOGGER = Logger.getLogger(RecordImportOperation.class);
    
    private Records records;
    
    private FieldMapping mapping;

    private String description;
    
    private ItemManager im;
    
    private CollectionConfiguration collectionConfig;
    
    private UserInfo user;
    
    private boolean started;
    
    private boolean abort;
    
    private String interactionRedirect;
    
    private List<String> errorIds;
    
    private StringBuffer exceptionMessages;
    
    private List<String> unresolvedIds;
    
    private List<String> updatedIds;
    
    private Dialog pendingDialog;
    
    public RecordImportOperation(String description, Records records, FieldMapping mapping, ItemManager im, UserInfo user, CollectionConfiguration collectionConfiguration, String interactionRedirect) {
        this.description = description;
        this.records = records;
        this.mapping = mapping;
        this.im = im;
        this.user = user;
        collectionConfig = collectionConfiguration;
        abort = false;
        pendingDialog = null;
    }
    
    public String getDescription() {
        return description;
    }

    /**
     * The method that performs the work of this operation.
     * @extends Runnable
     */
    public void run() {
        started = true;
        errorIds = new ArrayList<String>();
        exceptionMessages = new StringBuffer();
        unresolvedIds = new ArrayList<String>();
        updatedIds = new ArrayList<String>();
        for (Record record : records) {
            if (abort) {
                break;
            } else {
                String id = mapping.getId(record);
                if (id == null || id.trim().length() == 0) {
                    // skip this row because the id isn't specified
                } else {
                    try {
                        Item item = im.fetchItemIncludingPrivateMetadata(id, collectionConfig);
                        if (item == null) {
                            unresolvedIds.add(id);
                        } else {
                            mapping.updatedItemMetadata(record, item.getMetadata());
                            im.saveItemMetadata(item, collectionConfig, user);
                            updatedIds.add(id);
                        }
                    } catch (Throwable t) {
                        errorIds.add(id);
                        LOGGER.warn("Error for import of data for item " + id + "!", t);
                        if (t.getMessage() != null) {
                            exceptionMessages.append(id + ": " + t.getMessage() + "\n");
                        }
                    }
                }
            }
        }
        pendingDialog = new OverwriteRecordsDialog();
        
        // send an e-mail
        try {
            sendEmail(user.getEmailAddress(), "photocat-do-not-reply@localhost", "Photocat Import Operation", pendingDialog.getMessageFromOperation(), "localhost");
        } catch (Throwable t) {
            Logger.getLogger(this.getClass()).warn("Error sending e-mail!", t);
        }
    }
    
    public static void sendEmail(String to, String from, String subject, String body, String smtpHost) throws AddressException, MessagingException {
        Properties props = System.getProperties();
        props.put("mail.smtp.host", smtpHost);
        Session session = Session.getDefaultInstance(props, null);
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
        message.setSubject(subject);
        message.setText(body);
        Transport.send(message);
    }
    
    public void abort() {
        abort = true;
    }
    
    /**
     * Because the underlying implementation of the Records may
     * be streaming the content, the total count of records is 
     * unknown and no estimate can be made.
     * @returns -1 always to indicate that no estimate can be
     * made
     */
    public double getEstimatedPercentCompleted() {
        return -1;
    }
    
    public boolean hasStarted() {
        return started;
    }

    /**
     * User interaction is requested
     */
    public synchronized boolean requiresUserInteraction() {
        return pendingDialog != null;
    }
    
    public String getInteractionRedirect() {
        if (requiresUserInteraction()) {
            return interactionRedirect;
        } else {
            return null;
        }
    }

    public Dialog getInteractionDialog() {
        return pendingDialog;
    }

    public synchronized void respondToInteractionDialog(Dialog dialog, String response) {
        if (pendingDialog == null) {
            throw new IllegalStateException();
        } else if (!pendingDialog.getSuggestedResponses().contains(response)) {
            throw new IllegalArgumentException("\"" + response + "\" is not one of the valid responses!");
        } else {
            pendingDialog = null;
        }
        
    }
    
    private class OverwriteRecordsDialog implements Dialog {

        /**
         * Builds an English sentence that summarizes the situation and asks
         * a question.
         * TODO: this needs to be internationalized
         */
        public String getMessageFromOperation() {
            StringBuffer sb = new StringBuffer();
            if (updatedIds.size() == 1) {
                sb.append("One record was updated");
            } else if (updatedIds.size() > 1) {
                sb.append(updatedIds.size() + " records were updated");
            }
            if (unresolvedIds.size() == 1) {
                if (sb.length() > 0) {
                    sb.append(", one record identified in the batch was not found");
                } else {
                    sb.append("One record identified in the batch was not found");
                }
            } else if (unresolvedIds.size() > 1) {
                if (sb.length() > 0) {
                    sb.append(", " + unresolvedIds.size() + " records identified in the batch were not found");
                } else {
                    sb.append(unresolvedIds.size() + " records identified in the batch were not found");
                }
            }
            if (errorIds.size() == 1) {
                if (sb.length() > 0) {
                    sb.append(", one record update failed due to a system error");
                } else {
                    sb.append("One record update failed due to a system error");
                }
            } else if (errorIds.size() > 1) {
                if (sb.length() > 0) {
                    sb.append(", " + errorIds.size() + " record updates failed due to system errors");
                } else {
                    sb.append(errorIds.size() + " record updates failed due to system errors");
                }
            }
            sb.append(".");
            if (exceptionMessages.length() > 0) {
                sb.append("\n" + exceptionMessages.toString());
            }
            return sb.toString();

        }

        public Operation getOperation() {
            return RecordImportOperation.this;
        }

        public List<String> getSuggestedResponses() {
            return Collections.singletonList("OK");
        }
        
    }

}
