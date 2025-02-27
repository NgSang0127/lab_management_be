package org.sang.labmanagement.notification;


import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.user.User;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;


@Controller
@RequiredArgsConstructor
public class NotificationWebSocketController {

	private final SimpMessagingTemplate messagingTemplate;



}

