package com.example.iot_monitoring;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wind")
public class WindSensorController {

    private final WindSensorSimulator sensorSimulator;

    public WindSensorController(WindSensorSimulator sensorSimulator) {
        this.sensorSimulator = sensorSimulator;
    }

    @GetMapping("/publish")
    public String publishData() {
        sensorSimulator.publishWindData();
        return "Dados publicados com sucesso";
    }

    @GetMapping("/config")
    public String getConfig() {
        return "Sistema de monitoramento de vento operacional";
    }
}