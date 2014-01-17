package elasticemail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.List;
import java.util.Vector;

import nanoxml.XMLElement;
import nanoxml.XMLParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElasticEmailClient {

	final static private Logger logger = LoggerFactory.getLogger(ElasticEmailClient.class);

	final static private String API_SEND = "https://api.elasticemail.com/mailer/send";
	final static private String API_UPLOAD_ATTACHMENT = "https://api.elasticemail.com/attachments/upload";
	final static private String API_STATUS = "https://api.elasticemail.com/mailer/status";
	final static private String UTF8 = "UTF-8";

	private ElasticEmailProperties props;

	/**
	 * <p>
	 * You will pass an implementation of this interface in order to supply the credentials needed for the Elastic Email
	 * API.
	 * </p>
	 * 
	 * @author broc.seib@gmail.com
	 */
	public interface ElasticEmailProperties {
		/**
		 * @return the Elastic Email API username. It is probably an email address.
		 */
		public String getElasticEmailUserName();

		/**
		 * @return the Elastic Email API Key. It looks like a hash string.
		 */
		public String getElasticEmailApiKey();
	}

	/**
	 * <p>
	 * Create an instance of a client that can speak to the Elastic Email service in the cloud.
	 * </p>
	 * 
	 * <p>
	 * As for its statefulness, the reference 'props' that you pass it is kept as a member variable, but that's all. No
	 * other state is kept. So you can keep a single instance of this class around, or re-instantiate it with the same
	 * props if you wish next time you need it.
	 * </p>
	 * 
	 * @param props
	 *        {@link ElasticEmailProperties} credentials needed to communicate with the Elastic Email API
	 */
	public ElasticEmailClient(ElasticEmailProperties props) {
		this.props = props;
	}

	/**
	 * <p>
	 * Sends a plain text email.
	 * </p>
	 * 
	 * <p>
	 * This will send an outbound email via Elastic Email. Note that you must have already set up Elastic Email via
	 * their website to send emails from a particular domain, and your "from" fields need to match that domain.
	 * </p>
	 * 
	 * @param channel
	 *        This is an arbitrary string so that you can run reports at Elastic Email and bucket your outbound email
	 *        statistics. Pass null to ignore.
	 * @param fromEmail
	 *        This is the email address that the email will be "from".
	 * @param fromName
	 *        This is the human readable name of the person (or bot) the email will be "from".
	 * @param toEmails
	 *        This is a semicolon separated list of email recipients.
	 * @param subject
	 *        This is the Subject line of the email.
	 * @param bodyText
	 *        This is the plain text body of the email.
	 * @return A transactionId string is returned, which can be passed to {@link #getStatus(String)} to check the status
	 *         of your message delivery.
	 * @throws IOException
	 *         The Elastic Email API is over HTTP. If any communication goes awry, you'll get an IOException.
	 */
	public TransactionId sendEmail(String channel, String fromEmail, String fromName, String toEmails, String subject, String bodyText) throws IOException {
		return sendEmail(channel, fromEmail, fromName, toEmails, subject, bodyText, null, null);
	}

	/*
	 * toEmails is a semi colon separated list of email recipients**
	 */

	/**
	 * <p>
	 * Sends an email with both plain text and html versions available for the client to choose to display. Attachments
	 * can be made as well.
	 * </p>
	 * 
	 * <p>
	 * This implementation was updated 10/26/2011 per this:
	 * http://stackoverflow.com/questions/2793150/how-to-use-java-net-urlconnection-to-fire-and-handle-http-requests
	 * </p>
	 * 
	 * @param channel
	 *        This is an arbitrary string so that you can run reports at Elastic Email and bucket your outbound email
	 *        statistics. Pass null to ignore.
	 * @param fromEmail
	 *        This is the email address that the email will be "from".
	 * @param fromName
	 *        This is the human readable name of the person (or bot) the email will be "from".
	 * @param toEmails
	 *        This is a semicolon separated list of email recipients.
	 * @param subject
	 *        This is the Subject line of the email.
	 * @param bodyText
	 *        This is the plain text body of the email.
	 * @param bodyHtml
	 *        This is the html version of the email body. Pass null to not supply an html version of the body.
	 * @param attachmentIds
	 *        If you have any attachments, list them here. They must be uploaded in advance by calling
	 *        {@link #uploadAttachment(InputStream, String)}.
	 * @return
	 * @throws IOException
	 *         The Elastic Email API is over HTTP. If any communication goes awry, you'll get an IOException.
	 */
	public TransactionId sendEmail(String channel, String fromEmail, String fromName, String toEmails, String subject, String bodyText, String bodyHtml, List<AttachmentId> attachmentIds)
			throws IOException {
		StringBuilder buf = new StringBuilder();
		buf.append("username=").append(urlEncodeUTF8(props.getElasticEmailUserName()));
		buf.append("&api_key=").append(urlEncodeUTF8(props.getElasticEmailApiKey()));
		buf.append("&from=").append(urlEncodeUTF8(fromEmail));
		buf.append("&from_name=").append(urlEncodeUTF8(fromName));
		buf.append("&to=").append(urlEncodeUTF8(toEmails));
		buf.append("&subject=").append(urlEncodeUTF8(subject));
		buf.append("&body_text=").append(urlEncodeUTF8(bodyText));
		if ( channel != null ) {
			buf.append("&channel=").append(urlEncodeUTF8(channel));
		}
		if ( bodyHtml != null ) {
			buf.append("&body_html=").append(urlEncodeUTF8(bodyHtml));
		}
		if ( attachmentIds != null ) {
			buf.append("&attachments=");
			for ( int i = 0; i < attachmentIds.size(); i++ ) {
				if ( i > 0 ) {
					buf.append(";");
				}
				buf.append(urlEncodeUTF8(attachmentIds.get(i).getId()));
			}
		}

		URL resturl = new URL(API_SEND);
		URLConnection con = resturl.openConnection();
		con.setDoOutput(true); // FYI, this implicitly sets req method to POST
		// con.setRequestMethod("POST");
		con.setRequestProperty("Accept-Charset", UTF8);
		con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + UTF8);
		OutputStream out = null;
		try {
			out = con.getOutputStream();
			out.write(buf.toString().getBytes(UTF8));
		}
		finally {
			if ( out != null ) {
				try {
					out.close();
				}
				catch ( IOException e ) { /* punt */}
			}
		}

		// done writing. now read.

		// get result code
		int responseCode = ((HttpURLConnection) con).getResponseCode();
		logger.info("email sent. response={} to={}, from={}, subject={}", responseCode, toEmails, fromEmail, subject);
		if ( responseCode != 200 ) {
			throw new IOException("bad response code: " + responseCode);
		}

		// the following call is what actually fires the HTTP request.
		InputStream response = con.getInputStream();
		BufferedReader in = null;
		try {
			in = new BufferedReader(new InputStreamReader(response, UTF8));
			String transactionId = in.readLine(); // only expect a single line
			// System.out.println("got back txId = " +transactionId);
			return new TransactionId(transactionId);
		}
		catch ( IOException e ) {
			throw new IOException("Unable to read response from server for sendEmail.");
		}
		finally {
			if ( in != null ) {
				try {
					in.close();
				}
				catch ( IOException e ) { /* punt */}
			}
		}
	}

	/**
	 * <p>
	 * Upload an email attachment.
	 * </p>
	 * 
	 * <p>
	 * Attachments must be uploaded in advance of sending out email messages. Each uploaded attachment is assigned an
	 * {@link AttachmentId}, which will be referred to when sending a message with an attachment.
	 * </p>
	 * 
	 * @param data
	 *        Supply a binary of your attachment, in the form of an InputStream.
	 * @param filename
	 *        Provide a name for your attachment. Use the obvious file extensions, as they will likely help on the
	 *        client side when the attachment is received.
	 * @return The {@link AttachmentId} returned will be used in
	 *         {@link #sendEmail(String, String, String, String, String, String, String, List)} to specify the
	 *         attachment(s) to be used for the message.
	 * @throws IOException
	 *         The Elastic Email API is over HTTP. If any communication goes awry, you'll get an IOException.
	 */
	public AttachmentId uploadAttachment(InputStream data, String filename) throws IOException {
		StringBuilder params = new StringBuilder();
		params.append("?username=").append(urlEncodeUTF8(props.getElasticEmailUserName()));
		params.append("&api_key=").append(urlEncodeUTF8(props.getElasticEmailApiKey()));
		params.append("&file=").append(urlEncodeUTF8(filename));

		URL resturl = new URL(API_UPLOAD_ATTACHMENT + params.toString());
		HttpURLConnection con = (HttpURLConnection) resturl.openConnection();
		con.setDoOutput(true); // FYI, this implicitly sets req method to POST
		con.setRequestMethod("PUT");
		con.setRequestProperty("Accept-Charset", UTF8);
		con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + UTF8);
		OutputStream out = null;
		try {
			final int BUFSIZ = 64 * 1024; // why not have a 64K buffer?
			byte[] buffer = new byte[BUFSIZ];
			int bytesRead = 0;

			out = con.getOutputStream();
			int total = 0;
			while ( (bytesRead = data.read(buffer, 0, BUFSIZ)) > 0 ) {
				out.write(buffer, 0, bytesRead);
				total += bytesRead;
			}
			logger.debug("total bytes uploaded: {}", total);
		}
		finally {
			if ( out != null ) {
				try {
					out.close();
				}
				catch ( IOException e ) { /* punt */}
			}
		}

		// done writing. now read.

		// get result code
		int responseCode = ((HttpURLConnection) con).getResponseCode();
		logger.debug("upload attachment response code = {}", responseCode);
		if ( responseCode != 200 ) {
			throw new IOException("bad response code: " + responseCode);
		}

		// the following call is what actually fires the HTTP request.
		InputStream response = con.getInputStream();
		BufferedReader in = null;
		try {
			in = new BufferedReader(new InputStreamReader(response, UTF8));
			String attachmentId = in.readLine(); // only expect a single line
			logger.debug("got back attachmentId = {}", attachmentId);
			return new AttachmentId(attachmentId);
		}
		catch ( IOException e ) {
			throw new IOException("Unable to read response from server for sendEmail.");
		}
		finally {
			if ( in != null ) {
				try {
					in.close();
				}
				catch ( IOException e ) { /* punt */}
			}
		}
	}

	/**
	 * <p>
	 * Poll the status of an outbound message.
	 * </p>
	 * 
	 * @param transactionId
	 *        This is a TransactionId that you were given when you made the call to send the outbound message.
	 * @return {@link MailerStatus}, which contains counters for number delivered, bounced, etc. But for now, this class
	 *         ({@link ElasticEmailClient}) is setup to only send out messages one at a time, so the value returned in
	 *         one of the fields of MailerStatus should only be a 1.
	 * @throws IOException
	 *         The Elastic Email API is over HTTP. If any communication goes awry, you'll get an IOException.
	 */
	public MailerStatus getStatus(TransactionId transactionId) throws IOException, ElasticEmailException {
		StringBuilder buf = new StringBuilder();
		buf.append(API_STATUS).append("/").append(transactionId.getId()).append("?showstats=true");
		// buf.append("&username=").append(urlEncodeUTF8(USERNAME));
		// buf.append("&api_key=").append(urlEncodeUTF8(API_KEY));
		// System.out.println("status url: "+buf.toString());
		URL resturl = new URL(buf.toString());
		HttpURLConnection con = (HttpURLConnection) resturl.openConnection();
		con.setDoOutput(true);
		con.setRequestMethod("GET");
		OutputStreamWriter out = new OutputStreamWriter(con.getOutputStream());
		out.flush();
		out.close();

		BufferedReader in = null;
		try {
			if ( con.getInputStream() != null ) {
				in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			}
		}
		catch ( IOException e ) {
			if ( con.getErrorStream() != null ) {
				in = new BufferedReader(new InputStreamReader(con.getErrorStream()));
			}
		}

		if ( in == null ) {
			throw new IOException("Unable to read response from server for getStatus.");
		}
		String response = getStringFromBufferedReader(in);
		in.close();
		
		int responseCode = con.getResponseCode();
		if ( (responseCode >= 200) || (responseCode < 300 ) ) {
			if ( response.startsWith("<") ) {
				return parseXmlReponse(response);
			}
			else {
				throw new ElasticEmailException(response);
			}
		}
		else {
			throw new ElasticEmailException(response);
		}
	}

	private MailerStatus parseXmlReponse(String response) {
		XMLElement xml = new XMLElement();
		try {
			xml.parseString(response);
			// System.out.println(xml);

			MailerStatus status = new MailerStatus();
			@SuppressWarnings("rawtypes")
			Vector children = xml.getChildren();
			for ( Object c : children ) {
				XMLElement x = (XMLElement) c;
				if ( "status".equals(x.getName()) ) {
					DeliveryStatus ds = DeliveryStatus.valueOf(x.getContent());
					status.setStatus(ds);
				}
				else {
					int val = Integer.parseInt(x.getContent());
					if ( "recipients".equals(x.getName()) ) {
						status.setRecipients(val);
					}
					else if ( "delivered".equals(x.getName()) ) {
						status.setDelivered(val);
					}
					else if ( "failed".equals(x.getName()) ) {
						status.setFailed(val);
					}
					else if ( "pending".equals(x.getName()) ) {
						status.setPending(val);
					}
					else if ( "opened".equals(x.getName()) ) {
						status.setOpened(val);
					}
					else if ( "clicked".equals(x.getName()) ) {
						status.setClicked(val);
					}
					else if ( "unsubscribed".equals(x.getName()) ) {
						status.setUnsubscribed(val);
					}
					else if ( "abusereports".equals(x.getName()) ) {
						status.setAbusereports(val);
					}
				}
			}

			return status;
		}
		catch ( XMLParseException e ) {
			logger.error("could not parse response as xml: {}", response);
			throw e;
		}
	}

	private static String getStringFromBufferedReader(BufferedReader br) {
		StringBuilder sb = new StringBuilder();
		String line;
		try {
			while ( (line = br.readLine()) != null ) {
				sb.append(line);
			}

		}
		catch ( IOException e ) {
			e.printStackTrace();
		}
		finally {
			if ( br != null ) {
				try {
					br.close();
				}
				catch ( IOException e ) {
					e.printStackTrace();
				}
			}
		}
		return sb.toString();
	}

	/**
	 * <p>
	 * Convenience function to always url encode with UTF-8.
	 * </p>
	 * 
	 * @param url
	 *        The URL to be encoded
	 * @return UTF-8 encoded URL
	 */
	static private String urlEncodeUTF8(String url) {
		try {
			return URLEncoder.encode(url, "UTF-8");
		}
		catch ( UnsupportedEncodingException e ) {
			throw new RuntimeException("Can't encode url as UTF-8: " + url);
		}
	}

}
