# 🐾 PetTrack — Sistema Operacional de Saúde Animal e Monitoramento Contínuo

> **FIAP Challenge 2026 · 2º Ano ADS (Turma 2TDS Fevereiro)**  
> **Disciplina:** Java Advanced — **Sprint 4: Consolidação, procedures Oracle, Relatórios de Saúde e integração com MongoDB**  
> **Parceiro de Negócio:** Clyvo Vet  

---

## 👥 Integrantes do Grupo

| Nome Completo | RM |
| :--- | :--- |
| **Gabriel Sbrana Campos** | RM 565849 |
| **Moisés Waidemann** | RM 563719 |
| **Thiago Rodrigues da Mota** | RM 563650 |
| **Richard Freitas** | RM 566127 |

---

## 📹 Vídeo de Demonstração da Aplicação
> 🎥 **Link do Vídeo (YouTube / Loom - Máx. 10 minutos):**  
> `[https://youtu.be/1QZ9eECIdqk]`  
> *(Demonstração da autenticação, perfis de acesso, tela 403, migrações Flyway e os fluxos de negócio completos)*

---

## 📋 Sobre o Projeto PetTrack

O **PetTrack** é um ecossistema completo de telemetria e gestão clínica de saúde animal. A plataforma integra dados em tempo real provenientes de coleiras inteligentes (IoT), histórico de vacinas e consultas, suporte a diagnóstico de escore corporal (BCS), geração e histórico de relatórios de saúde consolidados e acompanhamento ativo de adesão medicamentosa.

O sistema Web corporativo integra camada de visualização com **Thymeleaf + Bootstrap 5**, versionamento de banco com **Flyway**, controle de acesso granular com **Spring Security**, integração com procedures Oracle (`PKG_PETTRACK_*`) e geração de **Health Reports** estruturados.

---

## 🏆 Funcionalidades Principais

| Módulo / Requisito | Descrição da Implementação |
| :--- | :--- |
| 🎨 **Frontend Web (Thymeleaf)** | Interface web responsiva desenvolvida com Thymeleaf e Bootstrap 5.3. Navegação dinâmica (`fragments/layout.html`) com menus contextuais baseados no perfil do usuário (`sec:authorize`). Formulários com validação visual e feedback imediato de erros (`Bean Validation`). |
| 🗄️ **Versionamento de Banco (Flyway)** | Migrações SQL versionadas em `src/main/resources/db/migration/`: `V1` (estrutura DDL das 13 tabelas), `V2` (tabela de segurança `TB_USUARIO`), `V3` (carga inicial de dados e usuários), `V4` (packages e procedures Oracle) e `V5` (tabela de relatórios de saúde `TB_HEALTH_REPORT`). |
| 🔐 **Spring Security & Perfis** | Autenticação via banco com hash BCrypt, formulário de login customizado (`/login`), redirecionamento pós-login (`/dashboard`) e separação de rotas entre Administradores/Veterinários (`/admin/**`), Tutores (`/portal-tutor/**`) e Relatórios de Saúde (`/relatorios/pets/**`). Tela de Acesso Negado (`/403`). |
| ⚙️ **Fluxos de Negócio e Telemetria** | **Fluxo 1 (IoT & Alertas Clínicos):** Ingestão de telemetria de sensores, detecção de febre/sedentarismo, emissão automática de alertas e recálculo dinâmico do Health Score.<br>**Fluxo 2 (Adesão Medicamentosa):** Acompanhamento diário de doses prescritas, cálculo em tempo real da taxa de adesão (%) e disparo de alerta clínico em caso de dose esquecida.<br>**Fluxo 3 (Health Report Consolidado):** Consolidação de telemetria IoT (24h), evolução BCS, histórico clínico, medicamentos, vacinas e clima para geração e armazenamento de relatórios estruturados por pet. |

---

## 🚀 Como Executar o Projeto

### 📌 Pré-requisitos
* **Java Development Kit (JDK):** Versão 21 instalada e configurada no `JAVA_HOME`.
* **Apache Maven:** Versão 3.9+ (ou utilizar o Maven Wrapper `./mvnw`).
* **Acesso ao Oracle FIAP:** Conexão com o banco Oracle (`oracle.fiap.com.br:1521:ORCL`).

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

#### Opção A: Executando com Integração Health Report (Windows PowerShell)
1. No repositório `DISRUPTIVE-ARCHITECTURES-IOT-IOB-GENERATIVE-IA/py`, inicie a API Python na porta `8000`:
   ```powershell
   .\.venv\Scripts\python.exe -m uvicorn main:app --host 127.0.0.1 --port 8000
   ```
