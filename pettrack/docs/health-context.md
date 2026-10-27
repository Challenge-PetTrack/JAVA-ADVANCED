# Contexto do Health Report - sprint 4, prioridade 1

O Java agora consolida os dados existentes de um pet em um contrato JSON para o futuro gerador de Health Report. Esta etapa consulta dados; nao chama um LLM, nao grava relatorios e nao cria tabelas.

## Consulta

`GET /health-context/pets/{idPet}`

Use a sessao de login ja existente no portal Java. Depois de entrar em `/login`, abra `/health-context/pets/1`, substituindo 1 pelo ID do pet. A operacao tambem aparece no Swagger em `/swagger`.

- Tutor: somente pets vinculados ao seu tutor.
- Veterinario: somente pets da clinica vinculada ao usuario.
- Administrador: acesso aos pets cadastrados.
- Visitante: redirecionamento para `/login`, conforme a seguranca existente.
- ID zero/negativo: 400; pet inexistente: 404; pet sem autorizacao: 403.
- Usuario inexistente ou inativo: 401.

O contrato `HealthContextDTO` usa records, sem serializar entidades JPA, senha, endereco ou contato do tutor.

## Mapeamento para o banco existente

| Bloco | Fonte real | Regra |
|---|---|---|
| `pet` | `TB_PET` | Nome, especie, raca, idade ja cadastrada e peso. Nao estima nascimento/idade. |
| `iotRecente` | `TB_COLLAR_LEITURA` | Ultimas 24 horas: total, amostras validas, medias, maxima de temperatura e ultima leitura. |
| `iotRecente.alertas` | `TB_ALERTA` | Mesmo pet e janela de 24 horas, incluindo status de resolucao. |
| `bcs` | `TB_BCS_HISTORICO` | Duas ultimas avaliacoes inteiras de 1 a 9, anteriores ao instante da consulta. |
| `historico.eventosRecentes` | `TB_EVENTO_CLINICO` | Eventos desde o inicio do dia de 90 dias atras ate o instante da consulta. |
| `historico.ultimaConsulta` | `TB_EVENTO_CLINICO` | Ultimo evento do tipo CONSULTA, mesmo se anterior aos 90 dias. |
| `historico.medicamentosAtivos` | `TB_MEDICAMENTO` + `TB_EVENTO_CLINICO` | Vinculados ao pet, iniciados ate hoje e sem fim ou com fim hoje/futuro. Exclui intervalos invertidos e eventos futuros. |
| `historico.vacinasVencidasOuVencendo` | `TB_PROTOCOLO_PREVENTIVO` | Tipo VACINA com proxima aplicacao vencida, hoje ou nos proximos 30 dias. |
| `clima` | OpenWeatherMap | Consulta opcional, em Celsius, para a cidade configurada explicitamente. |

As tabelas `tb_consulta`, `tb_vacina` e `tb_alerta_collar` do documento conceitual foram mapeadas para as estruturas acima. Nao foi criado um esquema paralelo.

Vacinas usam `DT_PROXIMA`, inclusive quando o registro da ultima aplicacao esta como REALIZADO: esse status descreve o registro existente, nao comprova que a proxima dose foi aplicada. Itens sem proxima data nao entram na lista. O contexto alerta que lista vazia nao significa vacinacao em dia ou ausencia de tratamento.

## Periodos, valores ausentes e tendencias

- `periodoIot` informa inicio inclusivo e fim exclusivo. Leituras e alertas futuros nao entram.
- JDBC preserva a hora das colunas DATE Oracle. As entidades atuais mapeadas como LocalDate nao foram alteradas.
- O fuso padrao e `America/Sao_Paulo`, configuravel por `pettrack.health-context.zone`. Deve corresponder ao fuso em que o Oracle grava seus DATE/SYSDATE, pois essas colunas nao carregam offset. Validar esse alinhamento na implantacao.
- Media de temperatura aceita 30 a 45 graus, conforme o contrato atual de `CollarLeitura`; atividade aceita valores nao negativos. Esses limites sao validacao de dados, nao uma nova regra clinica.
- Zero de atividade e um valor valido. Ausencia de amostras resulta em `null`, nunca em zero fabricado.
- Contagens separadas mostram quantas amostras entraram em cada media. A media e aritmetica por leitura, nao ponderada pelo tempo.
- BCS e ordenado por data/hora e, no empate, por ID decrescente. O desempate torna a selecao deterministica.
- A tendencia compara apenas os dois ultimos scores validos: `SUBINDO`, `DESCENDO`, `ESTAVEL` ou `SEM_HISTORICO_SUFICIENTE`. Nao representa uma regressao estatistica nem uma interpretacao clinica.
- Perfil incompleto, falta de IoT/BCS e clima indisponivel aparecem em `avisos`. Dados ausentes nao sao inventados.
- Falhas do banco continuam sendo erros de requisicao; nao sao convertidas em um contexto vazio que aparenta sucesso.

