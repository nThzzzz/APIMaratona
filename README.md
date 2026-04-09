# 🏆 API Maratona (Codeforces Integration)

Um sistema backend robusto desenvolvido em **Java com Spring Boot** para gerenciar competidores e times de maratonas de programação. O projeto foi desenhado com uma arquitetura escalável e segura, focado em regras de negócio estritas e preparação para **Persistência Poliglota** (PostgreSQL, MongoDB, Neo4j e Redis) e integração com a API do Codeforces.

---

## 🚀 Tecnologias Utilizadas

* **Java 21**
* **Spring Boot 4.0.2**
* **PostgreSQL** (Banco de Dados Relacional Principal)
* **Spring Data JPA / Hibernate** (Mapeamento Objeto-Relacional)
* **Neo4j** (Grafos - Configurado para o futuro Motor de Recomendações)
* **MongoDB** (Banco Orientado a Documentos - Preparado para textos longos/HTML)
* **Redis** (Banco de Dados em Memória - Camada de Cache)
* **Lombok** (Redução de Boilerplate)
* **Maven** (Gerenciamento de Dependências)

---

## 🏗️ Arquitetura e Padrões Aplicados

O projeto segue os princípios de **Clean Code** e a arquitetura em camadas padrão do Spring (Controller, Service, Repository, Model).

### 🧼 Princípios de Clean Code Aplicados:
* **Single Responsibility Principle (SRP):** Separação estrita de responsabilidades. `Controllers` lidam apenas com o tráfego HTTP, `Services` orquestram as regras de negócio e `Repositories` gerenciam o acesso a dados.
* **Meaningful Names:** Nomenclatura clara e reveladora de intenção para métodos e variáveis (ex: `adicionarUsuarioNoTime`, `senhaAntiga`, `senhaNova`), dispensando comentários desnecessários.
* **Fail-Fast & Exceções:** Uso de blocos de validação no início dos métodos lançando exceções claras (ex: `throw new RuntimeException("Time inexistente")`) em vez de retornos de erro genéricos ou múltiplos `if/else` aninhados.
* **Injeção de Dependência Limpa:** Utilização de injeção de dependência via construtor (com `@RequiredArgsConstructor` do Lombok) garantindo imutabilidade (`private final`) e segurança na instanciação dos componentes.

### ⚙️ Destaques de Engenharia:
* **Padrão DTO (Data Transfer Object):** Separação estrita entre o modelo de banco de dados (`Usuario`, `Time`) e os dados expostos nas requisições e respostas (`UsuarioResponseDTO`, `TimeRequisicaoDTO`, etc.). Isso garante que dados sensíveis, como senhas, nunca sejam vazados em listagens públicas.
* **Validação e Blindagem no Service:** Regras de negócio fortes aplicadas na camada de serviço, como limitação rigorosa de 3 membros por time, prevenção de e-mails duplicados e proteção contra valores nulos na atualização de dados.

---

## 📡 Documentação da API (Endpoints)

### 👤 Usuários (`/`)
Responsável pelo gerenciamento dos competidores.

| Método | Endpoint | Descrição | Body / Params |
| :--- | :--- | :--- | :--- |
| `GET` | `/teste` | Health check da aplicação. | - |
| `POST` | `/cadastro` | Cadastra um novo usuário. | `UsuarioRequisicaoDTO` |
| `GET` | `/listaUsuarios` | Retorna todos os usuários (sem expor senhas). | - |
| `GET` | `/buscarUsuario` | Busca um usuário específico. | Query: `?nomeUsuario=` ou `?email=` |
| `PUT` | `/editarUsuario/{nomeUsuario}` | Edita os dados e o time do usuário. | Path: `nomeUsuario` <br> Body: `EditarUsuarioRequisicaoDTO` |
| `DELETE` | `/excluirUsuario` | Exclui o usuário validando a senha. | Body: `ExcluirUsuarioRequisicaoDTO` |

### 🛡️ Times (`/`)
Responsável pelo gerenciamento dos times da maratona.

| Método | Endpoint | Descrição | Body / Params |
| :--- | :--- | :--- | :--- |
| `POST` | `/cadastroTime` | Cadastra um time e (opcionalmente) seus membros iniciais. | `TimeRequisicaoDTO` |
| `GET` | `/listarTimes` | Lista todos os times e seus membros. | - |
| `GET` | `/buscarTime` | Busca um time específico pelo nome. | Query: `?nome=` |
| `PUT` | `/adicionarUsuario` | Adiciona um ou mais usuários a um time existente (Limite: 3). | `TimeRequisicaoDTO` |
| `PUT` | `/removerUsuario` | Remove usuários específicos de um time. | `TimeRequisicaoDTO` |
| `DELETE` | `/excluirTime` | Exclui o time (os usuários são mantidos no sistema como "Sem time"). | Query: `?nome=` |

---

## 🔮 Próximos Passos (Roadmap)

1.  **Global Exception Handler:** Implementação de um `@RestControllerAdvice` para padronizar as respostas de erro (400 Bad Request, 404 Not Found) e substituir os retornos de Erro 500.
2.  **Web Scraping & Codeforces API:** Integração com o `user.status` do Codeforces para extrair as submissões aprovadas de cada usuário e acessar o texto dos problemas (via Jsoup).
3.  **Integração MongoDB:** Armazenamento de textos densos (descrições longas, exemplos de input/output e HTML dos problemas extraídos).
4.  **Integração Neo4j:** Construção de um sistema de recomendação baseado em grafos conectando Usuários ➔ Tags de Problemas Resolvidos.
5.  **Camada de Cache com Redis:** Otimização da aplicação mantendo as descrições dos problemas mais acessados em cache na memória, diminuindo a latência das requisições e aliviando a carga sobre o MongoDB.

---
