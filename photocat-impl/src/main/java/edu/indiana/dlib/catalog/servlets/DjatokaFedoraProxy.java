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
package edu.indiana.dlib.catalog.servlets;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.ApplicationContext;

import edu.indiana.dlib.catalog.config.ItemManager;
import edu.indiana.dlib.catalog.config.impl.FedoraItemManager;
import edu.indiana.dlib.catalog.pages.ApplicationPage;

public class DjatokaFedoraProxy extends HttpServlet {

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String url = req.getParameter("url");
        
        ApplicationContext context = ApplicationPage.getApplicationContext(req.getSession().getServletContext());
        if (context == null) {
            throw new RuntimeException("Unable to find the ApplicationContext!");
        }
        ItemManager im = (ItemManager) context.getBean("itemManager");
        if (im instanceof FedoraItemManager && url.startsWith(((FedoraItemManager) im).getFedoraServerUrl())) {
            // proxy the request
            proxyContentToResponse(url, req, resp);
        } else {
            // this servlet isn't supported or the path cannot be proxied
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
        }
    }
    
    public void proxyContentToResponse(String url, HttpServletRequest request, HttpServletResponse response) throws MalformedURLException, IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        response.setStatus(conn.getResponseCode());
        response.setHeader("Content-Type", conn.getHeaderField("Content-Type"));
        ReadableByteChannel inputChannel = Channels.newChannel(conn.getInputStream());  
        WritableByteChannel outputChannel = Channels.newChannel(response.getOutputStream());
        try {
            ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);  
            while (inputChannel.read(buffer) != -1) {  
                buffer.flip();  
                outputChannel.write(buffer);  
                buffer.compact();  
            }  
            buffer.flip();  
            while (buffer.hasRemaining()) {  
                outputChannel.write(buffer);  
            }  
        } finally {
            inputChannel.close();  
            outputChannel.close();
        } 
    }
    
}
