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
package edu.indiana.dlib.catalog.fields.click.control;

import java.util.Collection;

import org.apache.click.control.AbstractContainer;
import org.apache.click.control.Label;
import org.apache.click.control.TextField;

import edu.indiana.dlib.catalog.config.FieldData;
import edu.indiana.dlib.catalog.config.NameValuePair;

public class UnrecognizedFieldDataContainer extends AbstractContainer {
    
    public UnrecognizedFieldDataContainer(FieldData data) {
        super(data.getFieldType());
        
        for (NameValuePair attribute : data.getAttributes()) {
            this.add(new Label(data.getFieldType() + "-" + attribute.getName()));
            TextField field = new TextField(attribute.getName());
            field.setValue(attribute.getValue());
            field.setReadonly(true);
            this.add(field);
        }
        
        int i = 0;
        for (Collection<NameValuePair> parts : data.getParts()) {
            for (NameValuePair part : parts) {
                TextField field = new TextField(data.getFieldType() + "-" + part.getName() + i);
                field.setValue(part.getValue());
                field.setReadonly(true);
                this.add(field);
            }
            i ++;
        }
    }

}
