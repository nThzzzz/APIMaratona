# 🏆 API Maratona (Codeforces Integration)

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
* **Jsoup** (Web Scraping)
* **Lombok & Maven**

---

## 🏗️ Arquitetura e Destaques de Engenharia

O projeto segue os princípios de **Clean Code** e a arquitetura em camadas padrão do Spring (Controller, Service, Repository, Model), com separação estrita de responsabilidades.

### ⚙️ Funcionalidades Avançadas:
* **Sincronização com Codeforces:** Integração via `RestTemplate` para buscar submissões aprovadas em background (`@Async`), aliada a um **Web Scraper (Jsoup)** que extrai o texto original do problema diretamente do site.
* **Motor de Recomendação:** Consultas complexas em linguagem Cypher (Neo4j) para recomendar problemas baseados em similaridade de resolução (Filtro Colaborativo) e popularidade por faixa de rating.
* **Gerenciamento de Cache Cirúrgico:** Uso avançado das anotações `@Cacheable` e `@Caching(evict = ...)` para entregar respostas em milissegundos via Redis, garantindo a invalidação inteligente apenas das chaves afetadas durante atualizações (evitando "cache stale").
* **Transações Poliglotas:** Gerenciadores de transação isolados (`@Primary` para o Postgres e um específico para o Neo4j), prevenindo esgotamento de *Connection Pools* e garantindo a integridade entre os diferentes bancos de dados.
* **Tratamento de Exceções Global:** Um `@RestControllerAdvice` intercepta regras de negócio e erros de banco, padronizando as respostas HTTP (`400 Bad Request`, `404 Not Found`) através do padrão DTO.

---

## 📡 Documentação da API (Endpoints)

### 👤 Usuários (`/`)
Gerencia o cadastro, edição e exclusão de competidores.

| Método | Endpoint | Descrição |
| :--- | :--- | :--- |
| `POST` | `/cadastro` | Cadastra um usuário e dispara a sync do Codeforces. |
| `GET` | `/listaUsuarios` | Retorna todos os usuários (sem expor senhas). |
| `GET` | `/buscarUsuario` | Busca um usuário (Query: `?nomeUsuario=` ou `?email=`). |
| `PUT` | `/editarUsuario/{nomeUsuario}` | Edita os dados e/ou o time do usuário. |
| `DELETE` | `/excluirUsuario` | Exclui o usuário validando a senha antiga. |

### 🛡️ Times (`/`)
Gerencia os times (limitados a 3 integrantes).

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

## 🔮 Próximos Passos (Roadmap)

1. **Testes Unitários e de Integração:** Adicionar cobertura de testes com JUnit 5 e Mockito para garantir a estabilidade das regras de negócio.
2. **Dockerização:** Criar um `Dockerfile` e um `docker-compose.yml` para subir toda a infraestrutura poliglota localmente com um único comando.
3. **Segurança (Spring Security & JWT):** Substituir a validação manual de senhas por autenticação via Tokens JWT e encriptação de senhas no banco usando BCrypt.
