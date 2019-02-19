package main.java.com.resources.exception;
/**
 * Application specific exception class.
 */
public class ResourceCoveragesApiException extends Exception {

	private static final long serialVersionUID = 1L;
	private String statusCode;
	private String statusMessage;
	private String responseBody;

	/**
	 * @see Exception#Exception
	 */
	public ResourceCoveragesApiException() {

	}

	/**
	 * @see Exception#Exception((String )
	 */
	public ResourceCoveragesApiException(String message) {
		super(message);
	}
	
	/**
	 * @see Exception#Exception((String )
	 */
	public ResourceCoveragesApiException(String message, String statusCode, String statusMessage, String responseBody) {
		super(message);
		
		this.statusCode = statusCode;
		this.statusMessage = statusMessage;
		this.responseBody = responseBody;
	}

	/**
	 * @see Exception#Exception((String , Throwable )
	 */
	public ResourceCoveragesApiException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Constructs Exception class having <Throwable> as argument.
	 * 
	 * @param message
	 * @param cause
	 */
	public ResourceCoveragesApiException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructs a new exception with the specified detail message, cause,
	 * suppression enabled or disabled, and writable stack trace enabled or
	 * disabled.
	 *
	 * @param message
	 *            the detail message.
	 * @param cause
	 *            the cause. (A {@code null} value is permitted, and indicates
	 *            that the cause is nonexistent or unknown.)
	 * @param enableSuppression
	 *            whether or not suppression is enabled or disabled
	 * @param writableStackTrace
	 *            whether or not the stack trace should be writable
	 * @since 1.7
	 */

	protected ResourceCoveragesApiException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public String getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(String statusCode) {
		this.statusCode = statusCode;
	}

	public String getStatusMessage() {
		return statusMessage;
	}

	public void setStatusMessage(String statusMessage) {
		this.statusMessage = statusMessage;
	}

	public String getResponseBody() {
		return responseBody;
	}

	public void setResponseBody(String responseBody) {
		this.responseBody = responseBody;
	}

}
