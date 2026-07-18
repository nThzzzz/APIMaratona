package com.APImaratona.Maratona.DTO.Usuario;

import com.APImaratona.Maratona.Model.Usuario;
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

    public static UsuarioResponseDTO fromEntity(Usuario usuario) {
        UsuarioResponseDTO dto = new UsuarioResponseDTO();

        dto.setNome(usuario.getNome());
        dto.setEmail(usuario.getEmail());
        dto.setNomeUsuario(usuario.getNomeUsuario());
        dto.setNomeTime(usuario.getTime() != null ? usuario.getTime().getNome() : "Sem time");
        dto.setRank(usuario.getRank());
        dto.setRating(usuario.getRating());

        return dto;
    }
}
