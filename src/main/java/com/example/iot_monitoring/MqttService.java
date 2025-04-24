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

    @Value("${mqtt.broker.url:tcp://localhost:1883}")
    private String brokerUrl;

    @Value("${mqtt.client.id:wind-monitor-default}")
    private String clientId;

    @Value("${mqtt.connection.timeout:10}")
    private int connectionTimeout;

    @Value("${mqtt.reconnect.delay:5}")
    private int reconnectDelay;

    @Value("${mqtt.keepalive.interval:60}")
    private int keepAliveInterval;

    private MqttAsyncClient mqttClient;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @PostConstruct
    public void init() {
        try {
            logger.info("Initializing MQTT service for wind monitoring");
            logger.info("Connecting to broker: {} with ClientID: {}", brokerUrl, clientId);

            this.mqttClient = new MqttAsyncClient(brokerUrl, clientId);
            this.mqttClient.setCallback(this);
            connectToBroker();

        } catch (MqttException e) {
            logger.error("Failed to initialize MQTT client", e);
            scheduleReconnect();
        }
    }

    private void connectToBroker() throws MqttException {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setConnectionTimeout(connectionTimeout);
        options.setKeepAliveInterval(keepAliveInterval);
        options.setWill("wind/status", "sensor-offline".getBytes(), 2, true);

        this.mqttClient.connect(options).waitForCompletion();
        logger.info("Successfully connected to MQTT broker: {}", brokerUrl);
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        logger.info("{} to broker", reconnect ? "Reconnected" : "Connected");

        try {
            subscribeToTopics();
        } catch (MqttException e) {
            logger.error("Failed to subscribe to topics", e);
            scheduleReconnect();
        }
    }

    private void subscribeToTopics() throws MqttException {
        this.mqttClient.subscribe("wind/speed", 1);
        this.mqttClient.subscribe("wind/direction", 1);
        logger.info("Subscribed to topics: wind/speed and wind/direction");
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload());
        logger.debug("Received message - Topic: {} | Payload: {}", topic, payload);

        try {
            double value = Double.parseDouble(payload);
            if (topic.equals("wind/speed")) {
                logger.info("Wind speed updated: {} km/h", value);
                // Process wind speed data
            } else if (topic.equals("wind/direction")) {
                logger.info("Wind direction updated: {} degrees", value);
                // Process wind direction data
            }
        } catch (NumberFormatException e) {
            logger.error("Invalid payload received on topic {}: {}", topic, payload);
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        logger.warn("MQTT connection lost: {}", cause.getMessage());
        scheduleReconnect();
    }

    private void scheduleReconnect() {
        scheduler.schedule(() -> {
            try {
                if (mqttClient != null && !mqttClient.isConnected()) {
                    logger.info("Attempting to reconnect to MQTT broker...");
                    mqttClient.reconnect();
                }
            } catch (MqttException e) {
                logger.error("Reconnection attempt failed: {}", e.getMessage());
                scheduleReconnect();
            }
        }, reconnectDelay, TimeUnit.SECONDS);
    }

    public void publishMessage(String topic, String payload, int qos, boolean retained) {
        try {
            if (mqttClient == null || !mqttClient.isConnected()) {
                logger.warn("MQTT client not connected, attempting to reconnect...");
                connectToBroker();
            }

            MqttMessage message = new MqttMessage(payload.getBytes());
            message.setQos(qos);
            message.setRetained(retained);

            mqttClient.publish(topic, message);
            logger.debug("Message published to topic: {}", topic);

        } catch (MqttException e) {
            logger.error("Failed to publish message to topic {}", topic, e);
            scheduleReconnect();
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        try {
            if (token != null) {
                logger.debug("Message delivery confirmed for message ID: {}", token.getMessageId());
                if (token.getException() != null) {
                    logger.warn("Delivery exception: {}", token.getException().getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("Error processing delivery confirmation", e);
        }
    }

    @PreDestroy
    public void cleanup() {
        try {
            if (mqttClient != null) {
                if (mqttClient.isConnected()) {
                    mqttClient.disconnect();
                    logger.info("Disconnected from MQTT broker");
                }
                mqttClient.close();
            }
            scheduler.shutdownNow();
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warn("Scheduler did not terminate gracefully");
            }
            logger.info("MQTT service shutdown complete");
        } catch (Exception e) {
            logger.error("Error during cleanup", e);
        }
    }
}