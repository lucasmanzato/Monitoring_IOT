package com.example.iot_monitoring;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.util.HtmlUtils;

@Controller
public class WindDataController {

    private final SimpMessagingTemplate messagingTemplate;

    public WindDataController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    // Endpoint para envio direto
    @MessageMapping("/wind-data")
    @SendTo("/topic/wind_updates")
    public WindData sendWindData(WindData windData) throws Exception {
        // Processamento adicional pode ser feito aqui
        return new WindData(
                windData.getSpeed(),
                windData.getDirection()
        );
    }

    // Método para broadcast manual
    public void broadcastWindUpdate(WindData windData) {
        messagingTemplate.convertAndSend("/topic/wind_updates", windData);
    }
}