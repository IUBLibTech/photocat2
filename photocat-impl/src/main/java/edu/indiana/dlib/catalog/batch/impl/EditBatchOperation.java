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
package edu.indiana.dlib.catalog.batch.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.indiana.dlib.catalog.accesscontrol.UserInfo;
import edu.indiana.dlib.catalog.asynchronous.Dialog;
import edu.indiana.dlib.catalog.asynchronous.Operation;
import edu.indiana.dlib.catalog.config.CollectionConfiguration;
import edu.indiana.dlib.catalog.config.FieldConfiguration;
import edu.indiana.dlib.catalog.config.FieldData;
import edu.indiana.dlib.catalog.config.Item;
import edu.indiana.dlib.catalog.config.ItemManager;
import edu.indiana.dlib.catalog.config.ItemMetadata;
import edu.indiana.dlib.catalog.config.NameValuePair;
import edu.indiana.dlib.catalog.config.OptimisticLockingException;
import edu.indiana.dlib.catalog.config.RepositoryException;

/**
 * An asynchronous Operation implementation that updates 
 * a batch of records.
 */
public class EditBatchOperation implements Operation {
    
    private Logger LOGGER = Logger.getLogger(EditBatchOperation.class);
    
    /**
     * The list of ids to edit.  This was provided to the
     * constructor and isn't changed.
     */
    private List<String> idsToUpdate;
    
    /**
     * The new values that should be applied to all items (provided
     * to the constructor and unchanged).
     */
    private List<FieldData> changesToMake;
    
    private CollectionConfiguration collectionConfiguration;
    
    /**
     * Id values for items successfully updated.
     */
    private List<String> updatedIds;

    /**
     * Id values for items that did not require changes (likely because
     * the batch specified values already present in that item).
     */
    private List<String> idsRequiringNoChanges;
    
    /**
     * Id values for items that haven't been updated because they 
     * require approval.
     */
    private List<String> idsRequiringApproval;

    /**
     * The list of ids for items that required approval, and for 
     * which approval was granted.
     */
    private List<String> approvedIds;
    
    /**
     * The list of ids for items that required approval but were explicitly
     * not approved.
     */
    private List<String> disapprovedIds;
    
    /**
     * Ids for which an exception was thrown while processing.
     */
    private List<String> exceptionIds;

    private boolean triggerAbort;
    
    private ItemManager im;
    
    private Thread sleepingThread;
    
    private UserInfo user;

    private Dialog pendingDialog;

    private Set<String> fieldTypesRequiringPrompt;
    
    private Set<String> fieldTypesToOverwrite;
    
    private Set<String> repeatableFieldTypesToCombine;
    
    private Set<String> nonRepeatableFieldTypesToAppend;
    
    private Set<String> fieldTypesToSkipIfPresent;
    
    public EditBatchOperation(List<String> idsToUpdate, List<FieldData> dataToApply, ItemManager im, UserInfo user, CollectionConfiguration collection) {
        this.idsToUpdate = new ArrayList<String>(idsToUpdate);
        this.changesToMake = new ArrayList<FieldData>(dataToApply);
        triggerAbort = false;
        this.im = im;
        this.user = user;
        collectionConfiguration = collection;
        pendingDialog = null;
        
        updatedIds = new ArrayList<String>();
        approvedIds = new ArrayList<String>();
        disapprovedIds = new ArrayList<String>();
        exceptionIds = new ArrayList<String>();
        idsRequiringApproval = new ArrayList<String>();
        idsRequiringNoChanges = new ArrayList<String>();
        
        fieldTypesRequiringPrompt = new HashSet<String>();
        fieldTypesToOverwrite = new HashSet<String>();
        repeatableFieldTypesToCombine = new HashSet<String>();
        nonRepeatableFieldTypesToAppend = new HashSet<String>();
        fieldTypesToSkipIfPresent = new HashSet<String>();
    }
    
    public void setPromptField(String type) {
        fieldTypesRequiringPrompt.add(type);
    }
    
    public void setOvewriteField(String type) {
        fieldTypesToOverwrite.add(type);
    }
    
    public void setRepeatableFieldTypesToCombine(String type) {
        repeatableFieldTypesToCombine.add(type);
    }
    
    public void setFieldTypesToSkip(String type) {
        fieldTypesToSkipIfPresent.add(type);
    }
    
