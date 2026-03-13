package com.APImaratona.Maratona.DTO;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CadastroRequisicaoDTO {
    private String nome;
    private String email;
    private String senha;
    private String nomeUsuario;

    private String nomeTime;

    public CadastroRequisicaoDTO(String nome, String email, String senha, String nomeUsuario) {
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.nomeUsuario = nomeUsuario;
    }
}