2. Na pasta `JAVA-ADVANCED/pettrack`, execute:
   ```powershell
   powershell -ExecutionPolicy Bypass -File .\iniciar-health-report.ps1
   ```

#### Opção B: Executando via Maven Wrapper / IDE
```powershell
$env:JAVA_HOME = 'C:\Users\Sbrana\.jdks\ms-21.0.11'
.\mvnw.cmd spring-boot:run
```

A aplicação iniciará na porta **`8080`**.

---

## 🔑 Credenciais Pré-configuradas para Teste e Avaliação

| Perfil de Acesso | E-mail / Usuário | Senha | Permissões e Rota Pós-Login |
| :--- | :--- | :--- | :--- |
| 🛡️ **Administrador** | `admin@pettrack.com` | `admin123` | Acesso total: Painel Admin, Gestão e CRUD de Pets, Relatórios de Saúde, Simulador IoT, Documentação Swagger. Destino: `/admin/dashboard`. |
| 🩺 **Veterinário** | `vet@pettrack.com` | `vet123` | Permissões clínicas: Visualização de pacientes, relatórios de saúde, acompanhamento de alertas e simulador IoT. Destino: `/admin/dashboard`. |
| 🐶 **Tutor de Pet** | `ana.silva@icloud.com` | `tutor123` | Portal do Tutor: Carteirinha de saúde de seus pets, relatórios de saúde, vacinas e registro diário de doses de medicamentos. Destino: `/portal-tutor/dashboard`. |

---

## 🗺️ Rotas e Telas da Aplicação

### 🌐 Camada Web (Thymeleaf)
* **`/login`** — Tela de login com validação de credenciais.
* **`/dashboard`** — Roteador que redireciona para a área adequada de acordo com o perfil do usuário logado.
* **`/admin/dashboard`** — Visão geral da clínica com KPIs em tempo real, últimos pacientes e alertas pendentes.
* **`/admin/pets`** — Listagem de pacientes cadastrados com acesso aos relatórios de saúde, edição e exclusão.
* **`/admin/pets/novo`** — Formulário de cadastro de pet integrado à procedure `PKG_PETTRACK_CADASTRO.PRC_INSERT_PET`.
* **`/admin/telemetria`** — Simulador interativo da coleira inteligente IoT com presets de febre e sedentarismo.
* **`/portal-tutor/dashboard`** — Portal exclusivo do tutor com listagem dos seus pets, alertas e acesso aos relatórios de saúde.
* **`/portal-tutor/pet/{id}`** — Carteirinha digital do pet com vacinas, protocolos preventivos e histórico clínico.
* **`/portal-tutor/medicamentos`** — Interface para confirmação diária de administração de doses e cálculo de taxa de adesão.
* **`/relatorios/pets/{idPet}`** — Histórico paginado e geração de novos relatórios de saúde do pet.
* **`/relatorios/pets/{idPet}/{idGeracao}`** — Visualização detalhada do relatório de saúde, fontes utilizadas, contexto consolidado e impressão.
* **`/403`** — Tela de Acesso Negado disparada em tentativas de violação de perfil.

### 📚 Endpoints de Integração e API REST
* **`/swagger`** ou **`/swagger-ui.html`** — Documentação interativa OpenAPI/Swagger.
* **`GET /health-context/pets/{idPet}`** — Retorna o contexto consolidado do pet (IoT 24h, BCS, histórico clínico e clima).
* **`POST /health-reports/pets/{idPet}/generate`** — Gera e armazena um novo relatório de saúde no Oracle.
* **`GET /health-reports/pets/{idPet}`** — Lista metadados paginados dos relatórios de saúde do pet.
* **`GET /health-reports/pets/{idPet}/latest`** — Retorna o último relatório salvo para o pet.
* **`GET /health-reports/pets/{idPet}/{idGeracao}`** — Retorna o relatório e o snapshot do contexto por UUID.
* **`GET /pet/exportar-json`** — Exporta o dataset JSON via `PKG_PETTRACK_CONSULTA.PRC_LISTAR_PETS_JSON`.
* **`GET /pet/relatorio-atividades`** — Gera o relatório de subtotais via `PKG_PETTRACK_CONSULTA.PRC_RELATORIO_SUBTOTAIS_MANUAL`.

---

## 🧪 Execução dos Testes Automatizados

Na pasta `JAVA-ADVANCED/pettrack` com JDK 21:

```powershell
$env:JAVA_HOME = 'C:\Users\Sbrana\.jdks\ms-21.0.11'
.\mvnw.cmd '-Dtest=HealthReport*Test,HealthContext*Test,PetProcedureServiceTest' test
```
