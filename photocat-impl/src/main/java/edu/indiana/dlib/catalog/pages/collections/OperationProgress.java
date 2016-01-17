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
package edu.indiana.dlib.catalog.pages.collections;

import java.util.ArrayList;
import java.util.List;

import org.apache.click.control.Form;
import org.apache.click.control.Submit;

import edu.indiana.dlib.catalog.asynchronous.Dialog;
import edu.indiana.dlib.catalog.asynchronous.Operation;
import edu.indiana.dlib.catalog.asynchronous.UserOperationManager;

/**
 * <p>
 *   A page to view a summary of the running and queued operations
 *   and to interact with those operations that require interaction.
 * </p>
 */
public class OperationProgress extends CollectionPage {

    public Dialog dialog;
    
    public Form operationForm;
    
    /**
     * The submit buttons for the interaction proposed by 
     * the 'operation'.
     */
    private List<Submit> interactionSubmitList;
    
    public List<Operation> otherOperations;
    
    public void onInit() {
        super.onInit();
        UserOperationManager om = UserOperationManager.getOperationManager(getContext().getRequest(), user.getUsername());
        otherOperations = new ArrayList<Operation>();
        for (Operation operation : om.listIncompleteOperations()) {
            synchronized (operation) {
                if (dialog == null && operation.requiresUserInteraction()) {
                    int i = 0;
                    dialog = operation.getInteractionDialog();
                    Form operationForm = new Form("operationForm");
                    interactionSubmitList = new ArrayList<Submit>();
                    for (String response : dialog.getSuggestedResponses()) {
                        Submit submit = new Submit("response_" + (i ++), response, this, "onConfirm");
                        interactionSubmitList.add(submit);
                        operationForm.add(submit);
                    }
                    addControl(operationForm);
                } else {
                    otherOperations.add(operation);
                }
            }
        }
    }
    
    public boolean onConfirm() {
        for (int i = 0; i < interactionSubmitList.size(); i ++) {
            Submit button = interactionSubmitList.get(i);
            if (button.isClicked()) {
                dialog.getOperation().respondToInteractionDialog(dialog, button.getValue());
            }
        }
        setRedirect("display-collection.htm");
        return false;
    }
    
}
