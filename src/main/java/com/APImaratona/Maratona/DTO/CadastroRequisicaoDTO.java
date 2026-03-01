package com.APImaratona.Maratona.DTO;

public class CadastroRequisicaoDTO {
    private String nome;
    private String email;
    private String senha;
    private String nomeUsuario;

    private String nomeTime;

    public CadastroRequisicaoDTO() {}

    public CadastroRequisicaoDTO(String nome, String email, String senha, String nomeUsuario, String nomeTime) {
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.nomeUsuario = nomeUsuario;
        this.nomeTime = nomeTime;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getNomeUsuario() {
        return nomeUsuario;
    }

    public void setNomeUsuario(String nomeUsuario) {
        this.nomeUsuario = nomeUsuario;
    }

    public String getNomeTime() {
        return nomeTime;
    }

    public void setNomeTime(String nomeTime) {
        this.nomeTime = nomeTime;
    }
}
