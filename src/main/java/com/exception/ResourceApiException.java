package com.exception;

public class ResourceApiException {

	private String resourceStatusCode;
	private String resourceStatusMsg;
	private String resourceResponseBody;
	
	public ResourceApiException() {
		
	}
	public ResourceApiException(String resourceMessage, String resourceStatusCode, String resourceStatusMsg, String resourceResponseBody){
		//super(resourceMessage);
		this.resourceStatusCode = resourceStatusCode;
		this.resourceStatusMsg = resourceStatusMsg;
		this.resourceResponseBody = resourceResponseBody;
		
	}
	public String getResourceStatusCode() {
		return resourceStatusCode;
	}
	public void setResourceStatusCode(String resourceStatusCode) {
		this.resourceStatusCode = resourceStatusCode;
	}
	public String getResourceStatusMessage() {
		return resourceStatusMessage;
	}
	public void setResourceStatusMessage(String resourceStatusMessage) {
		this.resourceStatusMessage = resourceStatusMessage;
	}
	public String getResourceResponseBody() {
		return resourceResponseBody;
	}
	public void setResourceResponseBody(String resourceResponseBody) {
		this.resourceResponseBody = resourceResponseBody;
	}
	
	
}
