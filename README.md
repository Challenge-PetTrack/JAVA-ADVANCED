# 🐾 PetTrack — Sistema Operacional de Saúde Animal e Monitoramento Contínuo

## Integração IoT — contexto do Health Report

A prioridade 1 da sprint 4 de IoT adiciona `GET /health-context/pets/{idPet}`, com consolidação dos dados e controle de acesso por tutor/clínica. Consulte a [documentação do contexto](pettrack/docs/health-context.md).

A prioridade 2 adiciona `POST /health-reports/pets/{idPet}/generate`, integrando a geração ao serviço Python com autenticação e validação da resposta. Consulte a [documentação da geração e dos testes](pettrack/docs/health-report.md). Persistência e interface serão implementadas nas prioridades seguintes.

> **FIAP Challenge 2026 · 2º Ano ADS (Turma 2TDS Fevereiro)**  
> **Disciplina:** Java Advanced — **Sprint 4: Consolidação, procedures Oracle e integração com MongoDB**  
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
> *(Demonstração da autenticação, perfis de acesso, tela 403, migrações Flyway e os 2 fluxos de negócio completos)*

---

## 📋 Sobre o Projeto PetTrack

O **PetTrack** é um ecossistema completo de telemetria e gestão clínica de saúde animal. A plataforma integra dados em tempo real provenientes de coleiras inteligentes (IoT), histórico de vacinas e consultas, suporte a diagnóstico de escore corporal (BCS) e acompanhamento ativo de adesão medicamentosa.

Nesta **3ª Sprint**, a aplicação evoluiu de uma API puramente REST para um **sistema Web corporativo completo**, integrando camada de visualização com **Thymeleaf + Bootstrap 5**, versionamento de banco com **Flyway**, controle de acesso granular com **Spring Security** e **dois fluxos de negócio complexos** que extrapolam operações simples de CRUD.

---

## 🏆 Requisitos da Sprint 3 Atendidos

| Módulo / Requisito | Pontuação | Descrição da Implementação |
| :--- | :---: | :--- |
| 🎨 **Frontend Web (Thymeleaf)** | **30 pts** | Interface web responsiva desenvolvida com Thymeleaf e Bootstrap 5.3. Navegação completa e dinâmica (`fragments/layout.html`) com menus contextuais baseados no perfil do usuário (`sec:authorize`). Formulários com validação visual e feedback imediato de erros (`Bean Validation`). |
| 🗄️ **Versionamento de Banco (Flyway)** | **20 pts** | Migrações SQL versionadas em `src/main/resources/db/migration/`: `V1` (estrutura DDL das 13 tabelas), `V2` (tabela de segurança `TB_USUARIO`) e `V3` (carga inicial de dados e usuários). Estratégia de auto-repair configurada em `FlywayConfig`. |
| 🔐 **Spring Security & Perfis** | **30 pts** | Autenticação via banco com hash BCrypt, formulário de login customizado (`/login`), redirecionamento inteligente pós-login (`/dashboard`) e separação rígida de rotas entre Administradores/Veterinários (`/admin/**`) e Tutores (`/portal-tutor/**`). Tela amigável de Acesso Negado (`/403`). |
| ⚙️ **2 Fluxos de Negócio além do CRUD** | **20 pts** | **Fluxo 1 (IoT & Alertas Clínicos):** Ingestão de telemetria de sensores, detecção de febre/sedentarismo, emissão automática de alertas e recálculo dinâmico do Health Score.<br>**Fluxo 2 (Adesão Medicamentosa):** Acompanhamento diário de doses prescritas, cálculo em tempo real da taxa de adesão (%) e disparo de alerta clínico à equipe veterinária em caso de dose esquecida. |

---

## 🚀 Como Executar o Projeto

### 📌 Pré-requisitos
* **Java Development Kit (JDK):** Versão 21 instalada e configurada no `JAVA_HOME`.
* **Apache Maven:** Versão 3.9+ (ou utilizar o Maven Wrapper `./mvnw`).
* **Acesso à Rede FIAP:** Conexão com o banco Oracle (`oracle.fiap.com.br:1521:ORCL`).
* **IDE Recomendada:** IntelliJ IDEA, Eclipse ou VS Code.

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

