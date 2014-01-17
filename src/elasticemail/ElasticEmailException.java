package elasticemail;

public class ElasticEmailException extends Exception {
	private static final long serialVersionUID = 1L;

	public ElasticEmailException() {
	}

	public ElasticEmailException(String message) {
		super(message);
	}

	public ElasticEmailException(Throwable cause) {
		super(cause);
	}

	public ElasticEmailException(String message, Throwable cause) {
		super(message, cause);
	}

	public ElasticEmailException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
