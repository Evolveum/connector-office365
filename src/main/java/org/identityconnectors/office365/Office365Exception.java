package org.identityconnectors.office365;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.identityconnectors.common.logging.Log;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class Office365Exception extends RuntimeException {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8902330905878742105L;
	private static final Log log = Log.getLog(Office365UserOps.class);
	private Integer errorCode;
	private String response;
	private String errorMessage;
	
	public Office365Exception(Integer errorCode, String response)
	{
		this.errorCode = errorCode;
		this.setResponse(response);
	}
	
	public Integer getErrorCode() {
		return errorCode;
	}
	public void setErrorCode(Integer errorCode) {
		this.errorCode = errorCode;
	}
	public String getResponse() {
		return response;
	}
	public void setResponse(String response) {
		this.response = response;
		this.setErrorMessage(this.parseXML(this.response));		
	}
	public String getErrorMessage() {
		return errorMessage;
	}
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
	
	private String parseXML(String xml){
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        InputSource is;
        try {
            builder = factory.newDocumentBuilder();
            is = new InputSource(new StringReader(xml));
            Document doc = builder.parse(is);
            NodeList list = doc.getElementsByTagName("message");
            xml = list.item(0).getTextContent();
        } catch (ParserConfigurationException e) {
        	log.info("Error Parsing XML ",e);        
        } catch (SAXException e) {
        	log.info("Error Parsing XML ",e); 
        } catch (IOException e) {
        	log.info("Error Parsing XML ",e); 
        }        
		return xml;
		
	}
}
