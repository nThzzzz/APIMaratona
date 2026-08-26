# 🏆 API Maratona (Codeforces Integration)

![GitHub repo size](https://img.shields.io/github/repo-size/nThzzzz/API-Maratona?style=for-the-badge)
![GitHub code size in bytes](https://img.shields.io/github/languages/code-size/nThzzzz/API-Maratona?style=for-the-badge)
![GitHub top language](https://img.shields.io/github/languages/top/nThzzzz/API-Maratona?style=for-the-badge)
![GitHub license](https://img.shields.io/github/license/nThzzzz/API-Maratona?style=for-the-badge)
![GitHub contributors](https://img.shields.io/github/contributors/nThzzzz/API-Maratona?style=for-the-badge)

![GitHub last commit](https://img.shields.io/github/last-commit/nThzzzz/API-Maratona?style=for-the-badge)


API em Java e Spring Boot para gerenciar competidores e times de maratona de programação, integrada ao Codeforces. Cada usuário é um handle real, e o sistema puxa o perfil dele e os problemas que resolveu para montar recomendações.

O ponto do projeto é persistência poliglota de verdade: PostgreSQL, MongoDB, Neo4j e Redis, cada um resolvendo uma parte diferente do problema. O porquê de cada escolha está em [`docs/ARQUITETURA.md`](docs/ARQUITETURA.md).

---

## 🚀 Tecnologias

* **Java 21** e **Spring Boot 4.0.5**
* **PostgreSQL** para usuários e times, a fonte da verdade
* **MongoDB** para o catálogo de problemas (enunciado, tags, rating)
* **Neo4j** para o grafo `(Usuário)-[:RESOLVEU]->(Problema)`, usado só pelas recomendações
* **Redis** para o cache de resposta
* **Spring Data** (JPA, MongoRepository, Neo4jRepository)
* **Spring Security + JWT** (jjwt), com senhas em BCrypt
* **Jsoup** para o scraping dos enunciados
* **Lombok** e **Maven**

---

## 🐳 Como rodar

A aplicação depende de quatro bancos e de nove variáveis de ambiente, então o caminho mais curto é o Docker:

```bash
docker compose up --build --wait
```

Isso levanta PostgreSQL, MongoDB, Neo4j, Redis e a API já configurada, e só devolve o terminal quando tudo estiver saudável. A API sobe em `http://localhost:8080` e o estado dos bancos aparece em `/actuator/health`.

Para derrubar e apagar os dados: `docker compose down -v`.

> Se o seu Docker não reconhecer `docker compose` (o plugin v2), use `docker-compose` com hífen. Os comandos são iguais.

> As credenciais no [`docker-compose.yml`](docker-compose.yml) são fixas e servem para desenvolvimento. Para qualquer outro uso, gere seu próprio `JWT_SECRET` (`openssl rand -base64 32`) e troque as senhas dos bancos.

Rodando fora do container, as variáveis exigidas no boot são `DATABASE_URL`, `DATABASE_USUARIO`, `DATABASE_SENHA`, `MONGO_URI`, `REDIS_URL`, `NEO4J_URI`, `NEO4J_USUARIO`, `NEO4J_SENHA` e `JWT_SECRET`. A aplicação não inicia sem elas.

---

## 🏗️ Arquitetura

Camadas padrão do Spring: Controller, Service, Repository, Model. O que tem de interessante está em como os quatro bancos se dividem e no que acontece nas bordas.

* **Sincronização com o Codeforces.** No cadastro, a API busca as submissões aceitas do handle e registra cada problema resolvido. Roda em background (`@Async`) porque são centenas de itens.
* **Recomendação em Cypher.** Duas consultas no Neo4j: filtro colaborativo (quem resolveu o que eu resolvi também resolveu o quê) e popularidade dentro da faixa de rating do usuário.
* **Dois `TransactionManager`.** Um para o Postgres, outro para o Neo4j. Não existe transação distribuída entre os stores, e o sistema é desenhado para tolerar a divergência: o que é vital fica no Postgres.
* **Cache no Redis** nas listagens e nas consultas de relacionamento, com invalidação nas escritas que afetam cada chave.
* **Records e `@Valid`** nos DTOs de entrada, validados na borda do controller.
* **Erros padronizados.** Um `@RestControllerAdvice` traduz as exceções de domínio para `400`, `401` e `404` num mesmo formato de resposta. Os 401 levantados antes do controller são escritos pelo filtro, no mesmo formato.

> [!NOTE]
> 🧭 Por que quatro bancos, por que raspar HTML se existe API, o que quebra quando um store grava e o outro não, e o que hoje é dívida técnica: [`docs/ARQUITETURA.md`](docs/ARQUITETURA.md).

---

## 🔐 Autenticação e Segurança

Autenticação stateless com JWT, sem sessão nem cookie.

* **Login.** `POST /auth/login` recebe `nomeUsuario` e `senhaAtual`, devolve `{ "token": "...", "tipo": "Bearer" }`. O *subject* do token é o `nomeUsuario`.
* **Uso do token.** Mande `Authorization: Bearer <token>`. Um filtro valida em toda requisição; ausente, inválido ou expirado responde `401` em JSON.
* **Deny by default.** As rotas públicas estão listadas explicitamente no `SecurityConfig` e todo o resto exige token. Rota nova que ninguém classificar nasce fechada, o que é o modo certo de errar.
* **Dono do recurso.** O `nomeUsuario` do token precisa ser o da conta alvo. Token válido de outra pessoa também dá `401`.
* **Senha de novo nas operações sensíveis.** Alterar e-mail, senha, `nomeUsuario` ou excluir a conta exigem a `senhaAtual` no corpo mesmo com token válido. O token diz quem você é; a senha confirma que você está ali naquele momento.
* **Capitão do time.** Só o capitão adiciona membro, remove membro, renomeia, transfere a capitania ou exclui o time. Enquanto for capitão, não consegue excluir a própria conta: precisa transferir antes. O novo capitão tem que já ser integrante.
* **Rate limit** por IP em `/auth/login` e `/cadastro`, as duas rotas que não exigem token. Contador em memória, com as limitações disso documentadas no próprio filtro.

Nas tabelas abaixo, 🔒 marca o que exige token.

---

## 📡 Endpoints

### 🔑 Autenticação

| Método | Endpoint | Descrição |
| :--- | :--- | :--- |
| `POST` | `/auth/login` | Autentica com `nomeUsuario`/`senhaAtual` e retorna o token JWT. |

### 👤 Usuários

| Método | Endpoint                                             | Descrição |
| :---   |:-----------------------------------------------------| :--- |
| `POST` | `/cadastro`                                          | Cadastra um usuário e dispara a sincronização com o Codeforces. |
| `GET`  | `/listaUsuarios`                                     | Lista usuários, sem expor senha. Paginado: `?page=&size=&sort=`. |
| `GET`  | `/buscarUsuario/{nomeUsuario}`                       | Busca um usuário pelo nome de usuário. |
| `PUT`  | `/editarUsuario/perfil/{nomeUsuario}/nome`           | 🔒 Altera o nome de exibição. |
| `PUT`  | `/editarUsuario/credenciais/{nomeUsuario}/email`     | 🔒 Altera o e-mail. Exige a `senhaAtual`. |
| `PUT`  | `/editarUsuario/credenciais/{nomeUsuario}/senha`     | 🔒 Altera a senha. Exige a `senhaAtual`. |
| `PUT`  | `/editarUsuario/credenciais/{nomeUsuario}/nomeUsuario`| 🔒 Altera o username. Reflete no Postgres, no Neo4j e num token novo. Exige a `senhaAtual`. |
| `DELETE`| `/excluirUsuario/{nomeUsuario}/email`               | 🔒 Exclui a conta confirmando com o e-mail e a `senhaAtual`. |
| `DELETE`| `/excluirUsuario/{nomeUsuario}/nomeUsuario`         | 🔒 Exclui a conta confirmando com a `senhaAtual`. |

### 🛡️ Times

Times têm no máximo 3 integrantes.

| Método | Endpoint | Descrição |
| :--- | :--- | :--- |
| `POST` | `/cadastroTime` | 🔒 Cria o time. Quem cria vira capitão e precisa estar na lista de membros. |
| `GET` | `/listarTimes` | Lista os times e seus membros. Paginado: `?page=&size=&sort=`. |
| `GET` | `/buscarTime` | Busca um time (query: `?nome=`). |
| `PUT` | `/adicionarUsuario` | 🔒 Adiciona integrantes. Só o capitão. |
| `PUT` | `/removerUsuario` | 🔒 Remove integrantes. Só o capitão. |
| `PUT` | `/editarTime/{nomeTime}/nome` | 🔒 Renomeia o time. Só o capitão. |
| `PUT` | `/editarTime/{nomeTime}/capitao` | 🔒 Transfere a capitania para outro integrante. Só o capitão. |
| `DELETE` | `/excluirTime` | 🔒 Exclui o time. Os integrantes ficam sem time, não são apagados. |

### 🧩 Problemas e recomendações

| Método | Endpoint | Descrição |
| :--- | :--- | :--- |
| `GET` | `/buscarProblema/{idProblema}` | Dados do problema, vindos do Mongo. |
| `GET` | `/listarProblemas` | Lista os problemas, com cache no Redis. Paginado: `?page=&size=&sort=`. |
| `GET` | `/usuariosFizeramProblema/{idProblema}`| Quem resolveu um problema (Neo4j). |
| `GET` | `/problemasFeitorPor/{nomeUsuario}`| Problemas resolvidos por um usuário. |
| `GET` | `/recomendarProblemaSimilaridade/{nome}`| Recomendação por filtro colaborativo. |
| `GET` | `/recomendarProblemaRating/{nome}`| Problemas mais resolvidos na faixa de rating do usuário. |

---

## 🧪 Testes

São **86 testes**, e todos rodam sem precisar de banco nenhum:

```bash
./mvnw test
```

* **Testes de controller** (`@WebMvcTest` com os services mockados) cobrem o contrato HTTP de cada rota. Dois deles, `ControllerUsuarioSecurityTest` e `ControllerTimeSecurityTest`, sobem a cadeia real do Spring Security com um `jwt.secret` de teste, e é onde ficam as regressões de proteção de rota: já aconteceu duas vezes de um matcher sem `/**` deixar rota aberta sem ninguém perceber.
* **`UsuarioServiceSegurancaTest`** testa as checagens de senha usando BCrypt de verdade. Mockar o encoder esconderia justamente o tipo de bug que esses testes travam, como comparar hash com texto puro.
* **`TimeServiceTest`** cobre as regras de capitão, incluindo o caso de time gravado antes da coluna existir, que antes estourava NPE.
* **`JwtServiceTest`** é unitário puro: token válido, malformado, expirado e assinado com outro segredo.
* **`MaratonaApplicationTests`** sobe o contexto completo e por isso precisa dos quatro bancos reais. Está marcado com `@Tag("integration")` e fica fora do `./mvnw test` padrão.

> [!NOTE]
> 🧪 Tem também uma coleção Postman/Newman de 41 requisições que exercita a API contra o ambiente real, com bancos e Codeforces de verdade: [`postman/`](postman/README.md).
