package com.example.iot_monitoring;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;

@SpringBootApplication
@EnableScheduling
public class IotMonitoringApplication {
	private static final Logger logger = LoggerFactory.getLogger(IotMonitoringApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(IotMonitoringApplication.class, args);
	}

	@Configuration
	public static class FirebaseConfig {
		// URL atualizado para o novo banco de dados de vento/direção
		private static final String FIREBASE_DATABASE_URL = "https://iotmqtt-1656e-default-rtdb.firebaseio.com/";

		@PostConstruct
		public void initialize() {
			// Verifica se o Firebase já foi inicializado
			if (!FirebaseApp.getApps().isEmpty()) {
				logger.info("[WIND MONITOR] Firebase já inicializado");
				return;
			}

			try (InputStream serviceAccount = new FileInputStream("src/main/resources/iotmqtt-1656e-firebase-adminsdk-fbsvc-069606de25.json")) {
				if (serviceAccount == null) {
					throw new IllegalStateException("Arquivo de credenciais do Firebase não encontrado");
				}

				FirebaseOptions options = FirebaseOptions.builder()
						.setCredentials(GoogleCredentials.fromStream(serviceAccount))
						.setDatabaseUrl(FIREBASE_DATABASE_URL)
						.build();

				FirebaseApp.initializeApp(options);
				logger.info("[WIND MONITOR] Firebase inicializado | URL: {}", FIREBASE_DATABASE_URL);

			} catch (IOException e) {
				logger.error("[WIND MONITOR] Erro na inicialização: ", e);
				throw new RuntimeException("Falha ao inicializar Firebase", e);
			}
		}
	}
}