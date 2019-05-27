package edu.stevens.cs594.chat.service.representations;

import javax.ws.rs.core.UriInfo;
import javax.xml.bind.annotation.XmlRootElement;

import edu.stevens.cs594.chat.service.dto.UserDto;
import edu.stevens.cs594.chat.service.dto.util.UserDtoFactory;
import edu.stevens.cs594.chat.service.web.rest.data.UserType;

@XmlRootElement
public class UserRepresentation extends UserType {

	private UserDtoFactory userDtoFactory;
	
	public UserRepresentation () {
		super();
		userDtoFactory = new UserDtoFactory();
	}
	
	public UserRepresentation (UserDto dto, UriInfo uriInfo) {
		this();	
		this.username =  dto.getUsername();
		this.name = dto.getName();
	}
	
	public UserDto getUser() {
		UserDto user = userDtoFactory.createUserDto();
		user.setUsername(username);
		user.setName(name);
		return user;
	}

}
