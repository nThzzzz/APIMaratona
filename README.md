# 🏆 API Maratona (Codeforces Integration)

![GitHub repo size](https://img.shields.io/github/repo-size/nThzzzz/APImaratona?style=for-the-badge)
![GitHub code size in bytes](https://img.shields.io/github/languages/code-size/nThzzzz/APImaratona?style=for-the-badge)
![GitHub top language](https://img.shields.io/github/languages/top/nThzzzz/APImaratona?style=for-the-badge)
![GitHub license](https://img.shields.io/github/license/nThzzzz/APImaratona?style=for-the-badge)
![GitHub contributors](https://img.shields.io/github/contributors/nThzzzz/APImaratona?style=for-the-badge)

![GitHub last commit](https://img.shields.io/github/last-commit/nThzzzz/APImaratona?style=for-the-badge)


Um sistema backend robusto desenvolvido em **Java com Spring Boot** para gerenciar competidores e times de maratonas de programação. O projeto possui uma arquitetura escalável e segura, aplicando **Persistência Poliglota** real (PostgreSQL, MongoDB, Neo4j e Redis) e integração assíncrona com a API do Codeforces.

---

## 🚀 Tecnologias Utilizadas

* **Java 21**
* **Spring Boot 4.0.5**
* **PostgreSQL** (Relacional) - Gerenciamento transacional de Usuários e Times.
* **MongoDB** (NoSQL Documentos) - Armazenamento de textos densos e HTML (Web Scraping dos problemas).
* **Neo4j** (NoSQL Grafos) - Motor de recomendação e mapeamento de relações complexas (`(Usuário)-[:RESOLVEU]->(Problema)`).
* **Redis** (In-Memory Cache) - Cache distribuído de alta performance para listagens e consultas frequentes.
* **Spring Data / Hibernate** (JPA, MongoRepository, Neo4jRepository)
* **Spring Security + JWT** (`io.jsonwebtoken`/jjwt) - Autenticação stateless via token Bearer, com senhas protegidas por hash **BCrypt**.
* **Jsoup** (Web Scraping)
* **Lombok & Maven**

---

## 🏗️ Arquitetura e Destaques de Engenharia

O projeto segue os princípios de **Clean Code** e a arquitetura em camadas padrão do Spring (Controller, Service, Repository, Model), com separação estrita de responsabilidades e aplicação prática do Princípio da Responsabilidade Única (SRP).

### ⚙️ Funcionalidades Avançadas:
* **Sincronização com Codeforces:** Integração via `RestTemplate` para buscar submissões aprovadas em background (`@Async`), aliada a um **Web Scraper (Jsoup)** que extrai o texto original do problema diretamente do site.
* **Motor de Recomendação:** Consultas complexas em linguagem Cypher (Neo4j) para recomendar problemas baseados em similaridade de resolução (Filtro Colaborativo) e popularidade por faixa de rating.
* **Gerenciamento de Cache Cirúrgico:** Uso avançado das anotações `@Cacheable` e `@Caching(evict = ...)` para entregar respostas em milissegundos via Redis, garantindo a invalidação inteligente apenas das chaves afetadas durante atualizações (evitando "cache stale").
* **Transações Poliglotas:** Gerenciadores de transação isolados (`@Primary` para o Postgres e um específico para o Neo4j), prevenindo esgotamento de *Connection Pools* e garantindo a integridade entre os diferentes bancos de dados.
* **Autenticação Stateless (JWT):** `POST /auth/login` emite um token assinado (jjwt) validado a cada requisição por um filtro do Spring Security (`JwtAuthenticationFilter`), sem sessão ou cookie. Senhas são persistidas com hash **BCrypt**, nunca em texto puro.
* **Tratamento de Exceções Global:** Um `@RestControllerAdvice` intercepta regras de negócio e erros de banco, padronizando as respostas HTTP (`400 Bad Request`, `404 Not Found`) através do padrão DTO.

---

## 🔐 Autenticação e Segurança

A API usa **Spring Security + JWT** (stateless, sem sessão/cookie) para proteger as operações mais sensíveis sobre a própria conta do usuário. A arquitetura de segurança aplica o conceito de **Defesa em Profundidade**:

* **Login:** `POST /auth/login` recebe `nomeUsuario` e `senha` e devolve `{ "token": "...", "tipo": "Bearer" }`.
* **Uso do token:** envie o header `Authorization: Bearer <token>` nas rotas protegidas. Um filtro (`JwtAuthenticationFilter`) valida o token em toda requisição; se estiver ausente, inválido ou expirado, a rota protegida responde `401` com um corpo JSON padronizado (`JwtAuthenticationEntryPoint`).
* **Dono do recurso:** o `nomeUsuario` contido no token precisa ser o mesmo da conta alvo da operação — token válido de outro usuário também resulta em `401`.
* **Senhas (Sudo Mode):** Senhas são armazenadas com hash **BCrypt**. Para operações altamente sensíveis (como alterar e-mail, senha ou excluir a conta), o sistema exige a validação da senha atual no corpo da requisição, mesmo com o token JWT válido.
* **Escopo atual (deliberadamente reduzido):** por enquanto, as rotas de edição de usuário (`PUT /editarUsuario/**`) e exclusão (`DELETE /excluirUsuario`) (marcadas com 🔒 abaixo) exigem token. O restante da API permanece público.

---

## 📡 Documentação da API (Endpoints)

### 🔑 Autenticação (`/auth`)
Emite o token JWT usado nas rotas protegidas.

| Método | Endpoint | Descrição |
| :--- | :--- | :--- |
| `POST` | `/auth/login` | Autentica com `nomeUsuario`/`senha` e retorna o token JWT. |

### 👤 Usuários (`/`)
Gerencia o cadastro, edição de perfil/credenciais e exclusão de competidores.

| Método | Endpoint                                   | Descrição |
| :--- |:-------------------------------------------| :--- |
| `POST` | `/cadastro`                                | Cadastra um usuário e dispara a sync do Codeforces. |
| `GET` | `/listaUsuarios`                           | Retorna todos os usuários (sem expor senhas). |
| `GET` | `/buscarUsuario`                           | Busca um usuário (Query: `?nomeUsuario=` ou `?email=`). |
| `PUT` | `/editarUsuario/perfil/{nomeUsuario}`      | 🔒 Edita dados estéticos do perfil (ex: nome). Exige apenas o token JWT válido. |
| `PUT` | `/editarUsuario/credenciais/{nomeUsuario}` | 🔒 Edita dados sensíveis (e-mail, nome de usuário, nova senha). Exige o token JWT **e** a `senhaAntiga`. |
| `DELETE` | `/excluirUsuario`                          | 🔒 Exclui o usuário. Exige o token JWT **e** a confirmação da `senha`. |

### 🛡️ Times (`/`)
Gerencia os times (limitados a 3 integrantes) e a entrada/saída de membros.

| Método | Endpoint | Descrição |
| :--- | :--- | :--- |
| `POST` | `/cadastroTime` | Cadastra um time e (opcionalmente) seus membros. |
| `GET` | `/listarTimes` | Lista todos os times e seus membros. |
| `GET` | `/buscarTime` | Busca um time específico (Query: `?nome=`). |
| `PUT` | `/adicionarUsuario` | Adiciona usuários a um time existente. |
| `PUT` | `/removerUsuario` | Remove usuários específicos de um time. |
| `DELETE` | `/excluirTime` | Exclui o time (usuários ficam "Sem time"). |

### 🧩 Problemas e Recomendações (`/`)
Consulta os problemas resolvidos e cruza dados entre Mongo, Neo4j e Redis.

| Método | Endpoint | Descrição |
| :--- | :--- | :--- |
| `GET` | `/{idProblema}` | Traz o HTML e os dados completos do problema (Mongo). |
| `GET` | `/listarProblemas` | Lista todos os problemas cacheados (Redis). |
| `GET` | `/usuariosFizeramProblema/{idProblema}`| Lista quem resolveu uma questão específica (Neo4j). |
| `GET` | `/problemasFeitorPor/{nomeUsuario}`| Lista as questões resolvidas por um usuário. |
| `GET` | `/recomendarProblemaSimilaridade/{nome}`| Recomenda problemas via Filtro Colaborativo (Neo4j). |
| `GET` | `/recomendarProblemaRating/{nome}`| Recomenda os problemas mais populares na faixa de rating do usuário. |

---

## 🧪 Testes

A suíte é pensada para rodar **sem depender de infraestrutura real** (Postgres/Mongo/Neo4j/Redis) no dia a dia:

* **Testes de controller** (`@WebMvcTest`, services mockados com Mockito) cobrem o contrato HTTP de cada controller. Em especial, `ControllerUsuarioSecurityTest` sobe a cadeia **real** do Spring Security (`SecurityConfig` + `JwtAuthenticationFilter` + `JwtService`) para validar `401` sem token, `401` com token inválido e `200` com um token válido de verdade — usando um `jwt.secret` de teste via `@TestPropertySource`, sem tocar em nenhum banco.
* **`JwtServiceTest`** é um teste unitário puro (sem contexto Spring) da geração/validação do token: caminho feliz, token malformado, expirado e assinado com outro segredo.
* **`MaratonaApplicationTests`** (`contextLoads`) sobe o contexto completo da aplicação e por isso exige Postgres/Mongo/Neo4j/Redis e um `JWT_SECRET` reais. É marcado com `@Tag("integration")` e fica de fora do `./mvnw test` padrão.

### Rodando localmente

```bash
./mvnw test