    public void setNonRepeatableFieldTypesToAppend(String type) {
        nonRepeatableFieldTypesToAppend.add(type);
    }
    
    public boolean areOverwriteRulesSet() {
        return !fieldTypesToOverwrite.isEmpty() || !repeatableFieldTypesToCombine.isEmpty() || !nonRepeatableFieldTypesToAppend.isEmpty() || !fieldTypesToSkipIfPresent.isEmpty();
    }

    public List<String> getRepresentedFieldTypes() {
        List<String> types = new ArrayList<String>();
        for (FieldData data : changesToMake) {
            types.add(data.getFieldType());
        }
        return types;
    }
    
    public String getDescription() {
        // TODO: internationalize this
        return "Batch update";
    }

    public double getEstimatedPercentCompleted() {
        int processedCount = updatedIds.size() + exceptionIds.size() + idsRequiringNoChanges.size() + disapprovedIds.size();
        if (processedCount == 0) {
            return -1;
        }
        return (double) processedCount / (double) idsToUpdate.size();
    }

    public synchronized boolean requiresUserInteraction() {
        return pendingDialog != null;
    }
    
    public synchronized Dialog getInteractionDialog() {
        return pendingDialog;
    }

    public synchronized void respondToInteractionDialog(Dialog dialog, String response) {
        if (pendingDialog == null) {
            throw new IllegalStateException();
        } else if (!pendingDialog.getSuggestedResponses().contains(response)) {
            throw new IllegalArgumentException("\"" + response + "\" is not one of the valid responses!");
        } else if (response.equals(ResolveUpdateConflictsDialog.SKIP)) {
            LOGGER.info("User has elected to skip the conflicting records.");
            disapprovedIds = idsRequiringApproval;
            idsRequiringApproval = Collections.emptyList();
            sleepingThread.interrupt();
            sleepingThread = null;
        } else if (response.equals(ResolveUpdateConflictsDialog.UPDATE)) {
            LOGGER.info("User has elected to overwrite the conflicting records.");
            approvedIds = idsRequiringApproval;
            idsRequiringApproval = Collections.emptyList();
            sleepingThread.interrupt();
            sleepingThread = null;
        } else {
            throw new IllegalArgumentException("\"" + response + "\" is not a recognized response!");
        }
    }

