package com.APImaratona.Maratona.DTO.Usuario;

import lombok.Data;

import java.io.Serializable;

@Data
public class UsuarioResponseDTO implements Serializable  {
    private String nome;
    private String nomeUsuario;
    private String email;
    private String nomeTime;
    private String rank;
    private int rating;
}
