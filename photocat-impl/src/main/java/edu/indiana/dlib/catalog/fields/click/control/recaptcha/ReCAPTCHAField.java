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
package edu.indiana.dlib.catalog.fields.click.control.recaptcha;

import net.tanesha.recaptcha.ReCaptchaImpl;
import net.tanesha.recaptcha.http.SimpleHttpLoader;

import org.apache.click.control.Field;
import org.apache.click.util.HtmlStringBuffer;

/**
 * A Control that represents a ReCAPTCHA challenge.  This 
 * Control
 */
public class ReCAPTCHAField extends Field {

    private String privateKey;
    
    private String publicKey;
    
    public ReCAPTCHAField(String privateKey, String publicKey) {
        super("recaptcha_challenge_field");
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }
    
    public String getLabel() {
        return "ReCAPTCHA";
    }
    
    public void render(HtmlStringBuffer buffer) {
        buffer.append("<script type=\"text/javascript\" src=\"http://api.recaptcha.net/challenge?k=" + publicKey + "\" />\"></script>\n");
        buffer.append("<noscript>\n");
        buffer.append("  <iframe src=\"http://api.recaptcha.net/noscript?k=" + publicKey + "\" height=\"300\" width=\"500\" frameborder=\"0\"></iframe><br/>\n");
        buffer.append("  <textarea name=\"recaptcha_challenge_field\" rows=\"3\" cols=\"40\"></textarea> <br/>\n");
        buffer.append("  <input type=\"hidden\" name=\"recaptcha_response_field\" value=\"manual_challenge\"/>\n");
        buffer.append("</noscript>\n");
    }

    public boolean getValidate() {
        return true;
    }
    
    public void validate() {
        ReCaptchaImpl recaptcha = new ReCaptchaImpl();
        recaptcha.setIncludeNoscript(true);
        recaptcha.setPrivateKey(privateKey);
        recaptcha.setPublicKey(publicKey);
        recaptcha.setRecaptchaServer(ReCaptchaImpl.HTTPS_SERVER);
        recaptcha.setHttpLoader(new SimpleHttpLoader());
        String challenge = getContext().getRequest().getParameter("recaptcha_challenge_field");
        String response = getContext().getRequest().getParameter("recaptcha_response_field");
        if (recaptcha.checkAnswer(getContext().getRequest().getRemoteAddr(), challenge, response).isValid()) {
            // valid
        } else {
            setErrorMessage("invalid-recaptcha-response");
        }
    }
    
}
