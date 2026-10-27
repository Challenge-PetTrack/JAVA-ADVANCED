# Health Report - sprint 4, prioridade 2

A geracao usa o contexto consolidado da prioridade 1. O fluxo e:

`Usuario autenticado -> Java (autorizacao + contexto) -> Python -> OpenAI -> Python (validacao) -> Java (validacao) -> JSON`

Esta etapa retorna um relatorio transitorio. `persistido` e sempre `false`; nao cria tabelas, registros de relatorio ou telas. Persistencia e interface ficam nas prioridades 3 e 4.

## Endpoints Java

| Operacao | Endpoint | Protecao |
|---|---|---|
| Obter token CSRF da sessao | `GET /health-reports/csrf` | Sessao autenticada |
| Gerar relatorio | `POST /health-reports/pets/{idPet}/generate` | Sessao, CSRF e vinculo com o pet |

A geracao nao recebe o contexto do navegador. O servidor consulta os dados atuais pelo ID informado na URL e reaproveita as verificacoes de acesso de `HealthContextService`. Um JSON enviado pelo cliente nao substitui esses dados.

Tutor acessa seus pets; veterinario acessa os pets da clinica vinculada; administrador possui acesso geral. Falhas de autorizacao acontecem antes da chamada ao Python.

Exemplo no console do navegador, apos login no portal Java e na mesma origem, substituindo o ID por um pet autorizado:

```javascript
const csrf = await fetch('/health-reports/csrf').then(r => r.json());
const resposta = await fetch('/health-reports/pets/1/generate', {
  method: 'POST',
  headers: { [csrf.headerName]: csrf.token }
});
console.log(resposta.status, await resposta.json());
```

O endpoint gera texto em uma requisicao HTTP com timeout; nao e uma fila de jobs. A chamada acontece fora do caminho MQTT de alertas em tempo real. Agendamento semanal e processamento em background nao foram incluidos nesta prioridade.

## Configuracao

Java, por variaveis de ambiente do processo:

```text
HEALTH_REPORT_API_URL=http://127.0.0.1:8000
HEALTH_REPORT_SERVICE_TOKEN=<segredo aleatorio de pelo menos 32 caracteres>
```

Alternativas em propriedades: `pettrack.health-report.api-url` e `pettrack.health-report.service-token`.

Python, usando o mesmo segredo (arquivo `.env` na pasta `py` ou ambiente do processo):

```text
HEALTH_REPORT_SERVICE_TOKEN=<mesmo segredo do Java>
HEALTH_REPORT_MODE=openai
HEALTH_REPORT_MODEL=gpt-4o
OPENAI_API_KEY=<chave configurada localmente>
```

O Java nao carrega automaticamente o arquivo `.env` do Python. Configure o segredo nos dois processos. Nao use o token ficticio dos testes em uma implantacao.

O cliente Java exige HTTPS para hosts remotos; HTTP e aceito somente em loopback. Redirecionamentos do servico de IA sao rejeitados. A chave OpenAI permanece apenas no Python. Nao ha CORS adicional porque o navegador acessa somente o Java.

O exemplo de configuracao Python e versionado como `.env.example`; `.env`, ambientes virtuais e logs estao ignorados no Git.

## Executar o Python

Na pasta da materia de IoT, entre em `py` e use Python 3.12:

```powershell
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements-dev.lock.txt
.\.venv\Scripts\python.exe -m uvicorn main:app --host 127.0.0.1 --port 8000
```

O lock registra as versoes usadas nos testes, incluindo ferramentas de desenvolvimento. Para instalar apenas as dependencias da aplicacao, use `requirements.txt` (intervalos de versao). O contrato exige Pydantic 2 e OpenAI SDK 2.

O Java continua usando seu procedimento normal de inicializacao, com as variaveis acima configuradas. Os testes desta etapa nao iniciam Flyway nem o inicializador de dados.

## Contrato da resposta

| Campo | Significado |
|---|---|
| `idGeracao` | UUID da resposta, nao um ID de registro salvo |
| `idPet` | Pet autorizado que originou o contexto |
| `geradoEm` | Instante da geracao |
| `contextoGeradoEm` | Instante do contexto usado, validado novamente pelo Java |
| `provedor`, `modelo`, `versaoPrompt` | Origem e versoes da geracao |
| `simulado` | Distingue explicitamente o modo de demonstracao |
| `persistido` | Sempre false nesta etapa |
| `relatorio.resumo` | Sintese em linguagem natural |
| `relatorio.nivelAtencao` | BAIXO, MODERADO, ALTO ou INDETERMINADO; sugestao de revisao, sem validacao clinica |
| `relatorio.motivos` | Descricoes e blocos de dados utilizados como fontes |
| `relatorio.recomendacoes` | Orientacoes de acompanhamento e discussao com o veterinario |
| `relatorio.limitacoes` | Limitacoes geradas, avisos preservados do contexto e ressalva fixa |

