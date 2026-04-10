package com.APImaratona.Maratona.DTO.Usuario;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class EditarUsuarioRequisicaoDTO {

    private String nome;
    private String email;
    private String nomeUsuario;
    private String nomeTime;

    //senha antiga necessaria
    private String senhaAntiga;
    private String senhaNova;
}
