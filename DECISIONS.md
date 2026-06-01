# DECISIONS.md — gubee-stock-reconciliation

## Interpretação do problema

O serviço precisa lidar com eventos que chegam de fontes diferentes — seller, marketplace e Gubee
— e que podem vir duplicados, fora de ordem ou ao mesmo tempo. O foco foi manter o saldo de
estoque correto por conta/SKU e guardar o histórico de tudo que aconteceu.

## Fonte da verdade

Decidi que o `STOCK_ADJUSTED` é a fonte de verdade do estoque. Quando ele chega sobrescreve
o saldo atual com o valor do campo `available`, sem considerar o que estava antes. As vendas,
cancelamentos e recomposições movimentam esse saldo de forma relativa. Faz sentido porque na
prática é o seller quem define o estoque correto — o marketplace só movimenta com base nisso.

## Idempotência

Todo evento tem um `eventId`. Na chegada e eu busco esse id na tabela `stock_events`. Se já existe
com status diferente de `PENDING`, retorno `IGNORED` e não aplico nada. Eventos `PENDING` entram
no reprocessamento porque ainda não produziram efeito no estoque.

## Duplicidade lógica

Além do `eventId`, eu verifico duplicidade de negócio em dois casos:
- `ORDER_CREATED`: verifico se já existe um evento `PROCESSED` com o mesmo
  `accountId + sku + externalOrderId`. Se sim, marco como `INCONSISTENT`.
- `ORDER_CANCELLED` e `MARKETPLACE_STOCK_RESTORED`: verifico se já tem uma recomposição
  `PROCESSED` para o mesmo pedido. Se sim, bloqueio pra não recompor duas vezes.

Isso protege o estoque mesmo quando sistemas diferentes reenviam o mesmo evento com
`eventId` diferente.

## Eventos fora de ordem

Se caso um `ORDER_CANCELLED` ou `MARKETPLACE_STOCK_RESTORED` chega antes do `ORDER_CREATED`,
salvo o evento como `PENDING` com uma mensagem explicando o motivo. Um scheduler roda a cada
30 segundos tentando reprocessar tudo que está `PENDING`. Quando o `ORDER_CREATED` chega e
é processado, no próximo ciclo o cancelamento é aplicado normalmente.

O lado ruim disso é que o reprocessamento pode demorar até 30 segundos. Em produção esse
intervalo seria configurável, ou o reprocessamento seria disparado logo após cada evento
processado com sucesso.

## Concorrência

Uso `PESSIMISTIC_WRITE` lock no banco (`SELECT ... FOR UPDATE`) na hora de ler o saldo.
Com isso, dois eventos simultâneos pro mesmo `accountId/sku` ficam serializados pelo PostgreSQL
— um espera o outro terminar. Não precisa de lock distribuído, o banco já resolve. Se o saldo
for insuficiente pra venda, marco como `INCONSISTENT` e não altero nada.

## Múltiplas instâncias

O lock pessimista funciona com várias instâncias rodando ao mesmo tempo porque quem controla
o lock é o PostgreSQL, não a JVM. Duas instâncias tentando mexer no mesmo `accountId/sku`
vão ser serializadas pelo banco de qualquer forma. A idempotência por `eventId` também é
garantida via banco, então retry não causa processamento duplo.

## Chave de estoque

Controlei o estoque por `accountId + sku`, não por marketplace. O raciocínio foi que o seller
tem um estoque único por SKU que a Gubee distribui entre os marketplaces. Separar por marketplace
criaria fragmentação desnecessária e complicaria a reconciliação quando o mesmo produto vende
em canais diferentes.

## Multi-account

A chave composta `(accountId, sku)` garante que contas diferentes não se interferem.
`account-001/ABC-123` e `account-002/ABC-123` são estoques completamente separados.

## Trade-offs assumidos

- **Sem Kafka**: Não utilizei Kafka pois ele faz sentido para algo mais escalado e em produção, com múltiplos
  produtores e consumidores. Como o desafio exigia apenas um serviço, não faria sentido
  implementá-lo — estaria só sobrecarregando a aplicação. O comportamento assíncrono já está
  coberto pelo scheduler: eventos `PENDING` ficam persistidos e são reprocessados
  automaticamente, que é exatamente o papel de uma fila.
- **Sem Testcontainers**: os testes rodam no mesmo PostgreSQL do Docker Compose. Em produção
  usaria Testcontainers pra ter testes isolados e reproduzíveis em CI.
- **Scheduler simples**: usei `@Scheduled` com intervalo fixo de 30 segundos pro reprocessamento
  de eventos `PENDING`. Funciona bem pro desafio, mas em produção trocaria por um retry com
  backoff exponencial e uma dead-letter pra não perder eventos que ficam travados.
- **Swagger/OpenAPI**: adicionado via springdoc-openapi. Disponível em
  `http://localhost:8080/swagger-ui/index.html` com a aplicação rodando.

## O que simplifiquei por causa do prazo

- Testcontainers: os testes dependem do PostgreSQL do Docker Compose rodando localmente.
- Scheduler com intervalo fixo de 30 segundos em vez de retry com backoff exponencial.
- Sem autenticação nos endpoints.
- Sem dead-letter para eventos que travam no reprocessamento.
- Swagger sem customização de descrições nos endpoints.

## O que faria diferente em produção

- Kafka pra garantir durabilidade, replay e desacoplar os eventos.
- Testcontainers nos testes pra não depender de infraestrutura externa rodando.
- Dead-letter queue pros eventos que travam no reprocessamento e nunca saem do `PENDING`.
- Métricas com Micrometer + Prometheus pra acompanhar a taxa de `INCONSISTENT` e `PENDING`.
- Retry com backoff exponencial no scheduler.
- Autenticação nos endpoints com JWT ou API Key.