package edu.stevens.cs594.chat.service.web.rest.resources;

import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import edu.stevens.cs594.chat.service.dto.MessageDto;
import edu.stevens.cs594.chat.service.ejb.IMessageServiceLocal;
import edu.stevens.cs594.chat.service.representations.MessageRepresentation;

@Path("/forum")
@RequestScoped
@Transactional
public class MessageResource {
	
	final static Logger logger = Logger.getLogger(MessageResource.class.getCanonicalName());
	
    @Context
    private UriInfo uriInfo;

    /**
     * Default constructor. 
     */
    public MessageResource() {
    }
    
    @Inject
    private IMessageServiceLocal messageService;
    
	/**
	 * Return an SSL context that uses the app truststore to authenticate the server.
	 * 
	 * @return
	 * @throws GeneralSecurityException
	 */
	public static SSLContext getAuthContext(KeyStore keystore, KeyStore truststore) throws GeneralSecurityException {
		// TODO should include both trust store and key store.
		String algorithm = TrustManagerFactory.getDefaultAlgorithm();
		TrustManagerFactory tmf = TrustManagerFactory.getInstance(algorithm);
		tmf.init(truststore);
		KeyManagerFactory  kmf = KeyManagerFactory.getInstance(algorithm);
		KeyManager[] keymanagers =  kmf.getKeyManagers();
		kmf.init(keystore, null);
		SSLContext context = SSLContext.getInstance("TLS");
		context.init(keymanagers,tmf.getTrustManagers(), null);
		return context;
	}
    
	@GET
	@Path("messages")
	@Produces("application/xml")
	public List<MessageRepresentation> getMessages() {
		List<MessageDto> messages = messageService.getMessages();
		List<MessageRepresentation> messageReps = new ArrayList<MessageRepresentation>();
		for (MessageDto message : messages) {
			MessageRepresentation messageRep = new MessageRepresentation(message);
			messageReps.add(messageRep);
		}
		return messageReps;
	}

	@POST
	@Path("messages")
	@Consumes("application/xml")
	public Response addMessage(MessageRepresentation messageRep) {
		MessageDto message = messageRep.getMessage();
		long id = messageService.addMessage(message);
		UriBuilder ub = uriInfo.getAbsolutePathBuilder().path("{id}");
		URI url = ub.build(id);
		return Response.created(url).build();
	}
    
    @DELETE
    @Path("messages/{mid}")
    public Response deleteMessage(@PathParam("mid") String mid) {
    	messageService.deleteMessage(Long.parseLong(mid));
    	return Response.ok().build();
    }
    
}