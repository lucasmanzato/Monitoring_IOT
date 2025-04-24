package com.example.iot_monitoring;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${firebase.database.url}")
    private String databaseUrl;

    @Value("${firebase.config.path}")
    private String configPath;

    @PostConstruct
    public void initialize() {
        try {
            logger.info("Tentando carregar configuração do Firebase do caminho: {}", configPath);

            // Remove o prefixo 'classpath:' se existir
            String resourcePath = configPath.startsWith("classpath:")
                    ? configPath.substring("classpath:".length())
                    : configPath;

            InputStream serviceAccount = new ClassPathResource(resourcePath).getInputStream();

            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl(databaseUrl)
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                logger.info("Firebase inicializado com sucesso");
            }
        } catch (IOException e) {
            logger.error("ERRO: Arquivo de configuração do Firebase não encontrado", e);
            logger.error("Por favor, verifique:");
            logger.error("1. O arquivo '{}' está em src/main/resources/", configPath);
            logger.error("2. O nome do arquivo está correto (incluindo maiúsculas/minúsculas)");
            logger.error("3. O projeto foi reconstruído após adicionar o arquivo");
            throw new RuntimeException("Falha ao inicializar Firebase - Arquivo não encontrado", e);
        } catch (Exception e) {
            logger.error("Erro ao inicializar Firebase", e);
            throw new RuntimeException("Falha na configuração do Firebase", e);
        }
    }
}