O Python valida a entrada equivalente a `HealthContextDTO`, incluindo intervalo temporal, contagens e coerencia da tendencia BCS. Saidas da IA usam esquema Pydantic com Structured Outputs. O Java valida o corpo, campos obrigatorios, limites, identidade do pet, instante do contexto e coerencia entre `simulado` e `provedor`.

Sem IoT/BCS/historico relevante, a aplicacao fixa `INDETERMINADO`. Clima indisponivel nao pode ser citado como fonte. Os nomes, diagnosticos e observacoes sao enviados como dados em mensagem separada das instrucoes. O prompt proibe inventar dados, diagnosticar, prescrever ou alterar doses/dieta. Essas medidas reduzem riscos, mas nao demonstram precisao clinica nem eliminam alucinacoes. Nao foi implementado o painel de priorizacao da clinica nesta etapa.

## Provedor e divergencia da proposta

O documento da sprint 3 descrevia Claude; o codigo existente de BCS ja utilizava OpenAI. Esta implementacao preserva OpenAI como provedor para reutilizar a configuracao e o SDK existentes. O modelo padrao continua `gpt-4o`, configuravel por ambiente, sem afirmar disponibilidade para uma conta especifica.

O Health Report usa `client.responses.parse`, com `store=False`, resposta estruturada e sem ferramentas de execucao. Nao registra o contexto nem a resposta em logs da aplicacao. `store=False` nao e uma promessa de retencao zero pelo provedor.

Referencia tecnica consultada em 27/10/2026: [OpenAI - Structured Outputs](https://developers.openai.com/api/docs/guides/structured-outputs?api-mode=responses).

## Falhas e limites

- Sem segredo de integracao valido: 503; token Python incorreto: 401 (Java trata falha de integracao como 502).
- Sem chave OpenAI ou modo invalido: 503. Nao ha fallback automatico para mock.
- Limite/cota do provedor: 503; timeout: 504; resposta invalida, incompleta ou recusada: 502.
- API Python ocupada: 429. Ha ate 4 geracoes concorrentes por processo Python, sem fila.
- Contexto: no maximo 256 KiB, sem truncamento silencioso de historico. O Python limita inclusive corpos sem Content-Length.
- Timeout Python: 30 segundos por chamada ao provedor, sem retries automaticos. Cliente Java: conexao de 3 segundos e leitura de 40 segundos.
- Resposta aceita pelo Java: JSON de no maximo 256 KiB. Paginas de login, redirecionamentos e respostas para outro pet/contexto sao rejeitados.
- As mensagens de erro nao repassam o corpo do provedor, tokens ou dados clinicos. A rota BCS existente conserva seu contrato anterior, com cliente OpenAI criado apenas quando usado.

## Modo de demonstracao

Configure explicitamente `HEALTH_REPORT_MODE=mock` no Python. O mesmo fluxo retorna `provedor=mock`, `modelo=simulacao`, `simulado=true` e texto identificado como SIMULACAO. Nao ha consumo de API externa. O nivel de atencao e INDETERMINADO, sem avaliacao clinica.

## Testes reproduziveis

Python, na pasta `py`:

```powershell
.\.venv\Scripts\python.exe -m pytest -q
```

Java, na pasta `JAVA-ADVANCED/pettrack`, com JDK 21:

```powershell
.\mvnw.cmd '-Dtest=HealthReport*Test,HealthContext*Test,PetProcedureServiceTest' test
```

Integracao real Java -> Python com contexto ficticio, sem Oracle nem IA externa:

1. Em um terminal na pasta Python, configure `HEALTH_REPORT_MODE=mock` e `HEALTH_REPORT_SERVICE_TOKEN=token-de-integracao-ficticio-com-32-caracteres`.
2. Inicie `uvicorn main:app --host 127.0.0.1 --port 18080` pelo Python do ambiente virtual.
3. Em outro terminal na pasta Java:

```powershell
$env:PETTRACK_HEALTH_REPORT_SMOKE = 'true'
.\mvnw.cmd '-Dtest=HealthReportPythonIT' test
Remove-Item Env:PETTRACK_HEALTH_REPORT_SMOKE
```

O token ficticio e exclusivo desse teste em loopback. Encerre o servidor de teste ao finalizar. As fixtures equivalentes estao em `py/tests/fixtures/context.json` (IoT) e `src/test/resources/health-report/context.json` (Java).

Validacao desta etapa: 61 testes Java, incluindo o teste HTTP entre servicos, e 23 testes Python aprovados. A integracao com OpenAI foi testada com o SDK real e transporte HTTP simulado, incluindo recusas e falhas. Nao havia chave OpenAI configurada; uma geracao real com o provedor e a avaliacao clinica dos textos permanecem pendentes.