    public void run() {
        for (int cursor = 0; cursor < idsToUpdate.size(); cursor ++) {
            if (triggerAbort) {
                break;
            }
            String id = idsToUpdate.get(cursor);
            try {
                // fetch the item
                Item item = im.fetchItemIncludingPrivateMetadata(id, collectionConfiguration);
                ItemMetadata metadata = item.getMetadata();
                
                // update any changed fields
                boolean wasUpdated = false;
                boolean requiresApproval = false;
                for (FieldData newData : changesToMake) {
                    FieldData oldData = metadata.getFieldData(newData.getFieldType());
                    if (oldData == null) {
                        // no value existed, so we'll add the new value without conflict
                        metadata.setFieldValue(newData.getFieldType(), newData);
                        wasUpdated = true;
                    } else if (fieldTypesToSkipIfPresent.contains(newData.getFieldType())) {
                        // we don't want to update this field
                    } else {
                        FieldConfiguration config = collectionConfiguration.getFieldConfiguration(newData.getFieldType());
                        if (fieldTypesToOverwrite.contains(config.getFieldType())) {
                            // simple overwrite
                            metadata.setFieldValue(newData.getFieldType(), newData);
                            wasUpdated = true;
                        } else if (config.isRepeatable() && repeatableFieldTypesToCombine.contains(config.getFieldType())) {
                            // walk through and see what values need to be added to the existing list
                            List<List<NameValuePair>> uniqueValues = getNewValues(newData.getParts(), oldData.getParts());
                            if (!uniqueValues.isEmpty()) {
                                oldData.addValues(uniqueValues);
                                wasUpdated = true;
                            }
                        } else if (!config.isRepeatable() && nonRepeatableFieldTypesToAppend.contains(config.getFieldType())) {
                            // append this value, possibly adding a space
                            if (oldData.getEnteredValueCount() > 1) {
                                // this could occur if the field was at one time
                                // repeatable, or otherwise populated with multiple
                                // values for this now single-valued field.  In this
                                // case we will SKIP updating this value.
                            } else {
                                List<NameValuePair> updatedParts = new ArrayList<NameValuePair>();
                                if (newData.getEnteredValueCount() != 1) {
                                    // this is an unsupported request... we only allow
                                    // a single value to be specified for appending to 
                                    // the existing value
                                    // SKIP updating this value
                                } else {
                                    Set<String> partsAppended = new HashSet<String>();
                                    for (NameValuePair part : oldData.getParts().get(0)) {
                                        List<String> valuesToAppend = newData.getPartValues(part.getName());
                                        if (valuesToAppend.size() != 1) {
                                            // there's no value entered in this part to append to the existing
                                            // part value
                                        } else {
                                            String suffix = " " + valuesToAppend.get(0);
                                            updatedParts.add(new NameValuePair(part.getName(), part.getValue() + suffix));
                                            partsAppended.add(part.getName());
                                            wasUpdated = true;
                                        }
                                    }
                                    // now add any parts that didn't exist before
                                    for (NameValuePair newPart : newData.getParts().get(0)) {
                                        if (!partsAppended.contains(newPart.getName())) {
                                            // add this part, trimming the value
                                            updatedParts.add(new NameValuePair(newPart.getName(), newPart.getValue().trim()));
                                            wasUpdated = true;
                                        }
                                    }
                                    oldData.getParts().set(0, updatedParts);
                                }
                            }
                        } else if (newData.equals(oldData)) { 
                            // fall through and don't worry about this field
                        } else if (fieldTypesRequiringPrompt.contains(config.getFieldType())) {
                            requiresApproval = true;
                            break;
                        } else {
                            // Unexpected case, probably an invalid combination
                            requiresApproval = true;
                            break;
                        }
                    }
                }
                
                // store the updated item
                if (requiresApproval) {
                    idsRequiringApproval.add(id);
                } else if (wasUpdated) {
                    im.saveItemMetadata(item, collectionConfiguration, user);
                    updatedIds.add(id);
                } else {
                    idsRequiringNoChanges.add(id);
                }
            } catch (OptimisticLockingException ex) {
                // We should try again, because if unrelated fields were updated 
                // our update should succeed if we try again.  (Also this is *extremely* unlikely
                // because it would require a user to save the exact item we're working on
                // in the time it takes us to read, update and write the object.
                cursor --;
                LOGGER.info("Optimistic locking exception for batch operation... will try again.", ex);
            } catch (Throwable t) {
                LOGGER.warn("Exception while updating an item in a batch operation!", t);
                exceptionIds.add(id);
            }
        }
        while (!idsRequiringApproval.isEmpty()) {
            if (triggerAbort) {
                break;
            }
            synchronized (this) {
                pendingDialog = new ResolveUpdateConflictsDialog();
            }
            LOGGER.debug("Operation sleeping, waiting for user interation");
            // NOTE: this process requires interaction... we'll sleep
            sleepingThread = Thread.currentThread();
            try {
                Thread.sleep(10000000);
            } catch (InterruptedException ex) {
                LOGGER.debug("Sleeping Operation woken up!");
                // great, maybe we're good now.
            }
        }
        
        synchronized (approvedIds) {
            for (int cursor = 0; cursor < approvedIds.size(); cursor ++) {
                String id = approvedIds.get(cursor);
                if (triggerAbort) {
                    break;
                }
                try {
                    // fetch the item
                    Item item = im.fetchItemIncludingPrivateMetadata(id, collectionConfiguration);
                    ItemMetadata metadata = item.getMetadata();
                    
                    // update any changed fields
                    boolean wasUpdated = false;
                    for (FieldData newData : changesToMake) {
                        FieldData oldData = metadata.getFieldData(newData.getFieldType());
                        if (oldData == null) {
                            // no value existed, so we'll add the new value without conflict
                            metadata.setFieldValue(newData.getFieldType(), newData);
                            wasUpdated = true;
                        } else {
                            FieldConfiguration config = collectionConfiguration.getFieldConfiguration(newData.getFieldType());
                            if (config.isRepeatable()) {
                                if (!newData.getAttributes().isEmpty() && !newData.getAttributes().equals(oldData.getAttributes())) {
                                    oldData.setAttributes(newData.getAttributes());
                                    wasUpdated = true;
                                }
                                // walk through and see what values need to be added to the existing list
                                List<List<NameValuePair>> uniqueValues = getNewValues(newData.getParts(), oldData.getParts());
                                if (!uniqueValues.isEmpty()) {
                                    oldData.addValues(uniqueValues);
                                    wasUpdated = true;
                                }
                            } else {
                                // see if we can replace the existing value or whether we need to ask for
                                // approval
                                metadata.setFieldValue(newData.getFieldType(), newData);
                                wasUpdated = true;
                            }
                        }
                    }
                    
                    // store the updated item
                    if (wasUpdated) {
                        im.saveItemMetadata(item, collectionConfiguration, user);
                        updatedIds.add(id);
                    } else {
                        idsRequiringNoChanges.add(id);
                    }
                } catch (RepositoryException ex) {
                    LOGGER.warn("Exception while updating an item in a batch operation!", ex);
                    exceptionIds.add(id);
                } catch (OptimisticLockingException ex) {
                    // We should try again, because if unrelated fields were updated 
                    // our update should succeed if we try again.  (Also this is *extremely* unlikely
                    // because it would require a user to save the exact item we're working on
                    // in the time it takes us to read, update and write the object.
                    cursor --;
                    LOGGER.info("Optimistic locking exception for batch operation... will try again.", ex);
                }
            }
        }
    }
    
