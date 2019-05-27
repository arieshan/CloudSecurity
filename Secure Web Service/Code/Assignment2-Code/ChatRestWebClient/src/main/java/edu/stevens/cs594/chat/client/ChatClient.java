package edu.stevens.cs594.chat.client;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.logging.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

import edu.stevens.cs594.chat.service.representations.MessageRepresentation;
import edu.stevens.cs594.util.DateUtils;

public class ChatClient {
	
	@SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(ChatClient.class.getSimpleName());
	
	private Client client;
	
	private URI baseUri;
	
	public ChatClient(URI baseUri, SSLContext sslContext, String username, char[] password) {
		HostnameVerifier hostnameVerifier = new HostnameVerifier() {
			@Override
			public boolean verify(String arg0, SSLSession arg1) {
				return true;
			} 
		};
		HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic(username, new String(password));
		this.client = ClientBuilder.newBuilder()
				.hostnameVerifier(hostnameVerifier)
				.sslContext(sslContext)
				.register(feature)
				.build();
		this.baseUri = baseUri;
	}
	
	// TODO add constructor for the case where authenticating with client cert
	public ChatClient(URI baseUri,SSLContext sslContext){
		this.client = ClientBuilder.newBuilder().sslContext(sslContext).build();
		this.baseUri = baseUri;
	}
	
	private String postMessageUri() {
		UriBuilder ub = UriBuilder.fromUri(baseUri);
		return ub.path("resources").path("forum").path("messages").build().toString();
	}
	
	private String getMessagesUri() {
		UriBuilder ub = UriBuilder.fromUri(baseUri);
		return ub.path("resources").path("forum").path("messages").build().toString();
	}
	
	private String deleteMessageUri(int id) {
		UriBuilder ub = UriBuilder.fromUri(baseUri);
		return ub.path("resources").path("forum").path("messages").path("{id}").build(id).toString();
	}
	
	protected List<MessageRepresentation> getMessages() throws IOException {
		WebTarget target = client.target(getMessagesUri());
		Invocation.Builder request = target.request(MediaType.APPLICATION_XML_TYPE);
		Response response = request.get();
		if (response.getStatus() >= 400) {
			throw new IOException("Error response from chat server: "+response.getStatus());
		}
		return null;
	}
		
	/**
	 * Post a message to the forum.
	 */
	public int postMessage(String sender, String text) {
		MessageRepresentation message = new MessageRepresentation();
		message.setSender(sender);
		message.setText(text);
		message.setTimestamp(DateUtils.now());
		
		WebTarget target = client.target(postMessageUri());
		Invocation.Builder request = target.request();
		Response response = null;
		try {
		response = request.post(Entity.xml(message));
		} catch (Exception e) {
			 
		}
		// Response response = request.post(Entity.entity(message, MediaType.APPLICATION_XML), Response.class);
		return (response == null) ? 500 : response.getStatus();
	}
	
	/**
	 * Delete a message from the forum.
	 */
	protected int deleteMessage(int id) {
		WebTarget target = client.target(deleteMessageUri(id));
		Response response = target.request().delete();
		return response.getStatus();
	}

	
}
