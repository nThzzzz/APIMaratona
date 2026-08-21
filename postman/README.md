# Demonstração ponta a ponta (Postman/Newman)

Esta coleção percorre o ciclo de vida completo da API Maratona contra um ambiente **real** — cadastro de usuários e times, consultas, edições e limpeza — batendo em PostgreSQL, MongoDB, Neo4j, Redis e na API do Codeforces de verdade.

**São 41 requisições e todas devem responder `200`.**

> Não confunda com a suíte de testes automatizados em Java (`./mvnw test`, 83 testes), descrita no [README principal](../README.md#-testes). Aquela roda sem infraestrutura nenhuma e é o que o CI executa. Esta aqui exige o ambiente de pé e serve para demonstrar o sistema funcionando.

## 1. Suba o ambiente

Na raiz do repositório:

```bash
docker compose up --build --wait
```

Isso levanta os quatro bancos e a API já configurada, e só devolve o terminal quando tudo estiver saudável (o `--wait` depende dos *healthchecks* declarados no compose). A API responde em `http://localhost:8080`. Se o seu Docker não reconhecer `docker compose`, use `docker-compose` com hífen.

Se preferir rodar a aplicação fora do container, veja as variáveis de ambiente necessárias no [`docker-compose.yml`](../docker-compose.yml) — são nove, e a aplicação não sobe sem elas.

## 2. Instale o Newman

```bash
npm install -g newman newman-reporter-htmlextra
```

## 3. Rode

Ainda na raiz do repositório:

```bash
newman run postman/colecao.json -e postman/ambiente.json -r cli,htmlextra
```

O esperado é:

```
│                requests │        41 │         0 │
│              assertions │        41 │         0 │
```

A asserção de `Status 200` é declarada uma única vez, no nível da coleção, e vale para toda requisição — qualquer status diferente reprova.

Ao terminar, `docker compose down -v` derruba tudo e apaga os dados.

## 4. Estrutura e fluxo

As pastas são numeradas porque a execução é **sequencial e com estado**: cada uma depende do que a anterior deixou. Rodar uma pasta isolada geralmente falha.

### 📁 1. Cadastros

Cria os 4 usuários e os 2 times. Cada `POST /cadastroTime` é precedido de um `POST /auth/login` porque a rota exige token — e **quem está autenticado vira o capitão**, então precisa constar na lista de membros.

### 📁 2. Pesquisas

Rotas públicas, sem token: listagem e busca de times, de usuários e de problemas, mais as duas recomendações do Neo4j (filtro colaborativo e popularidade por faixa de rating).

### 📁 3. Edição

* **Time (só o capitão):** remove e devolve um integrante, renomeia o time e transfere a capitania.
* **Usuário (só o dono da conta):** altera nome de exibição, e-mail, `nomeUsuario` (e desfaz) e senha. Trocar o `nomeUsuario` invalida o token antigo, porque o *subject* do JWT muda — a resposta já devolve um token novo, que a coleção captura.

### 📁 4. Deletar

Devolve o ambiente ao estado inicial, o que torna a coleção repetível: sem isso a segunda execução falharia com "usuário já cadastrado".

**A ordem importa** — os times são excluídos antes dos usuários, porque quem é capitão não consegue excluir a própria conta.

## 5. Os 4 usuários são handles reais do Codeforces

`arthurb.zanvetor`, `alexandre.dpierri`, `becastal` e `albert__` existem de verdade. Isso é necessário porque o cadastro consulta o perfil para preencher `rating` e `rank`, e dispara em background a sincronização dos problemas resolvidos.

Duas consequências práticas:

* **A coleção depende de internet.** Sem acesso ao Codeforces tudo continua respondendo `200` (as falhas são engolidas por design), mas o `rating` vem zerado e as rotas de problema devolvem listas vazias.
* Trocar por nomes inventados tem o mesmo efeito: não quebra nada, só esvazia os dados.

## 6. Primeira execução × execuções seguintes

A sincronização com o Codeforces é `@Async` e passa por *web scraping*, então **numa base nova o Mongo ainda está vazio quando a pasta de Pesquisas roda**. Isso é tratado, não é um problema:

* `Listar problemas` captura o `idProblema` de um item que existe de fato, em vez de assumir um id fixo.
  A rota é paginada, então o script lê os itens de `.content` (as três listagens devolvem um objeto `Page`,
  não um array).
* As duas requisições que dependem dele (`Buscar problema` e `Usuários que fizeram o problema`) **se pulam sozinhas** quando não há nenhum, em vez de gerar `404`.
* A limpeza não apaga os problemas — eles são cache do Codeforces. Da segunda execução em diante já estão lá e essas duas requisições passam a rodar de verdade.

Como referência, numa execução limpa a sincronização traz cerca de 140 problemas em segundo plano.

Listas vazias em `Problemas feitos por` e nas recomendações também são `200`: o usuário existe, só ainda não tem nada no grafo.

## 7. Arquivos

* **`colecao.json`** — a coleção `TestesAPIMaratona`.
* **`ambiente.json`** — as variáveis `url`, `token_jwt` e `idProblema`. Só `url` precisa de atenção (padrão `http://localhost:8080`); as outras duas são preenchidas em tempo de execução.

O reporter `htmlextra` cria uma pasta `newman/` no diretório de execução, com um relatório interativo contendo *headers*, corpos e respostas de tudo que rodou. Ela está no `.gitignore`.
