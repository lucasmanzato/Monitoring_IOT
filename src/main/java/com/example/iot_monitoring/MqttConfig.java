package com.example.iot_monitoring;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MqttConfig {

    @Value("${mqtt.broker.url:tcp://localhost:1883}")
    private String brokerUrl;

    @Bean
    public MqttConnectOptions mqttConnectOptions() {
        if (brokerUrl == null || brokerUrl.trim().isEmpty()) {
            throw new IllegalStateException("MQTT broker URL must be configured");
        }

        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[]{brokerUrl});
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setConnectionTimeout(30);
        options.setKeepAliveInterval(60);
        options.setWill("wind/status", "sensor-offline".getBytes(), 2, true);
        return options;
    }
}