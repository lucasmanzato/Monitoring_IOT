# Monitor de Vento IoT com Spring Boot, MQTT e Firebase 🌬️

## Visão Geral 🌐

Este projeto é um sistema de monitoramento de dados de vento em tempo real, desenvolvido como parte da disciplina de Sistemas Distribuídos. Ele simula um sensor de vento que envia dados de velocidade e direção para um servidor backend. Os dados são então persistidos no Firebase Realtime Database e transmitidos via WebSockets para um frontend web que exibe as informações em tempo real, incluindo um gráfico e um histórico de leituras. O sistema utiliza MQTT como middleware para a comunicação entre o simulador do sensor e o backend.

## Funcionalidades ✨

* Simulação de dados de sensor de vento (velocidade e direção).
* Publicação de dados do sensor via MQTT.
* Processamento e armazenamento de dados no Firebase Realtime Database.
* Transmissão de dados em tempo real para o frontend via WebSockets (STOMP sobre SockJS).
* Visualização de dados em tempo real em uma interface web:
    * Velocidade e direção atuais.
    * Seta indicadora da direção do vento.
    * Gráfico com histórico de velocidade e direção.
    * Lista das últimas atualizações.

## Arquitetura do Sistema ⚙️

O sistema é composto pelos seguintes componentes principais:

1.  **Simulador do Sensor de Vento (`WindSensorSimulator`)**:
    * Uma classe Java (parte do backend Spring Boot) que gera dados aleatórios de velocidade e direção do vento.
    * Publica esses dados em tópicos MQTT (`wind/data`).
    * Salva os dados diretamente no Firebase Realtime Database.
    * Envia os dados via WebSockets para o frontend.

2.  **Broker MQTT (Ex: Mosquitto)**:
    * Atua como intermediário para as mensagens entre o simulador do sensor e outros possíveis serviços MQTT (embora no design atual, o `MqttService` também faz parte do backend e poderia ser um consumidor, o `WindSensorSimulator` é o publisher principal para o tópico `wind/data`).
    * O `MqttService` no backend se inscreve nos tópicos `wind/speed` e `wind/direction` (que não são os mesmos que o simulador publica `wind/data`). *Nota: Há uma aparente desconexão aqui ou uma funcionalidade não totalmente explorada no `MqttService` versus `WindSensorSimulator` em termos de quem publica o quê e quem consome o quê via MQTT para o backend em si. O simulador publica em `wind/data` e o `MqttService` espera em `wind/speed` e `wind/direction`.*

3.  **Aplicação Backend (Spring Boot)**:
    * **`FirebaseConfig`**: Inicializa a conexão com o Firebase Admin SDK.
    * **`MqttConfig` e `MqttService`**: Configuram e gerenciam a conexão com o broker MQTT (para receber dados, embora o fluxo principal de dados venha do `WindSensorSimulator` integrado).
    * **`WebSocketConfig`**: Configura o endpoint WebSocket (`/iot-websocket`) e o message broker STOMP.
    * **`WindDataController`**: Manipula mensagens WebSocket, permitindo o envio de dados para tópicos específicos (ex: `/topic/wind_updates`). O `WindSensorSimulator` usa o `SimpMessagingTemplate` para enviar dados para este tópico.
    * **`WindSensorController`**: Um controlador REST para possíveis interações (ex: `/api/wind/publish` para disparar uma publicação manual, `/api/wind/config`).
    * **`WindData`**: Modelo de dados para informações do vento.

4.  **Firebase Realtime Database**:
    * Banco de dados NoSQL na nuvem usado para persistir as leituras do sensor de vento.

5.  **Frontend (HTML, CSS, JavaScript)**:
    * Interface web para visualização dos dados.
    * Conecta-se ao backend via WebSocket (SockJS e STOMP) no endpoint `/iot-websocket` e se inscreve no tópico `/topic/wind_updates`.
    * Exibe a velocidade, direção, seta indicadora, gráfico de histórico e lista de atualizações.
    * Utiliza Chart.js para a renderização do gráfico.

**Fluxo de Dados Principal:**

[Simulador de Sensor (Backend)] --(MQTT: wind/data)--> [Broker MQTT]
                                       |
                                       +--(Salva no Firebase)--> [Firebase Realtime Database]
                                       |
                                       +--(WebSocket via SimpMessagingTemplate)--> [Frontend Web]


