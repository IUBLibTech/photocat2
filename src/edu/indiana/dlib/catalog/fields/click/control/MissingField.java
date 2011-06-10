/*
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
package edu.indiana.dlib.catalog.fields.click.control;

import org.apache.click.control.AbstractContainer;
import org.apache.click.control.Label;

import edu.indiana.dlib.catalog.config.ConfigurationException;
import edu.indiana.dlib.catalog.config.FieldConfiguration;
import edu.indiana.dlib.catalog.config.FieldDefinition;

/**
 * An Control that displays information about an UIField
 * that *should* have been displayed but is not because of
 * an error.  Though the current implementation only displays
 * a brief message, this class could be expanded to incorporate
 * an interactive reporting feature.
 *
 */
public class MissingField extends AbstractContainer {

    public MissingField(FieldConfiguration config, FieldDefinition def, Throwable t) {
        super();
        Label label = null;
        String message = getRootCauseMessage(t);
        if (message == null) {
            message = "";
        } else {
            message = " (" + message + ")";
        }
        if (def == null) {
            label = new Label("error", "<p class=\"errorMessage\">" + config.getFieldType() + " is not a defined field type!</p>");
        } else if (t instanceof ConfigurationException) {
            if (t.getCause() instanceof ClassNotFoundException) {
                label = new Label("error", "<p class=\"errorMessage\">" + config.getFieldType() + " field is not implemented." + message + "</p>");
            } else {
                label = new Label("error", "<p class=\"errorMessage\">" + config.getFieldType() + " field is improperly configured!" + message + "</p>");
            }
        } else {
            label = new Label("error", "<p class=\"errorMessage\">A system error has prevented " + config.getFieldType() + " field from being displayed!" + message + "</p>");
        }
        this.add(label);
    }
    
    private String getRootCauseMessage(Throwable t) {
        if (t.getCause() != null) {
            String message = getRootCauseMessage(t.getCause());
            if (message == null) {
                return t.getLocalizedMessage();
            } else {
                return message;
            }
        } else {
            return t.getLocalizedMessage();
        }
    }
}