# Configurações do Flyway
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.locations=classpath:db/migration
```

---

### ▶️ 3. Inicialização da Aplicação

#### Opção A: Executando pelo IntelliJ IDEA
1. Abra a pasta `pettrack` no IntelliJ IDEA.
2. Aguarde a indexação das dependências do Maven (`pom.xml`).
3. Localize e execute o método `main` na classe:
   `src/main/java/com/fiap/pettrack/PetTrackApplication.java`.

#### Opção B: Executando via Linha de Comando (Terminal)
```powershell
# Dentro do diretório pettrack; use o JDK 21 instalado.
$env:JAVA_HOME = 'C:\Users\Sbrana\.jdks\ms-21.0.11'
.\mvnw.cmd -DskipTests package
# Após BUILD SUCCESS:
& "$env:JAVA_HOME\bin\java.exe" -jar '.\target\PetTrack-0.0.1-SNAPSHOT.jar'
```

No Windows validado, o caractere `°` no caminho da pasta causa uma falha de
codificação no arquivo de argumentos usado por `spring-boot:run`, resultando em
`ClassNotFoundException` mesmo com a classe principal compilada. Executar o JAR
contorna essa falha sem alterar o código, a versão do Spring Boot ou a pasta.
`-DskipTests` apenas evita executar testes durante a geração do JAR; os comandos
para executar os testes continuam documentados na seção da Sprint 4.

A aplicação iniciará na porta **`8080`**. O Flyway executará as migrações automaticamente no banco de dados e o `DataInitializer` garantirá a disponibilidade dos usuários de teste.

---

## 🔑 Credenciais Pré-configuradas para Teste e Avaliação

| Perfil de Acesso | E-mail / Usuário | Senha | Permissões e Rota Pós-Login |
| :--- | :--- | :--- | :--- |
| 🛡️ **Administrador** | `admin@pettrack.com` | `admin123` | Acesso total: Painel Admin, Gestão e CRUD de Pets, Simulador IoT, Documentação Swagger. Destino: `/admin/dashboard`. |
| 🩺 **Veterinário** | `vet@pettrack.com` | `vet123` | Permissões clínicas: Visualização de pacientes, acompanhamento de alertas e simulador IoT. Destino: `/admin/dashboard`. |
| 🐶 **Tutor de Pet** | `ana.silva@icloud.com` | `tutor123` | Portal do Tutor: Carteirinha de saúde de seus pets, vacinas e registro diário de doses de medicamentos. Destino: `/portal-tutor/dashboard`. |

---

## 🗺️ Rotas e Telas da Aplicação

### 🌐 Camada Web (Thymeleaf)
* **`/login`** — Tela de login estilizada com validação de credenciais e mensagens informativas.
* **`/dashboard`** — Roteador inteligente: redireciona para a área adequada dependendo do papel do usuário logado.
* **`/admin/dashboard`** — Visão geral da clínica com KPIs em tempo real, últimos pacientes e alertas pendentes.
* **`/admin/pets`** — Listagem de pacientes cadastrados com ações de edição e exclusão.
* **`/admin/pets/novo`** — Formulário de cadastro de pet com validação de dados via Bean Validation.
* **`/admin/telemetria`** — **(Fluxo 1)** Simulador interativo da coleira inteligente IoT com presets de febre e sedentarismo.
* **`/portal-tutor/dashboard`** — Portal exclusivo do tutor com listagem dos seus pets e alertas de saúde.
* **`/portal-tutor/pet/{id}`** — Carteirinha digital do pet com vacinas, protocolos preventivos e histórico clínico.
* **`/portal-tutor/medicamentos`** — **(Fluxo 2)** Interface interativa para confirmação diária de administração de doses e cálculo de taxa de adesão.
* **`/403`** — Tela estilizada de Acesso Negado disparada em tentativas de violação de perfil.

### 📚 Documentação da API REST (OpenAPI / Swagger)
* **`/swagger`** ou **`/swagger-ui.html`** — Documentação viva interativa contendo todos os 12 controllers REST da aplicação.

---

## 🧪 Roteiro de Testes Recomendado (Para Demonstração e Avaliação)

### 1. Teste de Autenticação e Perfis (Spring Security)
1. Acesse `http://localhost:8080`.
2. Entre com o usuário Administrador (`admin@pettrack.com` / `admin123`).
3. Note que o menu superior renderiza opções exclusivas: *Dashboard*, *Gestão de Pets*, *Simulador IoT* e *Swagger*.
4. Clique em **Sair** e entre com a Tutora (`ana.silva@icloud.com` / `tutor123`).
5. Note que as opções de administração desaparecem e o menu passa a exibir *Meus Pets* e *Medicamentos & Adesão*.

