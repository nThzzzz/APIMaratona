# Guia de Testes Automatizados: API Maratona

Este documento explica a coleção Postman/Newman que demonstra a API Maratona ponta a ponta contra um ambiente real. A coleção percorre o ciclo de vida completo — cadastro de usuários e times, consultas, edições e limpeza — e **todas as requisições devem responder `200`**.

> A suíte automatizada de Java (`./mvnw test`) é outra coisa e está descrita no [README](README.md#-testes). Esta coleção não substitui aquela: ela exercita a API de verdade, com bancos e integração com o Codeforces reais.

## 1. Pré-requisitos

Você precisa dos dois arquivos deste repositório:

* **`colecao.json`** — a coleção `TestesAPIMaratona`.
* **`ambiente.json`** — as variáveis `url`, `token_jwt` e `idProblema`. Só `url` precisa de atenção; as outras duas são preenchidas em tempo de execução. O padrão é `http://localhost:8080`.

Instale o executor e o gerador de relatório:

```bash
npm install -g newman newman-reporter-htmlextra
```

## 2. Como executar

Com a aplicação de pé:

```bash
newman run colecao.json -e ambiente.json -r cli,htmlextra
```

## 3. Estrutura e fluxo

As pastas são numeradas porque a execução é **sequencial e com estado**: cada uma depende do que a anterior deixou. Rodar uma pasta isolada geralmente falha.

### 📁 1. Cadastros

Cria os 4 usuários e os 2 times. Os `POST /cadastroTime` são precedidos de `POST /auth/login` porque a rota exige token — e **quem está autenticado vira o capitão**, então precisa estar na lista de membros.

### 📁 2. Pesquisas

Rotas públicas, sem token: listagem e busca de times, de usuários e de problemas, mais as duas recomendações do Neo4j (filtro colaborativo e popularidade por faixa de rating).

### 📁 3. Edição

* **Time (só o capitão):** remove e devolve um integrante, renomeia o time e transfere a capitania.
* **Usuário (só o dono da conta):** altera nome de exibição, e-mail, `nomeUsuario` (e desfaz) e senha. Trocar o `nomeUsuario` invalida o token antigo, porque o *subject* do JWT muda — a resposta já devolve um token novo, que a coleção captura.

### 📁 4. Deletar

Devolve o ambiente ao estado inicial, o que torna a coleção repetível — sem isso a segunda execução falharia com "usuário já cadastrado".

**A ordem importa:** os times são excluídos primeiro, porque quem é capitão não consegue excluir a própria conta. Os problemas ficam no Mongo de propósito (ver abaixo).

## 4. Os 4 usuários são handles reais do Codeforces

`arthurb.zanvetor`, `alexandre.dpierri`, `becastal` e `albert__` existem de verdade no Codeforces. Isso é necessário porque o cadastro consulta o perfil para preencher `rating` e `rank`, e dispara em background a sincronização dos problemas resolvidos.

Trocar por nomes inventados não quebra a coleção — as consultas ao Codeforces falham em silêncio por design — mas o `rating` vem zerado e as rotas de problema devolvem listas vazias.

## 5. Sobre as rotas de problema

A sincronização com o Codeforces é `@Async` e passa por *web scraping*, então **numa base nova o Mongo ainda está vazio quando a pasta de Pesquisas roda**. Duas consequências:

* `Listar problemas` captura o `idProblema` do primeiro item real em vez de assumir um id fixo, e as duas requisições que dependem dele (`Buscar problema` e `Usuários que fizeram o problema`) **se pulam sozinhas** quando não há nenhum. Isso mantém o relatório limpo em vez de gerar `404`.
* A limpeza não apaga os problemas — eles são cache do Codeforces. A partir da segunda execução já estão lá, e essas duas requisições passam a rodar.

Listas vazias em `Problemas feitos por` e nas recomendações também são `200`: o usuário existe, só ainda não tem nada no grafo.

## 6. Resultados

* **Terminal (CLI):** tabela com o total de requisições, asserções e falhas.
* **Relatório HTML:** uma pasta `newman/` é criada no diretório de execução, com um arquivo interativo contendo *headers*, corpos de requisição e respostas de tudo que rodou.

Cada requisição carrega uma asserção de `Status 200`, definida uma única vez no nível da coleção. Qualquer status diferente reprova.
