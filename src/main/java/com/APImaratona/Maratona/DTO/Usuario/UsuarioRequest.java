package com.APImaratona.Maratona.DTO.Usuario;

import jakarta.validation.constraints.NotBlank;

public interface UsuarioRequest {

    // --------------------------- Edicao ----------------------------

    record AlterarSenha(
            @NotBlank(message = "Senha atual nula")String senhaAtual,
            @NotBlank(message = "Senha nova nula")String senhaNova) {}

    record AlterarEmail(
            @NotBlank(message = "Email nulo") String emailNovo,
            @NotBlank(message = "Senha nulo")String senhaAtual) {}

    record AlterarNomeUsuario(
            @NotBlank(message = "Nome de usuário nulo") String nomeUsuarioNovo,
            @NotBlank(message = "Senha nulo") String senhaAtual) {}

    record AlterarNome(
            @NotBlank(message = "Nome nulo")String nomeNovo) {}

    // -------------------------- Cadastro ---------------------------

    record CadastrarUsuario(
            @NotBlank(message = "Nome nulo") String nome,
            @NotBlank(message = "Email nulo")String email,
            @NotBlank(message = "Senha nulo")String senha,
            @NotBlank(message = "Nome de usuário nulo")String nomeUsuario,
            String nomeTime){}

    // -------------------------- Deletar ----------------------------

    record ExcluirUsuarioEmail(
            @NotBlank(message = "Email nulo") String email,
            @NotBlank(message = "Senha nulo")String senhaAtual) {}

    record ExcluirUsuarioNomeUsuario(
            @NotBlank(message = "Senha nulo")String senhaAtual) {}

    // --------------------------- Login -----------------------------

    record Login(
            @NotBlank(message = "Email nulo") String email,
            @NotBlank(message = "Senha nulo")String senhaAtual) {}
}
