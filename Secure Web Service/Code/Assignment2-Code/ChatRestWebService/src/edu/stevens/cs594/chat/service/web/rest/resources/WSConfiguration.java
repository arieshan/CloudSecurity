package edu.stevens.cs594.chat.service.web.rest.resources;

import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.server.ResourceConfig;

@ApplicationPath("/resources")
public class WSConfiguration extends ResourceConfig {

	public WSConfiguration() {
		packages("edu.stevens.cs594.chat.service.web.rest.resources");
	}

}