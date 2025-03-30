package com.example.iot_monitoring;

import com.google.firebase.database.*;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Random;
import java.util.concurrent.*;

@Service
public class WindSensorSimulator {
    private static final Logger logger = LoggerFactory.getLogger(WindSensorSimulator.class);

    // Configurações MQTT
    @Value("${mqtt.broker.url:tcp://localhost:1883}")
    private String brokerUrl;

    @Value("${mqtt.client.prefix:wind-sensor-publisher}")
    private String clientIdPrefix;

    @Value("${wind.simulation.interval:5}")
    private int simulationInterval;

    // Firebase
    private DatabaseReference firebaseRef;
    private MqttClient mqttClient;
    private final Random random = new Random();
    private final MqttConnectOptions connectOptions;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public WindSensorSimulator() {
        this.connectOptions = new MqttConnectOptions();
        this.connectOptions.setAutomaticReconnect(true);
        this.connectOptions.setConnectionTimeout(30);
        this.connectOptions.setKeepAliveInterval(60);

        // Inicializa Firebase
        this.firebaseRef = FirebaseDatabase.getInstance().getReference("wind_measurements");
    }

    @PostConstruct
    public void init() {
        try {
            String clientId = clientIdPrefix + "-" + System.currentTimeMillis();
            this.mqttClient = new MqttClient(brokerUrl, clientId);
            this.mqttClient.connect(connectOptions);
            logger.info("Conectado ao broker MQTT em {}", brokerUrl);

            startAutomaticPublishing();
        } catch (MqttException e) {
            logger.error("Falha na conexão MQTT", e);
        }
    }

    private void startAutomaticPublishing() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                publishWindData();
            } catch (Exception e) {
                logger.error("Erro na publicação automática", e);
            }
        }, 0, simulationInterval, TimeUnit.SECONDS);
    }

    public void publishWindData() {
        try {
            // Gera dados simulados
            double windSpeed = 10 + (random.nextDouble() * 40); // 10-50 km/h
            double windDirection = random.nextDouble() * 360; // 0-359°

            // Publica via MQTT
            publishMqttData(windSpeed, windDirection);

            // Armazena no Firebase
            saveToFirebase(windSpeed, windDirection);

        } catch (Exception e) {
            logger.error("Falha ao publicar dados", e);
        }
    }

    private void publishMqttData(double speed, double direction) throws MqttException {
        MqttMessage speedMessage = new MqttMessage(String.valueOf(speed).getBytes());
        mqttClient.publish("wind/speed", speedMessage);

        MqttMessage directionMessage = new MqttMessage(String.valueOf(direction).getBytes());
        mqttClient.publish("wind/direction", directionMessage);

        logger.info("Dados MQTT publicados - Velocidade: {:.2f} km/h, Direção: {:.1f}°", speed, direction);
    }

    private void saveToFirebase(double speed, double direction) {
        WindData data = new WindData(speed, direction);

        firebaseRef.push().setValue(data, (error, ref) -> {
            if (error != null) {
                logger.error("Erro ao salvar no Firebase: {}", error.getMessage());
            } else {
                logger.info("Dados salvos no Firebase: {}", data);
            }
        });
    }

    @PreDestroy
    public void cleanup() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
            }
            scheduler.shutdown();
        } catch (MqttException e) {
            logger.error("Erro ao encerrar", e);
        }
    }
}

@RestController
@RequestMapping("/api/wind-sensor")
class WindSensorController {
    private final WindSensorSimulator sensorSimulator;

    public WindSensorController(WindSensorSimulator sensorSimulator) {
        this.sensorSimulator = sensorSimulator;
    }

    @GetMapping("/publish")
    public String triggerWindMeasurement() {
        sensorSimulator.publishWindData();
        return "Dados de vento publicados com sucesso";
    }

    @GetMapping("/status")
    public String getStatus() {
        return "Simulador de sensor de vento operacional";
    }
}