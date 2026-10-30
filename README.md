# 🐾 PetTrack — Sistema Operacional de Saúde Animal e Monitoramento Contínuo

> **FIAP Challenge 2026 · 2º Ano ADS (Turmas de Fevereiro · 2º Semestre)**  
> **Disciplina:** Java Advanced — **Sprint 4: Consolidação Final, Deploy Online, Procedures Oracle, Relatórios de Saúde com IA e Integração Multidisciplinar**  
> **Parceiro de Negócio:** Clyvo Vet  

---

## 👥 Integrantes do Grupo

| Nome Completo | RM | Função Principal no Projeto |
| :--- | :--- | :--- |
| **Gabriel Sbrana Campos** | RM 565849 | Backend · Entidades JPA · Cache · Flyway · README |
| **Moisés Waidemann Molinillo Júnior** | RM 563719 | Backend · DTOs · Validações Bean Validation · Paginação · Notificações |
| **Thiago Rodrigues da Mota** | RM 563650 | Backend · Controllers · Health Score · Swagger · UI Thymeleaf |
| **Richard Freitas** | RM 566127 | Backend · Exceções Globais · Integrações de Serviços · Postman · Testes |

---

## 🔗 Links Oficiais da Entrega (Sprint 4)

* 🌐 **Link da Aplicação Online (Deploy):**  
  > `https://pettrack-java.onrender.com` *(ou via túnel público ngrok/cloud informado na apresentação: `http://localhost:8080` em execução de avaliação)*  
