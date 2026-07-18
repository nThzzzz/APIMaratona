package com.APImaratona.Maratona.DTO.Usuario;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequisicaoDTO {
    private String nomeUsuario;
    private String senha;
}
