package com.example.iot_monitoring;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class WindSensorSimulator implements MqttCallbackExtended {
    private static final Logger logger = LoggerFactory.getLogger(WindSensorSimulator.class);

    @Value("${mqtt.broker.url:tcp://localhost:1883}")
    private String brokerUrl;

    @Value("${mqtt.client.prefix:wind-sensor}")
    private String clientIdPrefix;

    @Value("${wind.simulation.interval:5}")
    private int simulationInterval;

    @Value("${wind.simulation.min-speed:10}")
    private double minSpeed;

    @Value("${wind.simulation.max-speed:50}")
    private double maxSpeed;

    @Value("${firebase.database.url}")
    private String firebaseDatabaseUrl;

    @Value("${firebase.config.path}")
    private String firebaseConfigPath;

    private DatabaseReference firebaseRef;
    private MqttAsyncClient mqttClient;
    private final Random random = new Random();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private MqttConnectOptions mqttConnectOptions;

    @PostConstruct
    public void init() {
        try {
            // Initialize Firebase first
            initializeFirebase();

            // Configure MQTT client
            if (mqttConnectOptions.getServerURIs() == null || mqttConnectOptions.getServerURIs().length == 0) {
                mqttConnectOptions.setServerURIs(new String[]{brokerUrl});
            }

            this.mqttClient = new MqttAsyncClient(
                    brokerUrl,
                    clientIdPrefix + "-" + UUID.randomUUID()
            );

            this.mqttClient.setCallback(this);
            this.mqttClient.connect(mqttConnectOptions).waitForCompletion();

            logger.info("Connected to MQTT broker: {}", brokerUrl);
            startAutomaticPublishing();

        } catch (MqttException e) {
            logger.error("MQTT connection failed", e);
            scheduleReconnect();
        } catch (Exception e) {
            logger.error("Service initialization error", e);
        }
    }

    private void initializeFirebase() {
        try {
            logger.info("Loading Firebase configuration from: {}", firebaseConfigPath);

            // Load configuration file from classpath
            InputStream serviceAccount = getClass().getClassLoader().getResourceAsStream(firebaseConfigPath);

            if (serviceAccount == null) {
                throw new IOException("Firebase config file not found in classpath: " + firebaseConfigPath);
            }

            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl(firebaseDatabaseUrl)
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                logger.info("Firebase initialized successfully");
            }

            this.firebaseRef = FirebaseDatabase.getInstance().getReference("wind_measurements");
            logger.info("Firebase reference configured");

        } catch (IOException e) {
            logger.error("ERROR: Firebase configuration file not found", e);
            logger.error("Please verify:");
            logger.error("1. File '{}' exists in src/main/resources/", firebaseConfigPath);
            logger.error("2. Filename is correct (including case sensitivity)");
            logger.error("3. Project was rebuilt after adding the file");
            throw new RuntimeException("Critical failure: Firebase config file not found", e);
        } catch (Exception e) {
            logger.error("Firebase initialization error", e);
            throw new RuntimeException("Firebase configuration failed", e);
        }
    }

    private void scheduleReconnect() {
        scheduler.schedule(() -> {
            logger.info("Attempting reconnection...");
            init();
        }, 1, TimeUnit.MINUTES);
    }

    private void startAutomaticPublishing() {
        scheduler.scheduleAtFixedRate(this::publishWindData, 0, simulationInterval, TimeUnit.SECONDS);
    }

    public void publishWindData() {
        try {
            if (firebaseRef == null) {
                logger.warn("Firebase reference not initialized, attempting reinitialization...");
                initializeFirebase();
                if (firebaseRef == null) {
                    throw new IllegalStateException("Failed to initialize Firebase reference");
                }
            }

            WindData data = new WindData(
                    minSpeed + (random.nextDouble() * (maxSpeed - minSpeed)),
                    random.nextDouble() * 360
            );

            publishMqttData(data);
            saveToFirebase(data);
            sendWebSocketUpdate(data);

            logger.info("Data published - Speed: {:.2f} km/h, Direction: {:.1f}°",
                    data.getSpeed(), data.getDirection());

        } catch (Exception e) {
            logger.error("Failed to publish data", e);
        }
    }

    private void publishMqttData(WindData data) throws MqttException {
        String payload = String.format("{\"speed\":%.2f,\"direction\":%.2f,\"timestamp\":%d}",
                data.getSpeed(), data.getDirection(), data.getTimestamp());

        MqttMessage message = new MqttMessage(payload.getBytes());
        message.setQos(1);
        message.setRetained(true);

        mqttClient.publish("wind/data", message);
    }

    private void sendWebSocketUpdate(WindData data) {
        try {
            messagingTemplate.convertAndSend("/topic/wind_updates", data);
        } catch (Exception e) {
            logger.error("Failed to send WebSocket update", e);
        }
    }

    private void saveToFirebase(WindData data) {
        try {
            firebaseRef.push().setValue(data, (error, ref) -> {
                if (error != null) {
                    logger.error("Firebase save error: {}", error.getMessage());
                } else {
                    logger.debug("Data saved to Firebase with key: {}", ref.getKey());
                }
            });
        } catch (Exception e) {
            logger.error("Failed to save data to Firebase", e);
        }
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        logger.info(reconnect ? "Reconnected to broker" : "Connected to broker");
    }

    @Override
    public void connectionLost(Throwable cause) {
        logger.warn("Connection lost: {}", cause.getMessage());
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        logger.debug("Message received: {} - {}", topic, new String(message.getPayload()));
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {}

    @PreDestroy
    public void cleanup() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
            }
            scheduler.shutdown();
        } catch (MqttException e) {
            logger.error("Error during cleanup", e);
        }
    }
}