* 🎥 **Vídeo de Demonstração e Apresentação da Solução (YouTube - Máx. 15 minutos):**  
  > **[https://youtu.be/1QZ9eECIdqk](https://youtu.be/1QZ9eECIdqk)**  
  > *(Vídeo com participação dos integrantes, apresentando a narrativa do negócio, decisões de arquitetura, navegação completa pelos 4 fluxos de negócio, validações, tela 403 e evidências no banco de dados)*  
* 💻 **Repositório GitHub do Projeto:**  
  > **[https://github.com/Challenge-PetTrack/JAVA-ADVANCED.git](https://github.com/Challenge-PetTrack/JAVA-ADVANCED.git)**  

---

## 📋 Sobre o Projeto PetTrack & Alinhamento com a Clyvo Vet

O mercado veterinário brasileiro enfrenta um gargalo crítico: a **jornada do cuidado pet é fragmentada e reativa**. O tutor só recorre à clínica em momentos de sintomas agudos ou emergências, resultando em abandono de tratamentos, agravamento de doenças preveníveis e baixa fidelização para as clínicas parceiras.

O **PetTrack** surge em parceria com a **Clyvo Vet** como o **sistema operacional do cuidado contínuo**, transformando a relação tutor-clínica-pet através de:
1. **Telemetria Contínua (IoT):** Ingestão em tempo real de temperatura corporal e nível de atividade física.
2. **Health Score Dinâmico (0 a 100):** Cálculo contínuo baseado em sinais vitais, histórico clínico, vacinas e adesão medicamentosa.
3. **Adesão Medicamentosa Ativa:** Controle diário de posologia com cálculo dinâmico de taxa de adesão (%) e alertas de esquecimento.
4. **Health Reports Estruturados com IA Generativa:** Consolidação automática de todo o prontuário do animal (IoT recente, evolução de escore corporal BCS, consultas, medicamentos prescritos, vacinas e clima local) para gerar relatórios preditivos de apoio ao veterinário e tutor.

---

## 🏛️ Narrativa da Solução, Decisões de Design e Arquitetura

O backend foi construído em **Java 21 com Spring Boot 3.3**, priorizando manutenibilidade, segurança e alto desacoplamento:

### 1. Padrões de Projeto e Princípios de Arquitetura
* **Clean Architecture & Separação em Camadas:**
  * **`control` / `web`:** Controllers desacoplados para APIs REST (`PetController`, `HealthReportController`, etc.) e visualização Web MVC (`AdminWebController`, `TutorWebController`, `HealthReportWebController`).
  * **`service` & `service.fluxos`:** Lógica de negócio, cálculos matemáticos do Health Score, orquestração de transações e regras complexas de domínio isoladas dos controladores.
  * **`repository`:** Acesso a dados encapsulado via Spring Data JPA e JDBC direto (`PetProcedureRepository`) para procedures Oracle.
  * **`dto` & `mapper`:** Proteção das entidades de domínio utilizando DTOs dedicados e conversão com **MapStruct 1.6**, evitando exposição direta do modelo relacional.
* **SOLID & DRY (Don't Repeat Yourself):** Métodos coesos, injeção de dependências estrita via construtor (`@RequiredArgsConstructor`), e reutilização de componentes visuais através de fragmentos Thymeleaf (`fragments/layout.html`).
* **Tratamento Global de Exceções:** Implementado com `@ControllerAdvice` e `GlobalExceptionHandler`, interceptando falhas de validação, registros duplicados, violações de integridade e indisponibilidade de serviços com códigos de status HTTP semânticos (200, 201, 204, 400, 401, 403, 404, 409, 413, 429, 502, 503, 504).
* **Otimização de Performance com Cache:** Consultas frequentes e dados analíticos utilizam a anotação `@Cacheable` do Spring Cache para minimizar acessos repetitivos ao banco de dados.

### 2. Controle de Acesso e Segurança Granular (Spring Security 6)
* Autenticação conectada diretamente à tabela `TB_USUARIO` do Oracle com criptografia unidirecional **BCrypt**.
* Controle granular de rotas por perfis de acesso:
  * `ADMIN`: Acesso total à clínica, CRUD de pets com procedures Oracle, simulador IoT e documentação OpenAPI.
  * `VET`: Permissões clínicas, visualização de prontuários, alertas e relatórios de saúde.
  * `TUTOR`: Portal exclusivo do tutor com carteirinha digital dos seus pets, confirmação diária de medicamentos e histórico de relatórios.
* Redirecionador pós-login contextual (`/dashboard`), proteção contra falsificação de requisições (**CSRF**) e página amigável de Acesso Negado (`/403`).

### 3. Versionamento Contínuo de Banco de Dados (Flyway)
O banco de dados relacional é integralmente versionado através de scripts SQL em `src/main/resources/db/migration/`:
* `V1__criar_tabelas_pettrack.sql`: Modelagem DDL de 13 tabelas relacionais em 3FN e sequences.
* `V2__criar_tabelas_seguranca.sql`: Estrutura de autenticação e perfis (`TB_USUARIO`).
* `V3__inserir_dados_iniciais.sql`: Carga de dados inicial com tutores, pets de teste e usuários com senhas hash BCrypt.
* `V4__packages_pettrack.sql`: Pacotes e procedures Oracle (`PKG_PETTRACK_CADASTRO`, `PKG_PETTRACK_CONSULTA`, `PKG_PETTRACK_LOG`) e triggers de auditoria.
* `V5__health_reports.sql`: Tabela `TB_HEALTH_REPORT` com índices compostos, constraints de integridade e persistência de snapshots em CLOB.

---

## 🌐 Integração Multidisciplinar (Conexão com as Demais Disciplinas)

O PetTrack foi concebido como uma solução coesa, integrando diretamente os componentes das matérias do semestre:

```
                               ┌─────────────────────────────────────────────────────┐
                               │       DISRUPTIVE ARCHITECTURES (IoT & IA)           │
                               │  ESP32 (Wokwi) ──► MQTT HiveMQ ──► Node-RED Dashboard│
                               │  FastAPI (Python) + Google Gemini Structured Outputs│
                               │  BCS Computer Vision (Escore Corporal 1-9)          │
                               └─────────────────────────┬───────────────────────────┘
                                                         │ REST HTTP / JSON
                                                         ▼
┌──────────────────────────────────────┐       ┌─────────────────────────────────────┐
│    MOBILE APPLICATION DEVELOPMENT    │       │        JAVA ADVANCED (CORE)         │
│  React Native + Expo                 │◄─────►│  Spring Boot 3.3 + Thymeleaf + MVC  │
│  Carteirinha Digital & Notificações  │ REST  │  Spring Security (BCrypt + Perfis)  │
│  TanStack Query + Firebase Auth      │       │  Flyway Migrations (V1 a V5)        │
└──────────────────────────────────────┘       └──────────────────┬──────────────────┘
                                                                  │
                                      ┌───────────────────────────┴──────────────────────────┐
                                      ▼                                                      ▼
                  ┌───────────────────────────────────────┐              ┌───────────────────────────────────────┐
                  │    MASTERING RELATIONAL & NOSQL       │              │    DEVOPS TOOLS & CLOUD COMPUTING     │
                  │  Oracle FIAP Database                 │              │  Dockerfile seguro (usuário appuser)  │
                  │  Packages: PKG_PETTRACK_*             │              │  Azure CLI + ACI / App Service        │
                  │  Triggers Autônomos de Auditoria      │              │  Pipelines CI/CD (Build & Test)       │
                  │  Exportação de Dataset para MongoDB   │              └───────────────────────────────────────┘
                  └───────────────────────────────────────┘
```

1. **Disruptive Architectures (IoT, IoB & Generative IA):**
   * Recebimento e processamento de telemetria das coleiras inteligentes (ESP32 via Node-RED).
   * Orquestração segura via Bearer Token com o microserviço Python FastAPI para geração de **Health Reports** enriquecidos por **Google Gemini** e persistência imutável no Oracle.
   * Registro e histórico de análises de escore corporal (BCS 1-9) geradas por Visão Computacional.
2. **Mastering Relational and Non-Relational Database:**
   * Modelagem relacional em 3FN executando sobre Oracle FIAP (`oracle.fiap.com.br:1521:ORCL`).
   * Chamada direta a packages empacotados via Spring Data/JDBC: `PKG_PETTRACK_CADASTRO.PRC_INSERT_PET`, `PKG_PETTRACK_LOG.PRC_INSERT_LOG_ERRO`, etc.
   * Trigger autônomo de auditoria para rastreamento de alterações DML.
   * Endpoints `/pet/exportar-json` e `/pet/relatorio-atividades` para gerar o dataset relacional e permitir a importação no **MongoDB**.
3. **DevOps Tools & Cloud Computing:**
   * Containerização da aplicação Spring Boot através de `Dockerfile` que compila e roda a aplicação sob usuário sem privilégios administrativos (`appuser`).
   * Suporte a deploy em nuvem (Azure / Render) com variáveis de ambiente protegidas e esteira de CI/CD.
4. **Mobile Application Development:**
   * Fornecimento de endpoints REST estruturados e documentados no Swagger OpenAPI para alimentar a aplicação mobile dos tutores em React Native.

---

## 🏆 Principais Fluxos de Negócio Implementados

| Fluxo de Negócio | Descrição Detalhada da Implementação |
| :--- | :--- |
| 📡 **Fluxo 1: Telemetria IoT & Alertas Clínicos** | Ingestão de temperatura corporal e atividade física da coleira inteligente. Detecção automática de febre (> 39.5°C) e sedentarismo crônico. Disparo de alertas imediatos, persistência do histórico e recálculo dinâmico do Health Score do pet (0–100). |
| 💊 **Fluxo 2: Gestão de Adesão Medicamentosa** | Interface interativa no Portal do Tutor para confirmação diária de doses prescritas. O sistema calcula a taxa de adesão (%) em tempo real e dispara alertas clínicos caso doses consecutivas sejam esquecidas. |
| 🧠 **Fluxo 3: Health Report com IA Generativa** | Consolidação em um snapshot auditável de todos os dados do pet (IoT das últimas 24h, evolução BCS, consultas e eventos clínicos, medicamentos ativos, vacinas e clima externo). Chamada autenticada ao Gemini com Structured Outputs para gerar diagnóstico compreensivo com recomendações e limitações clínicas, salvo na tabela `TB_HEALTH_REPORT`. |
| 🗄️ **Fluxo 4: Procedimentos Empacotados Oracle** | Operações transacionais executadas através de packages Oracle: cadastro de animais via `PKG_PETTRACK_CADASTRO.PRC_INSERT_PET`, geração de relatórios de subtotais e exportação de dataset JSON para importação NoSQL. |

---

## 🔑 Credenciais Pré-configuradas para Teste e Avaliação

| Perfil de Acesso | E-mail / Usuário | Senha | Permissões e Rota Pós-Login |
| :--- | :--- | :--- | :--- |
| 🛡️ **Administrador** | `admin@pettrack.com` | `admin123` | Acesso total: Painel Geral, Gestão e CRUD de Pets com procedures, Simulador IoT, Documentação Swagger. Destino: `/admin/dashboard`. |
| 🩺 **Veterinário** | `vet@pettrack.com` | `vet123` | Permissões clínicas: Visualização de pacientes, histórico clínico, geração de relatórios de saúde e simulador IoT. Destino: `/admin/dashboard`. |
| 🐶 **Tutor de Pet** | `ana.silva@icloud.com` | `tutor123` | Portal do Tutor: Carteirinha de saúde de seus pets, vacinas, relatórios de saúde e confirmação diária de medicamentos. Destino: `/portal-tutor/dashboard`. |

---

## 🗺️ Rotas e Telas Mapeadas na Aplicação

### 🌐 Camada Web (Thymeleaf)
* **`/login`** — Tela de login estilizada com validação de credenciais e mensagens de feedback.
* **`/dashboard`** — Roteador inteligente que direciona o usuário conforme seu perfil (`ADMIN`, `VET` ou `TUTOR`).
* **`/admin/dashboard`** — Visão geral da clínica com KPIs em tempo real, últimos pacientes e alertas pendentes.
* **`/admin/pets`** — Listagem de pacientes cadastrados com paginação, filtros, exclusão e atalho para relatórios.
* **`/admin/pets/novo`** — Formulário de cadastro de pet integrado à procedure `PKG_PETTRACK_CADASTRO.PRC_INSERT_PET`.
* **`/admin/telemetria`** — Simulador interativo da coleira inteligente IoT com presets de febre e sedentarismo.
* **`/portal-tutor/dashboard`** — Portal do tutor com listagem dos seus pets, alertas e acesso aos relatórios de saúde.
* **`/portal-tutor/pet/{id}`** — Carteirinha digital do pet com vacinas, protocolos preventivos e histórico de consultas.
* **`/portal-tutor/medicamentos`** — Interface de confirmação de doses diárias com cálculo da taxa de adesão em tempo real.
* **`/relatorios/pets/{idPet}`** — Histórico paginado e geração sob demanda de novos Relatórios de Saúde com IA.
* **`/relatorios/pets/{idPet}/{idGeracao}`** — Visualização detalhada do relatório de saúde com fontes, contexto consolidado e opção de impressão.
* **`/403`** — Tela amigável de Acesso Negado em tentativas de violação de perfis.

### 📚 Endpoints de Integração e API REST (OpenAPI / Swagger)
* **`/swagger`** ou **`/swagger-ui.html`** — Documentação interativa Swagger.
* **`GET /health-context/pets/{idPet}`** — Retorna o contexto consolidado do pet em JSON (IoT 24h, BCS, histórico e clima).
* **`POST /health-reports/pets/{idPet}/generate`** — Gera via IA e armazena um novo relatório de saúde no Oracle.
* **`GET /health-reports/pets/{idPet}`** — Lista metadados paginados dos relatórios salvos.
* **`GET /health-reports/pets/{idPet}/latest`** — Retorna o último relatório gerado para o pet.
* **`GET /health-reports/pets/{idPet}/{idGeracao}`** — Retorna relatório e snapshot do contexto pelo UUID da geração.
* **`GET /pet/exportar-json`** — Exporta o dataset relacional completo para JSON via `PKG_PETTRACK_CONSULTA.PRC_LISTAR_PETS_JSON`.
* **`GET /pet/relatorio-atividades`** — Executa relatório manual de subtotais via `PKG_PETTRACK_CONSULTA.PRC_RELATORIO_SUBTOTAIS_MANUAL`.

---

## 🚀 Como Executar o Projeto

### 📌 Pré-requisitos
* **Java Development Kit (JDK):** Versão 21 instalada e configurada (`JAVA_HOME`).
* **Python:** Versão 3.10+ (para executar o microserviço de IA e Relatórios).
* **Acesso à Internet:** Para conexão com o Oracle FIAP (`oracle.fiap.com.br:1521:ORCL`).

---

### 📥 1. Clonando o Repositório
```bash
git clone https://github.com/Challenge-PetTrack/JAVA-ADVANCED.git
cd JAVA-ADVANCED/pettrack
```

---

### ⚙️ 2. Configuração do Banco de Dados
As credenciais do Oracle Database da FIAP estão configuradas em `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:oracle:thin:@oracle.fiap.com.br:1521:ORCL
spring.datasource.username=rm563719
spring.datasource.password=111206
spring.datasource.driver-class-name=oracle.jdbc.OracleDriver

spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.locations=classpath:db/migration
```

---

### ▶️ 3. Inicialização da Aplicação

#### Opção A: Executando no macOS / Linux
```bash
# 1. Em um terminal, inicie a API Python de IA (se desejar geração de relatórios):
cd ../../DISRUPTIVE-ARCHITECTURES-IOT-IOB-GENERATIVE-IA/py
source .venv/bin/activate
uvicorn main:app --host 127.0.0.1 --port 8000

# 2. No diretório JAVA-ADVANCED/pettrack:
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
./mvnw spring-boot:run
```

#### Opção B: Executando no Windows PowerShell
```powershell
# 1. No repositório DISRUPTIVE-ARCHITECTURES-IOT-IOB-GENERATIVE-IA/py:
.\.venv\Scripts\python.exe -m uvicorn main:app --host 127.0.0.1 --port 8000

# 2. No diretório JAVA-ADVANCED/pettrack:
$env:JAVA_HOME = 'C:\Users\Sbrana\.jdks\ms-21.0.11'
powershell -ExecutionPolicy Bypass -File .\iniciar-health-report.ps1
```

A aplicação iniciará na porta **`8080`** (acesse `http://localhost:8080`).

---

## 🧪 Execução dos Testes Automatizados

A aplicação conta com suíte automatizada cobrindo contratos de DTOs, orquestração de serviços, segurança, controllers e integração com banco e IA.

### Executar a suíte de testes no macOS / Linux:
```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
./mvnw test "-Dtest=HealthReport*Test,HealthContext*Test,PetProcedureServiceTest"
```

### Executar a suíte de testes no Windows:
```powershell
$env:JAVA_HOME = 'C:\Users\Sbrana\.jdks\ms-21.0.11'
.\mvnw.cmd test "-Dtest=HealthReport*Test,HealthContext*Test,PetProcedureServiceTest"
```

**Resultado dos testes:** `105 passed, 0 failures, 0 errors` (BUILD SUCCESS).
