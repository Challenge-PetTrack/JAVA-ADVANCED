# 🐾 PetTrack — Sistema Operacional de Saúde Animal e Monitoramento Contínuo

> **FIAP Challenge 2026 · 2º Ano ADS (Turma 2TDSPF · 2º Semestre)**  
> **Disciplinas Integradas:** DevOps Tools & Cloud Computing & Java Advanced — **Sprint 4: Esteiras Automatizadas de CI/CD, Nuvem Azure, Azure SQL e Governança de Deploy**  
> **Parceiro de Negócio:** Clyvo Vet  

---

## 👥 Integrantes do Grupo

| Nome Completo | RM | Turma | Função Principal no Projeto |
| :--- | :--- | :--- | :--- |
| **Gabriel Sbrana Campos** | RM 565849 | 2TDSPF | DevOps · Infraestrutura Azure · Pipelines CI/CD · Backend JPA |
| **Moisés Waidemann Molinillo Júnior** | RM 563719 | 2TDSPF | Backend · DTOs · Validações · Notificações |
| **Thiago Rodrigues da Mota** | RM 563650 | 2TDSPF | Backend · Controllers · Swagger · UI Thymeleaf |
| **Richard Freitas** | RM 566127 | 2TDSPF | Backend · Testes Automatizados · Integrações de Serviços |

---

## 🔗 Links Oficiais da Entrega (Sprint 4)

* 🌐 **Aplicação em Nuvem (Azure Web App):**  
  > `https://<seu-app>.azurewebsites.net` *(Substitua pelo nome do seu Web App no Azure Portal)*
* 🚀 **Projeto no Azure DevOps:**  
  > `https://dev.azure.com/<sua-organizacao>/Sprint%204%20%E2%80%93%20Azure%20DevOps`
* 💻 **Repositório GitHub do Projeto:**  
  > [https://github.com/Challenge-PetTrack/JAVA-ADVANCED](https://github.com/Challenge-PetTrack/JAVA-ADVANCED) (Branch: `master`)
* 🎥 **Vídeo Demonstrativo de CI/CD e CRUD (YouTube):**  
  > `https://youtu.be/<seu-link-do-video>` *(Link não listado ou público)*

---

## 📋 1. Descrição da Solução e Stack de Tecnologias

O **PetTrack** surge em parceria com a **Clyvo Vet** como o **sistema operacional do cuidado contínuo**, transformando a relação tutor-clínica-pet através de:
1. **Telemetria Contínua (IoT):** Ingestão em tempo real de temperatura corporal e nível de atividade física.
2. **Health Score Dinâmico (0 a 100):** Cálculo contínuo baseado em sinais vitais, histórico clínico, vacinas e adesão medicamentosa.
3. **Adesão Medicamentosa Ativa:** Controle diário de posologia com cálculo dinâmico de taxa de adesão (%) e alertas de esquecimento.
4. **Health Reports Estruturados com IA Generativa:** Consolidação automática de prontuário e análises preditivas.

### 🛠️ Stack de Tecnologias
* **Backend & Linguagem:** Java 17 / 21, Spring Boot 3.3 (Spring Data JPA, Spring Web, Spring Security, MapStruct).
* **Banco de Dados em Nuvem (PaaS):** Azure SQL Database (`pettrackdb` em `pettrack-db.database.windows.net`) com pools de conexão HikariCP e migrações Flyway.
* **Hospedagem & Computação em Nuvem:** Azure App Service (Linux Web App — Plano B1).
* **Controle de Versão:** Git & GitHub (Branch principal monitorada: `master`).
* **Automação de CI/CD:** Azure DevOps Pipelines (Editor Clássico com agentes `ubuntu-latest`).
* **Testes Automatizados:** JUnit 5 / Spring Boot Starter Test com publicação de relatórios XML (`surefire-reports`).

---

## 🏛️ 2. Diagrama da Arquitetura em Nuvem & Fluxo de CI/CD

O fluxo de entrega contínua do PetTrack integra o desenvolvimento local à nuvem Microsoft Azure através de esteiras automatizadas de integração contínua (CI) e deploy contínuo (CD):

```mermaid
graph TD
    subgraph Personas ["👤 Personas do Sistema"]
        Dev["👨‍💻 Desenvolvedor / DevOps"]
        User["🐾 Usuário Final (Tutor / Veterinário)"]
    end

    subgraph Versionamento ["🐙 Controle de Versão"]
        GH["GitHub Repositories<br/><code>Challenge-PetTrack/JAVA-ADVANCED</code><br/>(Branch: master)"]
    end

    subgraph Azure_DevOps ["⚙️ Azure DevOps Services (Sprint 4 – Azure DevOps)"]
        CI["Esteira de CI (Build Pipeline)<br/>1. Maven clean package<br/>2. Testes JUnit Automatizados<br/>3. Copy .jar to Staging"]
        Art[("📦 Azure Artifacts<br/>drop/pettrack.jar")]
        CD["Esteira de CD (Release Pipeline)<br/>Gatilho Contínuo (Trigger)<br/>Injeção de Variáveis Secretas (🔒)"]
    end

    subgraph Microsoft_Azure ["☁️ Microsoft Azure Cloud Infrastructure"]
        WebApp["🌐 Azure Web App (App Service - Linux)<br/>Runtime Java 17 / 21 SE<br/>https://&lt;seu-app&gt;.azurewebsites.net"]
        SQL[("🗄️ Azure SQL Database (PaaS)<br/>pettrack-db.database.windows.net:1433<br/>Database: pettrackdb")]
    end

    Dev -->|"1. Commit & Push de código"| GH
    GH -->|"2. Webhook dispara CI na branch master"| CI
    CI -->|"3. Compilação e 100% dos testes aprovados"| CI
    CI -->|"4. Publica pacote executável .jar"| Art
    Art -->|"5. Gatilho automático dispara o Release"| CD
    CD -->|"6. Deploy automatizado com credenciais mascaradas"| WebApp
    WebApp -->|"7. Conexão JDBC criptografada (Porta 1433)"| SQL
    User -->|"8. Acesso ao Sistema e Execução do CRUD"| WebApp