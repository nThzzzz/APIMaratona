package com.APImaratona.Maratona.DTO.Usuario;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class EditarUsuarioCredenciaisRequisicaoDTO {

    private String email;
    private String nomeUsuario;

    // Senha antiga necessaria
    private String senhaAntiga;
    private String senhaNova;
}
