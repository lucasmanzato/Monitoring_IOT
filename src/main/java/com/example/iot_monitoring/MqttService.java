package com.example.iot_monitoring;

import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class MqttService implements MqttCallbackExtended {
    private static final Logger logger = LoggerFactory.getLogger(MqttService.class);

    @Value("${mqtt.broker.url}")
    private String brokerUrl;

    @Value("${mqtt.client.id}")
    private String clientId;

    @Value("${mqtt.connection.timeout:10}")
    private int connectionTimeout;

    private MqttAsyncClient mqttClient;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @PostConstruct
    public void init() {
        try {
            logger.info("Iniciando serviço MQTT para monitoramento de vento/direção");
            logger.info("Timeout de conexão configurado para: {} segundos", connectionTimeout);

            this.mqttClient = new MqttAsyncClient(brokerUrl, clientId);
            this.mqttClient.setCallback(this);

            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setConnectionTimeout(connectionTimeout);
            options.setKeepAliveInterval(60);

            this.mqttClient.connect(options).waitForCompletion();
            logger.info("Conectado ao broker MQTT: {}", brokerUrl);

        } catch (MqttException e) {
            logger.error("Falha na conexão inicial com MQTT. Agendando reconexão...", e);
            scheduleReconnect();
        }
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        try {
            // Tópicos atualizados para vento/direção
            this.mqttClient.subscribe("wind/speed", 1);     // Velocidade do vento
            this.mqttClient.subscribe("wind/direction", 1); // Direção do vento
            logger.info("Inscrito nos tópicos de vento/direção");
        } catch (MqttException e) {
            logger.error("Falha ao se inscrever nos tópicos MQTT", e);
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload());
        logger.info("Dados recebidos - Tópico: {} | Valor: {}", topic, payload);

        try {
            double value = Double.parseDouble(payload);

            if (topic.equals("wind/speed")) {
                logger.info("Velocidade do vento atualizada: {} km/h", value);
                // Aqui você processaria os dados de velocidade do vento
            }
            else if (topic.equals("wind/direction")) {
                logger.info("Direção do vento atualizada: {} graus", value);
                // Aqui você processaria os dados de direção do vento
            }

        } catch (NumberFormatException e) {
            logger.error("Payload inválido recebido no tópico {}: {}", topic, payload);
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        logger.warn("Conexão MQTT perdida: {}", cause.getMessage());
        scheduleReconnect();
    }

    private void scheduleReconnect() {
        scheduler.schedule(() -> {
            try {
                if (mqttClient != null && !mqttClient.isConnected()) {
                    logger.info("Tentando reconectar ao broker MQTT...");
                    mqttClient.reconnect();
                }
            } catch (MqttException e) {
                logger.error("Falha na reconexão: {}", e.getMessage());
                scheduleReconnect();
            }
        }, 5, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void cleanup() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
                logger.info("Conexão MQTT encerrada");
            }
            scheduler.shutdown();
        } catch (MqttException e) {
            logger.error("Erro ao desconectar: {}", e.getMessage());
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // Implementação vazia conforme necessário
    }
}