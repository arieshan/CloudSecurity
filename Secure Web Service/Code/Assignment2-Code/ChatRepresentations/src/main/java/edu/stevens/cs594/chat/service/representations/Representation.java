package edu.stevens.cs594.chat.service.representations;

public abstract class Representation {
	
	public static final String RELATIONS = "http://cs594.stevens.edu/chat/rest/relations/";
	
	public static final String RELATION_MESSAGE = RELATIONS + "message";
	
	public static final String RELATION_SENDER = RELATIONS + "sender";
	
	public static final String RELATION_RECEIVER = RELATIONS + "receiver";
	
	public static final String NAMESPACE = "http://cs549.stevens.edu/chat/service/web/rest/data";
	
	public static final String DAP_NAMESPACE = NAMESPACE + "/dap";
	
	public static final String MEDIA_TYPE = "application/xml";
	
	// public abstract List<Link> getLinks();

}
