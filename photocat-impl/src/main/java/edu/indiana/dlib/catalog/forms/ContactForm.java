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
package edu.indiana.dlib.catalog.forms;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.click.control.Field;
import org.apache.click.control.Form;
import org.apache.click.control.Submit;
import org.apache.click.control.TextArea;
import org.apache.click.extras.control.EmailField;
import org.apache.click.util.ContainerUtils;
import org.apache.click.util.HtmlStringBuffer;

import edu.indiana.dlib.catalog.fields.click.control.recaptcha.ReCAPTCHAField;

public class ContactForm extends Form {

    private String contactEmailAddress;
    
    private EmailField fromEmailAddressField;
    
    private TextArea commentTextArea;
    
    private boolean wasSubmitted;
    
    private List<String> extraFieldNames;
    
    private Map<String, Field> extraFieldMap;
    
    public ContactForm(String name, String contactEmailAddress) {
        super(name);
        this.contactEmailAddress = contactEmailAddress;
        
        fromEmailAddressField = new EmailField("address", getMessage("address"));
        fromEmailAddressField.setRequired(true);
        fromEmailAddressField.setSize(32);
        add(fromEmailAddressField);
        
        commentTextArea = new TextArea("comments", getMessage("comments"));
        commentTextArea.setRequired(true);
        commentTextArea.setRows(10);
        commentTextArea.setCols(48);
        add(commentTextArea);
        
        ReCAPTCHAField captcha = new ReCAPTCHAField(getContext().getServletContext().getInitParameter("recaptchaPrivateKey"), getContext().getServletContext().getInitParameter("recaptchaPublicKey"));
        add(captcha);
        
        add(new Submit("sendComments", getMessage("send-comments"), this, "sendEmail"));
        wasSubmitted = false;
    }
    
    public void addExtraField(Field field, String nameInMessage) {
        if (extraFieldMap == null) {
            extraFieldNames = new ArrayList<String>();
            extraFieldMap = new HashMap<String, Field>();
        }
        if (extraFieldNames.contains(nameInMessage)) {
            throw new IllegalArgumentException(nameInMessage + " is already registered!");
        }
        extraFieldNames.add(nameInMessage);
        extraFieldMap.put(nameInMessage, field);
        add(field);
    }
    
    public boolean wasSubmitted() {
        return wasSubmitted;
    }
    
    public boolean sendEmail() {
        if (isValid()) {
            try {
                //set up mail-properties
                Properties props = new Properties();
                props.put("mail.smtp.host", "127.0.0.1");
                Session s = Session.getInstance(props, null);
                
                //set-up message
                MimeMessage message = new MimeMessage(s);
                
                //set from
                message.setFrom(new InternetAddress(fromEmailAddressField.getValue()));
                
                //set to
                if (contactEmailAddress.indexOf(',') != -1) {
                    for (String address : contactEmailAddress.split(",")) {
                        message.addRecipient(Message.RecipientType.BCC, new InternetAddress(address));
                    }
                } else {
                    message.addRecipient(Message.RecipientType.BCC, new InternetAddress(contactEmailAddress));
                }
                message.addRecipient(Message.RecipientType.BCC, new InternetAddress(fromEmailAddressField.getValue()));
                
                //set subject
                message.setSubject("Collection Feedback");
                
                //set message itself
                StringBuffer messageContent = new StringBuffer();
                messageContent.append("A user identifying him/herself with the e-mail address \"" + fromEmailAddressField.getValue() + "\" has entered the following comments:\n\n");
                messageContent.append(commentTextArea.getValue() + "\n");
                if (extraFieldNames != null) {
                    for (String n : extraFieldNames) {
                        String value = extraFieldMap.get(n).getValue();
                        if (value != null) {
                            messageContent.append("\n" + n + ": " + value);
                        }
                    }
                }
                message.setContent(messageContent.toString(), "text/plain");
        
                //set date
                message.setSentDate(new Date());
                Transport.send(message);
                wasSubmitted = true;
                return true;
            } catch (MessagingException ex) {
                throw new RuntimeException(ex);
            }
        } else {
            return true;
        }
    }
    
    
    /**
     * Render the given form start tag and the form hidden fields to the given
     * buffer.
     *
     * @param buffer the HTML string buffer to render to
     * @param formFields the list of form fields
     */
    protected void renderHeader(HtmlStringBuffer buffer, List<Field> formFields) {

        buffer.elementStart(getTag());

        buffer.appendAttribute("method", getMethod());
        buffer.appendAttribute("id", getId());
        buffer.appendAttribute("action", getActionURL());
        buffer.appendAttribute("enctype", getEnctype());

        appendAttributes(buffer);

        if (isJavaScriptValidation()) {
            String javaScript = "return on_" + getId() + "_submit();";
            buffer.appendAttribute("onsubmit", javaScript);
        }
        buffer.closeTag();
        buffer.append("\n");

        // render fieldset open tag
        buffer.elementStart("fieldset");
        buffer.closeTag();
        
        // Render hidden fields
        for (Field field : ContainerUtils.getHiddenFields(this)) {
            field.render(buffer);
            buffer.append("\n");
        }
    }
    
    /**
     * Close the form tag and render any additional content after the Form.
     * <p/>
     * Additional content includes <tt>javascript validation</tt> and
     * <tt>javascript focus</tt> scripts.
     *
     * @param formFields all fields contained within the form
     * @param buffer the buffer to render to
     */
    protected void renderTagEnd(List<Field> formFields, HtmlStringBuffer buffer) {

        buffer.elementEnd("fieldset");
        
        buffer.elementEnd(getTag());
        buffer.append("\n");

        renderFocusJavaScript(buffer, formFields);

        renderValidationJavaScript(buffer, formFields);
    }
}
