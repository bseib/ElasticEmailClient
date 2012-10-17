package elasticemail;

public class MailerStatus {

	private DeliveryStatus status;  // can have only one of two values: "complete" or "in_progress" according to email with support on Oct 14 2011
	private int recipients;
	private int delivered;
	private int failed;
	private int pending;
	private int opened;
	private int clicked;
	private int unsubscribed;
	private int abusereports;

	public DeliveryStatus getStatus() {
		return status;
	}
	public void setStatus(DeliveryStatus status) {
		this.status = status;
	}
	public int getRecipients() {
		return recipients;
	}
	public void setRecipients(int recipients) {
		this.recipients = recipients;
	}
	public int getDelivered() {
		return delivered;
	}
	public void setDelivered(int delivered) {
		this.delivered = delivered;
	}
	public int getFailed() {
		return failed;
	}
	public void setFailed(int failed) {
		this.failed = failed;
	}
	public int getPending() {
		return pending;
	}
	public void setPending(int pending) {
		this.pending = pending;
	}
	public int getOpened() {
		return opened;
	}
	public void setOpened(int opened) {
		this.opened = opened;
	}
	public int getClicked() {
		return clicked;
	}
	public void setClicked(int clicked) {
		this.clicked = clicked;
	}
	public int getUnsubscribed() {
		return unsubscribed;
	}
	public void setUnsubscribed(int unsubscribed) {
		this.unsubscribed = unsubscribed;
	}
	public int getAbusereports() {
		return abusereports;
	}
	public void setAbusereports(int abusereports) {
		this.abusereports = abusereports;
	}

}
