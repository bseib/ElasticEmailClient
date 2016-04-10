# ElasticEmailClient

This is far from a complete lib to use the full Elastic Email API. But it has enough to send an
email and get it's status. It's lightweight and has no dependencies.

##Download Jar Files##

  https://bitbucket.org/brocseib/elasticemailclient/downloads

## Example ##

You write a bit of code to supply your credentials. Something like this:


```
#!java

public enum ElasticEmail {

	$;

	private ElasticEmailClient client = createElasticEmailClient();

	public ElasticEmailClient getClient() {
		return client;
	}

	private ElasticEmailClient createElasticEmailClient() {
		return new ElasticEmailClient(new ElasticEmailProperties() {

			@Override
			public String getElasticEmailUserName() {
				// you write something here to safely inject your credentials.
				// this is an example:
				return Keys.getElasticEmailClientUsername();
			}

			@Override
			public String getElasticEmailApiKey() {
				// you write something here to safely inject your credentials.
				// this is an example:
				return Keys.getElasticEmailClientApiKey();
			}
		});
	}

}


```

## Then you can send an email like this: ##


```
#!java

	// send an email
	ElasticEmailClient eec = ElasticEmail.$.getClient();
	TransactionId tid = eec.sendEmail(channel, fromEmail, fromName, toEmails, subject, bodyText);
	
	// and later you can check the delivery status:
	MailerStatus status = ElasticEmail.$.getClient().getStatus(tid);
	if ( elasticemail.DeliveryStatus.complete.equals(status.getStatus()) ) {
		isComplete = true;
	}
```