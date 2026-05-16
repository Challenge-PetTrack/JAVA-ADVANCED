# 🐾 PetTrack — Sistema Operacional da Saúde Contínua do Pet

> Challenge FIAP 2026 · Turma 2TDS Fevereiro · Parceiro: **Clyvo Vet**

---

## 📋 Descrição

O **PetTrack** é uma plataforma de saúde contínua que conecta tutores, pets e clínicas veterinárias. O sistema centraliza o histórico clínico dos animais, monitora a adesão a medicamentos, analisa o Body Condition Score (BCS) por inteligência artificial e coleta dados em tempo real via collar IoT.

---

## 👥 Equipe

| Nome | RM |
|---|---|
| Gabriel Sbrana Campos | RM 565849 |
| Moisés Waidemann | RM 563719 |
| Thiago Rodrigues da Mota | RM 563765 |
| Richard Freitas | RM 566127 |

---

## 🏗️ Arquitetura

```
React Native (Mobile)
        ↓
Java Spring Boot (API Central) ←→ Oracle DB
        ↑               ↑
Python FastAPI        Node-RED
(BCS via GPT-4o)   (ESP32 via MQTT)
```

### Stack Completa

| Camada | Tecnologia |
|---|---|
| Mobile | React Native + Expo |
| Backend | Java 17 + Spring Boot 3 |
| Banco de Dados | Oracle DB (oracle.fiap.com.br:1521:ORCL) |
| IA / BCS | Python FastAPI + GPT-4o Vision |
| IoT | ESP32 + MQTT TLS HiveMQ + Node-RED |
| Cloud | Azure VM + Docker Compose |

---

## 📦 Estrutura de Pacotes

```
com.fiap.pettrack
├── config/          → Swagger, Cache
├── control/         → REST Controllers (12)
├── dto/             → DTOs com Bean Validation (12)
├── mapper/          → MapStruct Mappers (12)
├── model/           → Entidades JPA (12) + Enums (7)
│   └── enums/
├── repository/      → JPA Repositories (12)
├── service/         → CachingServices (12) + PaginacaoServices (12)
└── validations/     → GlobalExceptionHandler
```

---

## 🗄️ Banco de Dados

O projeto utiliza Oracle DB com tabelas já criadas via DDL. As entidades JPA apenas mapeiam as tabelas existentes — **não recria o banco**.

### Tabelas Mapeadas

```
TB_TUTOR               TB_CLINICA
TB_PET                 TB_EVENTO_CLINICO
TB_PROTOCOLO_PREVENTIVO TB_MEDICAMENTO
TB_ADESAO_MEDICAMENTO  TB_NOTIFICACAO
TB_SCORE_HISTORICO     TB_BCS_HISTORICO
TB_COLLAR_LEITURA      TB_ALERTA
```

---

## 🔄 Fluxo de Dados

```
Mobile → POST /api → Spring Boot → Oracle (CRUD principal)

Node-RED → HTTP POST → Spring Boot → TB_COLLAR_LEITURA
Node-RED → HTTP POST → Spring Boot → TB_ALERTA

Mobile → foto → Python FastAPI → GPT-4o Vision → BCS
                                               ↓
                              Spring Boot → TB_BCS_HISTORICO
```

---

## 🚀 Como Rodar

### Pré-requisitos

- Java 17+
- Maven 3.8+
- IntelliJ IDEA (recomendado)
- Acesso ao Oracle FIAP (`oracle.fiap.com.br:1521:ORCL`)

### Passos

**1. Clone o repositório**
```bash
git clone https://github.com/Challenge-PetTrack/JAVA-ADVANCED.git
cd JAVA-ADVANCED
```

**2. Configure o `application.properties`**
```properties
spring.datasource.username=SEU_RM
spring.datasource.password=SUA_SENHA
```

**3. Rode pelo IntelliJ**

Abra o projeto no IntelliJ IDEA e execute a classe `PetTrackApplication.java`.

**4. Acesse o Swagger**
```
http://localhost:8080/swagger
```

---

## 📡 Endpoints Principais

| Recurso | Base URL |
|---|---|
| Tutores | `/tutor` |
| Clínicas | `/clinica` |
| Pets | `/pet` |
| Eventos Clínicos | `/evento` |
| Protocolos Preventivos | `/protocolo` |
| Medicamentos | `/medicamento` |
| Adesão Medicamentosa | `/adesao` |
| Notificações | `/notificacao` |
| Health Score | `/score` |
| BCS Histórico | `/bcs` |
| Collar Leitura | `/collar` |
| Alertas | `/alerta` |

Cada recurso expõe:

```
GET    /todos          → lista todos
GET    /paginar        → lista paginada
GET    /{id}           → busca por ID
POST   /novo           → cria novo registro
PUT    /atualizar/{id} → atualiza registro
DELETE /remover/{id}   → remove registro
```

---

## ✅ Requisitos Técnicos Atendidos

- [x] Entidades JPA mapeadas com relacionamentos (`@OneToMany`, `@ManyToOne`)
- [x] Bean Validation nos DTOs (`@NotBlank`, `@Email`, `@Min`, `@Max`, `@DecimalMin`, `@DecimalMax`)
- [x] DTOs separados das entidades com MapStruct
- [x] Paginação nos endpoints de listagem (`PageRequest`)
- [x] Cache nas consultas frequentes (`@Cacheable`, `@CacheEvict`)
- [x] Tratamento global de exceções (`@RestControllerAdvice`)
- [x] Documentação Swagger/OpenAPI (`springdoc-openapi`)
- [x] Padrões REST com status HTTP corretos (200, 201, 204, 400, 404)
- [x] JPQL e Spring JPA Query Methods
- [x] Design Patterns: Repository, Service, DTO, Mapper

---

## 🔗 Links

- **GitHub:** https://github.com/Challenge-PetTrack/JAVA-ADVANCED
- **Swagger:** http://localhost:8080/swagger

---

## 📅 Cronograma

| Sprint | Período | Foco |
|---|---|---|
| Sprint 1 | 23/04 – 28/04 | Banco, estrutura e entidades JPA |
| Sprint 2 | 28/04 – 04/05 | Services, Controllers e integrações |
| Sprint 3 | 04/05 – 08/05 | Qualidade, documentação e entrega |
