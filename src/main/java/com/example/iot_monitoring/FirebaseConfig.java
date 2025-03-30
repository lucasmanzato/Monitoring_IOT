package com.example.iot_monitoring;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);
    // URL atualizado para o novo banco de dados
    private static final String FIREBASE_DATABASE_URL = "https://iotmqtt-1656e-default-rtdb.firebaseio.com/";

    @PostConstruct
    public void initialize() {
        // Evita inicialização redundante
        if (!FirebaseApp.getApps().isEmpty()) {
            logger.info("[FIREBASE] Firebase já foi inicializado anteriormente");
            return;
        }

        try (InputStream serviceAccount = new FileInputStream("src/main/resources/iotmqtt-1656e-firebase-adminsdk-fbsvc-069606de25.json")) {
            if (serviceAccount == null) {
                throw new IllegalStateException("Arquivo de credenciais do Firebase não encontrado em src/main/resources");
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl(FIREBASE_DATABASE_URL)
                    .build();

            FirebaseApp.initializeApp(options);
            logger.info("[FIREBASE] Inicializado com sucesso | DatabaseURL: {}", FIREBASE_DATABASE_URL);

        } catch (Exception e) {
            logger.error("[FIREBASE] Erro crítico na inicialização: ", e);
            throw new RuntimeException("Falha ao inicializar Firebase", e);
        }
    }
}