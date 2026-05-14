# PetTrack — Sistema Operacional da Saúde Contínua do Pet

> Projeto acadêmico desenvolvido para o **Challenge 2026 — FIAP + Clyvo Vet**  
> Matéria: **Java Advanced**

---

# 📌 Sobre o Projeto

O **PetTrack** é uma plataforma de saúde contínua para pets, conectando:

- Tutores
- Clínicas veterinárias
- Dispositivos IoT
- Inteligência Artificial
- Aplicativo mobile

O sistema transforma o acompanhamento veterinário de um modelo **reativo** para um modelo **preventivo, inteligente e contínuo**.

O **Spring Boot** é o núcleo central da arquitetura, responsável por:

- Expor APIs REST
- Persistir dados no Oracle
- Integrar serviços externos
- Receber dados de IoT
- Centralizar regras de negócio

---

# 🏗 Arquitetura do Sistema

```txt
React Native App
        ↓
Spring Boot API
        ↓
Oracle Database

Node-RED + ESP32 + MQTT
        ↓
Spring Boot API
        ↓
TB_ALERTA / TB_COLLAR_LEITURA

Spring Boot API
        ↓
Python FastAPI + Claude Vision
        ↓
BCS Analysis
        ↓
TB_BCS_HISTORICO
```

---

# 🚀 Stack Tecnológica

## Backend

- Java 21
- Spring Boot 3
- Spring Data JPA
- Hibernate
- Bean Validation
- Swagger/OpenAPI
- WebClient
- Spring Cache
- Maven

## Banco de Dados

- Oracle Database

## Mobile

- React Native
- Expo

## Cloud

- Azure VM
- Docker Compose

## IoT

- ESP32
- MQTT TLS
- HiveMQ
- Node-RED

## Inteligência Artificial

- Python FastAPI
- Claude Vision API

---

# 📂 Estrutura de Pacotes

```txt
com.fiap.pettrack
│
├── config/
├── controller/
├── dto/
├── entity/
├── exception/
├── repository/
├── service/
└── integration/
```

---

# 📦 Dependências Principais

```xml
<dependencies>

    <!-- REST API -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- JPA -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

    <!-- Validation -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Oracle -->
    <dependency>
        <groupId>com.oracle.database.jdbc</groupId>
        <artifactId>ojdbc11</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Swagger -->
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        <version>2.5.0</version>
    </dependency>

    <!-- Cache -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-cache</artifactId>
    </dependency>

    <!-- WebClient -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>

    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

</dependencies>
```

---

# 🗄 Banco de Dados

## Tabelas Principais

```txt
TB_TUTOR
TB_CLINICA
TB_PET
TB_EVENTO_CLINICO
TB_PROTOCOLO_PREVENTIVO
TB_MEDICAMENTO
TB_ADESAO_MEDICAMENTO
TB_NOTIFICACAO
TB_SCORE_HISTORICO
TB_BCS_HISTORICO
TB_COLLAR_LEITURA
TB_ALERTA
TB_LOG_ERRO
```

---

# 🔗 Principais Endpoints

## Tutores

```http
GET    /api/v1/tutores
GET    /api/v1/tutores/{id}
POST   /api/v1/tutores
PUT    /api/v1/tutores/{id}
DELETE /api/v1/tutores/{id}
```

## Pets

```http
GET    /api/v1/pets
GET    /api/v1/pets/{id}
GET    /api/v1/pets/tutor/{id_tutor}
POST   /api/v1/pets
PUT    /api/v1/pets/{id}
DELETE /api/v1/pets/{id}
```

## Alertas

```http
POST   /api/v1/alertas
GET    /api/v1/alertas/pet/{id_pet}
GET    /api/v1/alertas/pendentes
PUT    /api/v1/alertas/{id}/resolver
```

## Collar IoT

```http
POST   /api/v1/collar/leitura
GET    /api/v1/collar/{id_pet}/ultimas
```

## BCS + IA

```http
POST   /api/v1/bcs/analyze
GET    /api/v1/bcs/pet/{id_pet}/historico
```

---

# 🔥 Funcionalidades

- CRUD completo de tutores, pets e clínicas
- Histórico clínico
- Protocolos preventivos
- Controle medicamentoso
- Health Score
- Monitoramento IoT
- Alertas inteligentes
- Integração com IA para análise corporal (BCS)
- API REST documentada
- Cache de consultas frequentes
- Tratamento global de exceções

---

# 🧠 Integrações Externas

## Python FastAPI — BCS

Fluxo:

```txt
Mobile → Spring Boot → Python FastAPI → Claude Vision → Oracle
```

O Spring Boot envia imagens para análise corporal via IA e persiste o resultado no banco.

---

## Node-RED + ESP32

Fluxo:

```txt
ESP32 → MQTT → Node-RED → Spring Boot → Oracle
```

Responsável pelo envio de:

- Temperatura
- Atividade
- Alertas críticos

---

# ⚙️ Requisitos Técnicos da Matéria

- JPA/Hibernate
- Relacionamentos entre entidades
- DTOs Request/Response
- Bean Validation
- Paginação
- Ordenação
- Cache
- Swagger/OpenAPI
- ControllerAdvice
- Repository Pattern
- Service Layer
- GitHub público
- Postman Collection

---

# ▶️ Como Executar

## 1. Clonar o projeto

```bash
git clone https://github.com/seu-usuario/pettrack.git
```

---

## 2. Entrar na pasta

```bash
cd pettrack
```

---

## 3. Configurar o application.properties

```properties
spring.datasource.url=jdbc:oracle:thin:@oracle.fiap.com.br:1521:ORCL
spring.datasource.username=RM
spring.datasource.password=SENHA

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

---

## 4. Rodar o projeto

```bash
mvn spring-boot:run
```

---

# 📘 Swagger

Após iniciar a aplicação:

```txt
http://localhost:8080/swagger-ui/index.html
```

---

# 👥 Equipe

## FIAP — Challenge 2026

Projeto desenvolvido em parceria com a **Clyvo Vet**.

---

# 📄 Licença

Projeto acadêmico sem fins comerciais.