### 2. Teste de Violação de Rota (Página 403)
1. Estando conectado como Tutora (`ana.silva@icloud.com`), tente forçar o acesso manual a:
   `http://localhost:8080/admin/dashboard`.
2. O Spring Security interceptará a requisição e exibirá a página **403 - Acesso Negado**, exibindo o e-mail logado e o motivo da restrição.

### 3. Teste do Fluxo 1: Telemetria IoT & Geração Automática de Alertas
1. Conecte-se como Admin ou Veterinário.
2. Acesse o menu **Simulador IoT** (`/admin/telemetria`).
3. Clique no botão de atalho **Cenário de Febre (40.2°C)** e clique em **Processar Fluxo IoT**.
4. **Comportamento observado:** O sistema grava a leitura do sensor em `TB_COLLAR_LEITURA`, identifica a temperatura acima de $39.5^\circ\text{C}$, insere um alerta crítico em `TB_ALERTA`, cria uma notificação de urgência em `TB_NOTIFICACAO` e recalcula imediatamente o Health Score do paciente.

### 4. Teste do Fluxo 2: Gestão de Adesão Medicamentosa
1. Conecte-se como Tutora (`ana.silva@icloud.com`).
2. Acesse o menu **Medicamentos & Adesão** (`/portal-tutor/medicamentos`).
3. Selecione um medicamento prescrito, marque a opção **Não / Esquecido**, digite a justificativa e clique em **Confirmar Registro**.
4. **Comportamento observado:** O percentual de adesão é recalculado na hora e, como a dose foi pulada, o sistema dispara automaticamente um alerta médico para acompanhamento da clínica veterinária.

### 5. Teste de Validação de Formulários (Bean Validation)
1. No menu de administração, vá em **Gestão de Pets** ➡️ **Cadastrar Novo Pet**.
2. Deixe campos obrigatórios em branco (ex: Nome) e clique em **Salvar**.
3. Observe que os campos são sinalizados em vermelho com mensagens amigáveis de validação sem travar a aplicação.

---

## 🏛️ Padrões de Projeto e Princípios de Arquitetura

* **SOLID:** Separação estrita de responsabilidades: Controllers apenas orquestram chamadas; regras de negócio e validações complexas residem nos Services (`service.fluxos`); o acesso aos dados é isolado em Repositories JPA.
* **DRY (Don't Repeat Yourself):** Reutilização de cabeçalhos, navegação, alertas globais e rodapés através de fragmentos modulares do Thymeleaf (`fragments/layout.html`).
* **Clean Code:** Nomes autoexplicativos em classes e métodos, tratamento centralizado de exceções e uso de DTOs e MapStruct para proteger as entidades de domínio.
* **Idempotência no Banco:** Migrações Flyway e rotinas de carga projetadas para execução segura e reproduzível em qualquer ambiente de banco de dados Oracle.


---

## Integração com Banco de Dados - Sprint 4

O cadastro de novos pets pela API `/pet/novo` e pelo formulário `/admin/pets/novo`
utiliza `PKG_PETTRACK_CADASTRO.PRC_INSERT_PET`, na mesma conexão Oracle existente.
A nova migração `V4__packages_pettrack.sql` instala os quatro packages e adapta
o trigger de auditoria; as migrations anteriores permanecem intactas.

ADMIN e VET têm acesso a `/pet/exportar-json` (download de dataset Oracle real)
e `/pet/relatorio-atividades` (relatório manual de subtotais). A lógica de JDBC
fica em `PetProcedureRepository`; a integração transacional, em
`PetProcedureService`. Nenhuma dependência nova foi acrescentada ao pom.xml.

Os scripts Oracle/MongoDB, o PDF dos modelos, o dataset, os comandos de validação
e o roteiro do vídeo estão na pasta irmã
`MASTERING-RELATIONAL-NON-RELATIONAL-DATABASE`, seção **Sprint 4** do README.
O schema deve conter a estrutura aprovada na Sprint 3, inclusive a auditoria.

Testes unitários: `./mvnw -Dtest=PetProcedureServiceTest test`.
Integração Oracle: definir `PETTRACK_ORACLE_TESTS=true` e executar
`./mvnw -Dtest=PetProcedureOracleIT test`.
Migração: `./mvnw -Dtest=PetTrackFlywayOracleIT test` com a mesma variável.
Os testes de integração reais são de execução explícita.
