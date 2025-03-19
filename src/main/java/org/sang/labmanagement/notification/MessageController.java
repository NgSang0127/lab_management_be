package org.sang.labmanagement.notification;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class MessageController {

//	@MessageMapping("/ws")
//	@SendTo("/topic/messages")
//	public Map<String, String> send(Message message) {
//		System.out.println("📩 Received Message from: " + message.getFrom() + " → " + message.getText());
//
//		String time = new SimpleDateFormat("HH:mm").format(new Date());
//		Map<String, String> response = new HashMap<>();
//		response.put("from", message.getFrom());
//		response.put("text", message.getText());
//		response.put("time", time);
//		return response;
//	}


	private  final SimpMessagingTemplate simpMessagingTemplate;

	@MessageMapping("/message")
	@SendTo("/chatroom/public")
	public Message receiveMessage(@Payload Message message){
		return message;
	}

	@MessageMapping("/private-message")
	public Message recMessage(@Payload Message message){
		simpMessagingTemplate.convertAndSendToUser(message.getReceiverName(),"/private",message);
		System.out.println(message.toString());
		return message;
	}

}
