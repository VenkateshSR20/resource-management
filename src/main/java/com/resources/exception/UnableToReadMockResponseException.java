package main.java.com.resources.exception;
/**
 * Application specific exception class.
 */
public class UnableToReadMockResponseException extends Exception {

	private static final long serialVersionUID = 1L;

	/**
	 * @see Exception#Exception
	 */
	public UnableToReadMockResponseException() {

	}

	/**
	 * @see Exception#Exception((String )
	 */
	public UnableToReadMockResponseException(String message) {
		super(message);
	}

	/**
	 * @see Exception#Exception((String , Throwable )
	 */
	public UnableToReadMockResponseException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Constructs Exception class having <Throwable> as argument.
	 * 
	 * @param message
	 * @param cause
	 */
	public UnableToReadMockResponseException(Throwable cause) {
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

	protected UnableToReadMockResponseException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
