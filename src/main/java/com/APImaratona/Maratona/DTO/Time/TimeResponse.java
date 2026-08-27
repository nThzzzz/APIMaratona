package com.APImaratona.Maratona.DTO.Time;

import com.APImaratona.Maratona.DTO.Usuario.UsuarioResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeResponse {
    private String nomeTime;

    // Sem isto o cliente nao tem como saber quem e o capitao, e as acoes que so ele pode
    // fazer (adicionar, remover, renomear, transferir, excluir) so dariam para descobrir
    // tentando e lendo o 400. Null em times gravados antes da coluna capitao_id existir.
    private String nomeCapitao;

    private List<UsuarioResponse> usuarios;
}