Nota: O MqttService está configurado para ouvir wind/speed e wind/direction,
mas o WindSensorSimulator publica em wind/data. Esta parte do fluxo MQTT
para o MqttService pode não estar sendo ativamente usada para o display do frontend,
que recebe dados diretamente do WindSensorSimulator via WebSocket.

## Tecnologias Utilizadas 🚀

* **Backend**:
    * Java 21
    * Spring Boot 3.4.4
    * Spring Web
    * Spring WebSocket (STOMP sobre SockJS)
    * Spring Integration MQTT
    * Eclipse Paho MQTT Client
    * Firebase Admin SDK
    * Maven (Gerenciador de dependências)
* **Frontend**:
    * HTML5
    * CSS3
    * JavaScript (ES6+)
    * SockJS Client
    * Stomp.js
    * Chart.js
* **Middleware**:
    * Broker MQTT (ex: Mosquitto)
* **Database**:
    * Firebase Realtime Database
* **Logging**:
    * SLF4J

## Pré-requisitos 📋

* JDK 21 ou superior (ex: OpenJDK, Oracle JDK)
* Apache Maven 3.6+
* Um broker MQTT instalado e em execução (ex: [Mosquitto](https://mosquitto.org/download/)).
* Uma conta Google e um projeto Firebase configurado.
* Navegador web moderno (Chrome, Firefox, Edge, etc.).

## Configuração 🛠️

### 1. Firebase

1.  **Crie um Projeto Firebase**: Acesse o [Console do Firebase](https://console.firebase.google.com/) e crie um novo projeto (ou use um existente).
2.  **Configure o Realtime Database**:
    * No menu lateral do seu projeto Firebase, vá em "Realtime Database".
    * Crie um novo banco de dados. Escolha a localização e inicie no **modo de teste** (para regras de leitura/escrita permissivas durante o desenvolvimento) ou configure regras de segurança apropriadas.
    * Copie a URL do seu Realtime Database. Ela será algo como `https://SEU-PROJETO-ID-default-rtdb.firebaseio.com/`.
3.  **Gere a Chave de Conta de Serviço (JSON)**:
    * No console do Firebase, vá em "Configurações do projeto" (ícone de engrenagem ao lado de "Visão geral do projeto").
    * Vá para a aba "Contas de serviço".
    * Clique em "Gerar nova chave privada" e confirme. Um arquivo JSON será baixado.
4.  **Configure o Backend**:
    * Renomeie o arquivo JSON baixado para um nome de sua escolha (ex: `firebase-service-key.json`).
    * Mova este arquivo para a pasta `src/main/resources/` do seu projeto backend.
    * Abra o arquivo `src/main/resources/application.properties` e configure as seguintes propriedades:
        firebase.database.url=COLE_A_URL_DO_SEU_FIREBASE_DATABASE_AQUI
        firebase.config.path=NOME_DO_SEU_ARQUIVO_JSON_AQUI.json
        Exemplo:
        firebase.database.url=https://iot-finalversion-default-rtdb.firebaseio.com/
        firebase.config.path=iot-finalversion-firebase-adminsdk-fbsvc-0a80e0ff6c.json

### 2. Broker MQTT

1.  **Instale e Inicie o Mosquitto** (ou outro broker MQTT de sua preferência).
    * Para Mosquitto, siga as instruções de instalação para o seu sistema operacional.
    * Após a instalação, inicie o serviço Mosquitto. Por padrão, ele roda na porta `1883` sem autenticação.
2.  **Configure o Backend**:
    * No arquivo `src/main/resources/application.properties`, verifique a configuração do broker MQTT:
        mqtt.broker.url=tcp://localhost:1883
        # Se o seu broker estiver em outra máquina ou porta, ajuste aqui.
        # Ex: mqtt.broker.url=tcp://IP_DO_BROKER:1883
        *Nota: A classe `MqttConfig.java` também pega essa URL, mas a classe `WindSensorSimulator.java` e `MqttService.java` também têm a URL definida via `@Value`, o que é bom para consistência.*

### 3. Configuração de Rede (IPs)

* **Backend**:
    * O backend Spring Boot, por padrão (definido no `application.properties`), rodará na porta `8080`.
        server.port=8080
    * O `server.address=0.0.0.0` significa que ele aceitará conexões de qualquer interface de rede na máquina.

* **Frontend**:
    * O frontend (`script.js`) precisa saber o endereço IP da máquina onde o backend (Spring Boot) está rodando para se conectar ao WebSocket.
    * Abra o arquivo `frontend/script.js` e edite a constante `SERVER_IP`:
        // 1. CONFIGURAÇÃO DA CONEXÃO
        const SERVER_IP = 'SEU_IP_DE_REDE_DA_MAQUINA_BACKEND'; // Ex: '192.168.1.21'
        // Se o backend estiver rodando na mesma máquina que o navegador, você pode usar 'localhost'.
        const WS_URL = `http://${SERVER_IP}:8080/iot-websocket`;
    * **Importante**: `SERVER_IP` deve ser o IP da máquina backend que é acessível pela máquina onde o navegador está executando o frontend. Se estiverem na mesma rede local, use o IP local da máquina backend (ex: `192.168.x.x`).

## Como Executar 🚀

### 1. Backend (Spring Boot)

1.  Navegue até o diretório raiz do projeto backend (onde está o `pom.xml`).
2.  Compile e execute a aplicação usando Maven:
    mvn spring-boot:run
    Ou, para gerar um JAR executável:
    mvn clean package
    java -jar target/iot-monitoring-0.0.1-SNAPSHOT.jar
3.  O backend estará rodando na porta `8080` (ou na porta configurada). Você deverá ver logs indicando a inicialização do Firebase, MQTT e WebSocket.

### 2. Frontend

1.  Após configurar o `SERVER_IP` corretamente em `frontend/script.js`.
2.  Abra o arquivo `frontend/index.html` diretamente no seu navegador web.
3.  A página tentará se conectar ao backend via WebSocket. Verifique o status da conexão na interface e no console do navegador (F12).

## Estrutura do Projeto 📁 (Simplificada)

iot-monitoring/
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/iot_monitoring/
│   │   │   │   ├── FirebaseConfig.java         # Configuração do Firebase
│   │   │   │   ├── IotMonitoringApplication.java # Classe principal Spring Boot
│   │   │   │   ├── MqttConfig.java           # Configuração do cliente MQTT (opções)
│   │   │   │   ├── MqttService.java          # Lógica para interagir com MQTT (consumidor)
│   │   │   │   ├── WebSocketConfig.java      # Configuração do WebSocket
│   │   │   │   ├── WindData.java             # Modelo de dados
│   │   │   │   ├── WindDataController.java   # Controlador WebSocket
│   │   │   │   ├── WindSensorController.java # Controlador REST (para simulação)
│   │   │   │   └── WindSensorSimulator.java  # Simulador do sensor (publicador MQTT, Firebase, WebSocket)
│   │   │   └── resources/
│   │   │       ├── application.properties    # Configurações da aplicação
│   │   │       └── SEU_ARQUIVO_JSON_FIREBASE.json # Chave do Firebase
│   ├── pom.xml                             # Dependências e build do Maven
│   └── ...
└── frontend/
    ├── index.html                          # Estrutura da página
    ├── style.css                           # Estilos da página
    └── script.js                           # Lógica do cliente (WebSocket, Chart.js)

## Possíveis Problemas e Dicas 💡

* **Firewall**: Certifique-se de que a porta do backend (ex: `8080`) e a porta do broker MQTT (ex: `1883`) não estão bloqueadas por um firewall na máquina do servidor ou na rede.
* **Endereço IP**:
    * Use o IP correto da rede local para `SERVER_IP` no `script.js` se o backend e o frontend estiverem em máquinas diferentes na mesma rede.
    * Se o IP da máquina backend mudar, você precisará atualizar o `SERVER_IP` no `script.js`.
* **Erro de Conexão WebSocket**: Verifique o console do navegador (F12) e os logs do backend Spring Boot para mensagens de erro. Certifique-se que o `SERVER_IP` e a porta estão corretos e que o backend está acessível.
* **Firebase Rules**: Se você não usou o "modo de teste" para o Realtime Database, certifique-se de que suas regras de segurança permitem leitura e escrita para os caminhos que a aplicação usa (ex: `/wind_measurements`).
* **Dependências MQTT/Firebase**: Se o backend falhar ao iniciar, verifique os logs para erros relacionados à inicialização do MQTT ou Firebase. Pode ser um problema com o caminho do arquivo JSON do Firebase, a URL do banco de dados ou a URL do broker MQTT.