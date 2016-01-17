/*
 * File: DataUtils.java
 * 
 * Copyright 2007 Macquarie E-Learning Centre Of Excellence
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package melcoe.fedora.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;

/**
 * Utility class for managing documents.
 * 
 * @author nishen@melcoe.mq.edu.au
 */
public class DataUtils
{
	private static final Logger log = Logger.getLogger(DataUtils.class);

	/**
	 * Loads a file into a byte array.
	 * 
	 * @param filename name of file to load.
	 * @return byte array containing the data file.
	 * @throws Exception
	 */
	public static byte[] loadFile(String filename) throws Exception
	{
		File file = new File(filename.trim());
		return loadFile(file);
	}

	/**
	 * Loads a file into a byte array.
	 * 
	 * @param File to load.
	 * @return byte array containing the data file.
	 * @throws Exception
	 */
	public static byte[] loadFile(File file) throws Exception
	{
		if (!file.exists() || !file.canRead())
		{
			String message = "Cannot read file: " + file.getCanonicalPath();
			log.error(message);
			throw new Exception(message);
		}

		FileInputStream fis = new FileInputStream(file);
		ByteArrayOutputStream data = new ByteArrayOutputStream();
		int len = 0;
		byte[] buf = new byte[1024];
		while ((len = fis.read(buf)) >= 0)
			data.write(buf, 0, len);

		return data.toByteArray();
	}

	/**
	 * Saves a byte array to a file.
	 * 
	 * @param filename the filename of the file to save the data as.
	 * @param document the byte array.
	 * @throws Exception
	 */
	public static void saveDocument(String filename, byte[] document) throws Exception
	{
		try
		{
			DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
			documentBuilderFactory.setNamespaceAware(true);
			DocumentBuilder docBuilder = documentBuilderFactory.newDocumentBuilder();

			Document doc = docBuilder.parse(new ByteArrayInputStream(document));

			saveDocument(filename, doc);
		}
		catch (Exception e)
		{
			String message = "Unable to save file: " + filename;
			log.error(message, e);
			throw new Exception(message, e);
		}
	}

	/**
	 * Saves a Document object as a file.
	 * 
	 * @param filename the filename of the file to save the data as.
	 * @param doc the Document to save.
	 * @throws Exception
	 */
	public static void saveDocument(String filename, Document doc) throws Exception
	{
		try
		{
			File file = new File(filename.trim());
			String data = format(doc);
			PrintWriter writer = new PrintWriter(file, "UTF-8");
			writer.print(data);
			writer.flush();
			writer.close();
		}
		catch (Exception e)
		{
			String message = "Unable to save file: " + filename;
			log.error(message, e);
			throw new Exception(message, e);
		}
	}

	/**
	 * Formats a Document.
	 * 
	 * @param doc the Document to format.
	 * @return the Document as a formatted String.
	 * @throws Exception
	 */
	public static String format(Document doc) throws Exception
	{
		OutputFormat format = new OutputFormat(doc);
		format.setEncoding("UTF-8");
		format.setIndenting(true);
		format.setIndent(2);
		format.setOmitXMLDeclaration(true);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Writer output = new OutputStreamWriter(out);

		XMLSerializer serializer = new XMLSerializer(output, format);
		serializer.serialize(doc);

		return new String(out.toByteArray(), "UTF-8");
	}

	/**
	 * Formats an XML Document represented as an array of bytes.
	 * 
	 * @param doc the byte array of the Document to format.
	 * @return the Document as a formatted String.
	 * @throws Exception
	 */
	public static String format(byte[] document) throws Exception
	{
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		documentBuilderFactory.setNamespaceAware(true);
		DocumentBuilder docBuilder = documentBuilderFactory.newDocumentBuilder();

		Document doc = docBuilder.parse(new ByteArrayInputStream(document));

		return format(doc);
	}

	/**
	 * Format the XML document for Fedora hash computation.
	 * 
	 * @param data the XML data as a byte array.
	 * @return the Fedora hash compatible formatted version of the document.
	 * @throws Exception
	 */
	public static byte[] fedoraXMLHashFormat(byte[] data) throws Exception
	{
		OutputFormat format = new OutputFormat("XML", "UTF-8", false);
		format.setIndent(0);
		format.setLineWidth(0);
		format.setPreserveSpace(false);
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		XMLSerializer serializer = new XMLSerializer(outStream, format);

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(new ByteArrayInputStream(data));
		serializer.serialize(doc);

		ByteArrayInputStream in = new ByteArrayInputStream(outStream.toByteArray());
		BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
		String line = null;
		StringBuffer sb = new StringBuffer();
		while ((line = br.readLine()) != null)
		{
			line = line.trim();
			sb = sb.append(line);
		}

		return sb.toString().getBytes("UTF-8");
	}

	/**
	 * Generates an MD5 checksum of a series of bytes.
	 * 
	 * @param data the byte array on which to compute the hash.
	 * @return the MD5 hash.
	 * @throws NoSuchAlgorithmException
	 */
	public static String getHash(byte[] data) throws NoSuchAlgorithmException
	{
		MessageDigest digest = MessageDigest.getInstance("MD5");
		byte[] hash = digest.digest(data);

		String hexHash = byte2hex(hash);

		return hexHash;
	}

	/**
	 * Converts a hash into its hexadecimal string representation.
	 * 
	 * @param bytes the byte array to convert
	 * @return the hexadecimal string representation
	 */
	private static String byte2hex(byte[] bytes)
	{
		char[] hexChars = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < bytes.length; i++)
		{
			sb.append(hexChars[(bytes[i] >> 4) & 0xf]);
			sb.append(hexChars[bytes[i] & 0xf]);
		}

		return new String(sb);
	}
}