    /**
     * This is synchronzied with any other method with non-atomic 
     * access to the sleepingThread object.  (ie, notifyUserInteraction()) 
     */
    public synchronized void abort() {
        triggerAbort = true;
        if (sleepingThread != null) {
            sleepingThread.interrupt();
            sleepingThread = null;
        }
    }

    
    private static List<List<NameValuePair>> getNewValues(List<List<NameValuePair>> possibleValues, List<List<NameValuePair>> currentValues) {
        List<List<NameValuePair>> newValues = new ArrayList<List<NameValuePair>>();
        for (List<NameValuePair> possibleValue : possibleValues) {
            if (!alreadyHasValue(possibleValue, currentValues)) {
                newValues.add(possibleValue);
            }
        }
        return newValues;
    }
    
    private static boolean alreadyHasValue(List<NameValuePair> value, List<List<NameValuePair>> currentValues) {
        for (List<NameValuePair> currentValue : currentValues) {
            if (currentValue.containsAll(value) && value.containsAll(currentValue)) {
                return true;
            }
        }
        return false;
    }
    
    private class ResolveUpdateConflictsDialog implements Dialog {

        private static final String UPDATE = "Update and Overwrite";
        private static final String SKIP = "Skip these Records";
        
        public String getMessageFromOperation() {
            StringBuffer sb = new StringBuffer();
            if (updatedIds.size() == 1) {
                sb.append("One record was updated");
            } else if (updatedIds.size() > 1) {
                sb.append(updatedIds.size() + " records were updated");
            }
            if (idsRequiringNoChanges.size() == 1) {
                if (sb.length() > 0) {
                    sb.append(", one record was unaffected by this update");
                } else {
                    sb.append("One record was unaffected by this update");
                }
            } else if (idsRequiringNoChanges.size() > 1) {
                if (sb.length() > 0) {
                    sb.append(", " + idsRequiringNoChanges.size() + " records were unaffected by this update");
                } else {
                    sb.append(idsRequiringNoChanges.size() + " records were unaffected by this update");
                }
            }
            if (exceptionIds.size() == 1) {
                if (sb.length() > 0) {
                    sb.append(", one record update failed due to a system error");
                } else {
                    sb.append("One record update failed due to a system error");
                }
            } else if (exceptionIds.size() > 1) {
                if (sb.length() > 0) {
                    sb.append(", " + exceptionIds.size() + " record updates failed due to system errors");
                } else {
                    sb.append(exceptionIds.size() + " record updates failed due to system errors");
                }
            }
            if (idsRequiringApproval.size() == 1) {
                if (sb.length() > 0) {
                    sb.append(" but one record had a value in an field that would be overwritten.");
                } else {
                    sb.append("One record has values in in a field that would be overwritten.");
                }
            } else if (idsRequiringApproval.size() > 1) {
                if (sb.length() > 0) {
                    sb.append(" but " + idsRequiringApproval.size() + " records had values in an field that would be overwritten.");
                } else {
                    sb.append(idsRequiringApproval.size() + " records had values in in a field that would be overwritten.");
                }
            }
            return sb.toString();
        }

        public Operation getOperation() {
            return EditBatchOperation.this;
        }

        public List<String> getSuggestedResponses() {
            return Arrays.asList(UPDATE, SKIP);
        }
        
    }
    
}