## Configuracao opcional de clima

Defina no ambiente do processo Java, sem incluir valores reais no Git:

```text
OPENWEATHER_API_KEY=<sua chave>
OPENWEATHER_CITY=Sao Paulo,BR
```

Tambem sao aceitas as propriedades `pettrack.health-context.weather.api-key` e `pettrack.health-context.weather.city`, que prevalecem sobre essas variaveis.

Sem chave ou cidade, `clima.status` retorna `NAO_CONFIGURADO`. Outras situacoes sao `DISPONIVEL`, `FALHA_PROVEDOR`, `RESPOSTA_INVALIDA` e `DADO_DESATUALIZADO`. O timeout e de 2 segundos na conexao e 3 segundos na leitura. Uma falha climatica nao impede a consolidacao dos dados internos.

Observacoes climaticas com mais de 3 horas ou mais de 5 minutos no futuro sao rejeitadas. Localidade, fonte e horario da observacao acompanham o resultado. A cidade e uma configuracao do ambiente; nao ha geolocalizacao automatica do pet. Nao reutilizamos a chave exposta no fluxo antigo do Node-RED.

## Exemplo parcial do contrato

Valores abaixo sao ilustrativos, nao extraidos de pacientes reais:

```json
{
  "versao": "1.0",
  "geradoEm": "2026-10-26T12:00:00-03:00",
  "fusoHorario": "America/Sao_Paulo",
  "pet": {"id": 1, "nome": "Rex", "especie": "Cao", "raca": null, "idadeAnos": 4.0, "pesoKg": 28.5},
  "periodoIot": {"inicioInclusivo": "2026-10-25T12:00:00", "fimExclusivo": "2026-10-26T12:00:00"},
  "iotRecente": {
    "totalLeituras": 2, "temperaturasValidas": 2, "atividadesValidas": 2,
    "temperaturaMedia": 39.0, "temperaturaMaxima": 40.0, "atividadeMedia": 10.0,
    "ultimaLeitura": "2026-10-26T11:00:00", "alertas": []
  }
}
```

A resposta completa inclui tambem `bcs`, `historico`, `clima` e `avisos`, conforme o record Java.

## Testes

Com JDK 21, na pasta `pettrack`:

```powershell
.\mvnw.cmd '-Dtest=HealthContext*Test,PetProcedureServiceTest' test
```

Essa selecao executa testes HTTP com a seguranca real, testes de servico, consultas em H2 no modo Oracle usando o DDL V1 real e o cliente climatico com respostas HTTP simuladas. Nao depende de chave de IA/clima e nao altera o banco Oracle. H2 e dependencia exclusiva de testes; o banco da aplicacao permanece Oracle.

O Surefire carrega o agente Byte Buddy no inicio da JVM de testes, usando a versao gerenciada pelo Spring Boot e o repositorio Maven local. Isso evita o attach dinamico bloqueado em ambientes restritos com Java 21.

Verificacao opcional de consultas no Oracle configurado:

```powershell
$env:PETTRACK_ORACLE_CONTEXT_TESTS = 'true'
.\mvnw.cmd '-Dtest=HealthContextOracleIT' test
Remove-Item Env:PETTRACK_ORACLE_CONTEXT_TESTS
```

Esse teste usa uma unica conexao, `SET TRANSACTION READ ONLY` e rollback final. Nao inicia a aplicacao, Flyway ou DataInitializer; nao executa seeds nem DDL. Exige ao menos um pet cadastrado e credenciais validas. Nao valida os cenarios extremos, cobertos pelos dados isolados dos demais testes.

## Limites desta etapa

Os nomes e anotacoes clinicas sao dados do sistema, nao instrucoes para o futuro LLM. A prioridade 2 devera delimitar esses dados no prompt e validar a resposta gerada.

Esta etapa entrega o contexto e seu endpoint de consulta. Geracao de texto com IA, persistencia do Health Report e tela do relatorio pertencem, respectivamente, as prioridades 2, 3 e 4 e dependem de autorizacao para prosseguir.
