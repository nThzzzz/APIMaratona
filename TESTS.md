# Guia de Testes Automatizados: API Maratona

Este documento explica como executar a suíte de testes automatizados da API Maratona utilizando o Postman e o Newman. A coleção testa o ciclo de vida completo da aplicação, desde o cadastro de usuários até a exclusão de dados.

## 1. Pré-requisitos e Configuração

Para rodar os testes via terminal, você precisará dos arquivos exportados do Postman:
* **Arquivo de Ambiente:** Contém as variáveis `url` e `token_jwt`. A variável `url` deve estar preenchida com o endereço local da sua API (ex: `http://localhost:8080`).
* **Arquivo da Coleção:** Contém todas as requisições organizadas e os scripts de teste da coleção chamada `TestesAPIMaratona`.

**Instalação do Newman:**
Certifique-se de ter o Node.js instalado e execute o comando abaixo para instalar o executor e o gerador de relatórios visuais:
`npm install -g newman newman-reporter-htmlextra`

## 2. Como Executar os Testes

Com a sua aplicação Spring Boot rodando, abra o terminal na pasta onde os arquivos `.json` estão salvos e execute:

`newman run nome-da-colecao.json -e nome-do-ambiente.json -r cli,htmlextra`

*(Substitua os nomes dos arquivos pelos nomes exatos que você exportou).*

## 3. Estrutura da Coleção e Fluxo de Execução

A coleção está configurada para injetar automaticamente o token JWT de autenticação nas rotas protegidas usando a variável `{{token_jwt}}`. A execução segue uma ordem lógica de negócios separada nas seguintes pastas:

### 📁 Cadastros
Responsável por popular o banco de dados inicial.
* Cria múltiplos usuários no sistema através da rota `POST /cadastro`.
* Registra novos times através da rota `POST /cadastroTime`.

### 📁 Pesquisas
Testa os endpoints públicos e de listagem da API, divididos em três categorias:
* **Time:** Testa a listagem completa de times e a busca detalhada de um time por nome.
* **Usuario:** Testa a listagem de todos os usuários e a busca específica por nome de usuário.
* **Problemas:** Valida as consultas de problemas, os relacionamentos (quem resolveu qual problema) e os algoritmos de recomendação baseados em rating e similaridade.

### 📁 Edição
Executa ações que modificam dados existentes e exigem autenticação.
* Inicia com um `POST /auth/login`, que contém um script para capturar e salvar o token JWT gerado na variável de ambiente.
* Testa a adição e remoção de usuários de um time usando `PUT /adicionarUsuario` e `PUT /removerUsuario`.
* Testa a edição de credenciais e dados do perfil do usuário em `PUT /editarUsuario/{nomeUsuario}`.

### 📁 Deletar
Faz a limpeza dos dados gerados durante o teste, garantindo que o banco não fique poluído.
* Realiza o login de diferentes usuários criados no passo de cadastro para adquirir os tokens de exclusão.
* Executa rotas de `DELETE /excluirUsuario` e `DELETE /excluirTime` para remover permanentemente os registros.

## 4. Resultados Esperados

Ao finalizar a execução, o Newman apresentará dois resultados principais:
1. **Terminal (CLI):** Uma tabela resumindo o tempo total de execução, a quantidade de requisições disparadas e o status de falha/sucesso dos testes.
2. **Relatório HTML:** Uma pasta chamada `newman` será criada automaticamente no diretório. Dentro dela, haverá um arquivo `.html` interativo contendo os detalhes exatos de todos os *cabeçalhos (headers)*, *corpos de requisição (bodies)* e *respostas* geradas pela API durante a automação.
