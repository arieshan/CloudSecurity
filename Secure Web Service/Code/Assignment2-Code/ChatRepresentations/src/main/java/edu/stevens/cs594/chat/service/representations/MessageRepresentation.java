package edu.stevens.cs594.chat.service.representations;

import javax.xml.bind.annotation.XmlRootElement;

import edu.stevens.cs594.chat.service.dto.MessageDto;
import edu.stevens.cs594.chat.service.dto.util.MessageDtoFactory;
import edu.stevens.cs594.chat.service.web.rest.data.MessageType;
import edu.stevens.cs594.chat.service.web.rest.data.ObjectFactory;

@XmlRootElement
public class MessageRepresentation extends MessageType {
	
	@SuppressWarnings("unused")
	private ObjectFactory repFactory = new ObjectFactory();
	
	private MessageDtoFactory messageDtoFactory;
	
	public MessageRepresentation() {
		super();
		messageDtoFactory = new MessageDtoFactory();
	}
	
	public MessageRepresentation (MessageDto dto) {
		this.sender = dto.getSender();
		this.text = dto.getText();
		this.timestamp = dto.getTimestamp();
	}

	public MessageDto getMessage() {
		MessageDto m = messageDtoFactory.createMessageDto();
		m.setSender(this.sender);
		m.setText(this.text);
		m.setTimestamp(this.timestamp);
		return m;
	}
	
}
