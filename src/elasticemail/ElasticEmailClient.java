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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ElasticEmailClient {
	
	final static private Logger logger = LoggerFactory.getLogger(ElasticEmailClient.class);
	
	final static private String API_SEND = "https://api.elasticemail.com/mailer/send";
	final static private String API_UPLOAD_ATTACHMENT = "https://api.elasticemail.com/attachments/upload";
	final static private String API_STATUS = "https://api.elasticemail.com/mailer/status";
	final static private String UTF8 = "UTF-8";
	
	private ElasticEmailProperties props;
	
	public interface ElasticEmailProperties {
		public String getElasticEmailUserName();
		public String getElasticEmailApiKey();
	}
	
	public ElasticEmailClient(ElasticEmailProperties props) {
		this.props = props;
	}

	/*
	 * toEmails is a semi colon separated list of email recipients**
	 */
	public String sendEmail(String channel, String fromEmail, String fromName, String toEmails, String subject, String bodyText) throws IOException {
		return sendEmail(channel, fromEmail, fromName, toEmails, subject, bodyText, null, null);
	}

	/*
	 * toEmails is a semi colon separated list of email recipients**
	 * updated 10/26/2011 per this: http://stackoverflow.com/questions/2793150/how-to-use-java-net-urlconnection-to-fire-and-handle-http-requests
	 */
	public String sendEmail(String channel, String fromEmail, String fromName, String toEmails, String subject, String bodyText, String bodyHtml, List<AttachmentId> attachmentIds) throws IOException {
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
			for (int i=0;i<attachmentIds.size();i++) {
				if ( i > 0 ) {
					buf.append(";");
				}
				buf.append(urlEncodeUTF8(attachmentIds.get(i).getId()));				
			}
		}
		
		URL resturl = new URL(API_SEND);
		URLConnection con = resturl.openConnection();
		con.setDoOutput(true); // FYI, this implicitly sets req method to POST
//		con.setRequestMethod("POST");
		con.setRequestProperty("Accept-Charset", UTF8);
		con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + UTF8);
		OutputStream out = null;
		try {
			out = con.getOutputStream();
			out.write(buf.toString().getBytes(UTF8));
		}
		finally {
			if ( out != null ) {
				try { out.close(); }
				catch ( IOException e ) { /* punt */ }
			}
		}
		
		// done writing. now read.

		// get result code
		int responseCode = ((HttpURLConnection) con).getResponseCode();
		logger.debug("send email response code = " +responseCode);
		if ( responseCode != 200 ) {
			throw new IOException("bad response code: " + responseCode);
		}

		// the following call is what actually fires the HTTP request.
		InputStream response = con.getInputStream();
		BufferedReader in = null;
		try {
			in = new BufferedReader(new InputStreamReader(response, UTF8));
			String transactionId = in.readLine(); // only expect a single line
			//System.out.println("got back txId = " +transactionId);
			return transactionId;
		}
		catch( IOException e ){
			throw new IOException("Unable to read response from server for sendEmail.");
		}
		finally {
			if ( in != null ) {
				try { in.close(); }
				catch ( IOException e ) { /* punt */ }
			}
		}
	}
	
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
			final int BUFSIZ = 64*1024; // why not have a 64K buffer?
			byte[] buffer = new byte[BUFSIZ];
			int bytesRead = 0;

			out = con.getOutputStream();
			int total = 0;
			while ( (bytesRead = data.read(buffer, 0, BUFSIZ)) > 0 ) {
				out.write(buffer, 0, bytesRead);
				total += bytesRead;
			}
			logger.debug("total bytes uploaded: " + total);
		}
		finally {
			if ( out != null ) {
				try { out.close(); }
				catch ( IOException e ) { /* punt */ }
			}
		}
		
		// done writing. now read.

		// get result code
		int responseCode = ((HttpURLConnection) con).getResponseCode();
		logger.debug("upload attachment response code = " +responseCode);
		if ( responseCode != 200 ) {
			throw new IOException("bad response code: " + responseCode);
		}

		// the following call is what actually fires the HTTP request.
		InputStream response = con.getInputStream();
		BufferedReader in = null;
		try {
			in = new BufferedReader(new InputStreamReader(response, UTF8));
			String attachmentId = in.readLine(); // only expect a single line
			logger.debug("got back attachmentId = " +attachmentId);
			return new AttachmentId(attachmentId);
		}
		catch( IOException e ){
			throw new IOException("Unable to read response from server for sendEmail.");
		}
		finally {
			if ( in != null ) {
				try { in.close(); }
				catch ( IOException e ) { /* punt */ }
			}
		}
	}
	
	public MailerStatus getStatus(String transactionId) throws IOException {
		StringBuilder buf = new StringBuilder();
		buf.append(API_STATUS).append("/").append(transactionId).append("?showstats=true");
//		buf.append("&username=").append(urlEncodeUTF8(USERNAME));
//		buf.append("&api_key=").append(urlEncodeUTF8(API_KEY));
//System.out.println("status url: "+buf.toString());
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
		catch( IOException e ){
			if ( con.getErrorStream() != null ) {
				in = new BufferedReader(new InputStreamReader(con.getErrorStream()));
			}
		}

		if ( in == null ) {
			throw new IOException("Unable to read response from server for getStatus.");
		}

		XMLElement xml = new XMLElement();
		xml.parseFromReader(in);
		in.close();
//		System.out.println(xml);


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
	
	static public String urlEncodeUTF8(String url) {
		try {
			return URLEncoder.encode(url, "UTF-8");
		}
		catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Can't encode url as UTF-8: "+url);
		}		
	}
	
}
