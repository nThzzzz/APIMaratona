package com.APImaratona.Maratona.DTO.Time;

import jakarta.validation.constraints.NotBlank;

public interface TimeRequest {

    // --------------------------- Edicao ----------------------------

    // O time alvo e o capitao atual nao entram no corpo: vem do path e do token.
    record AlterarNomeTime(
            @NotBlank(message = "Nome do time nulo") String nomeTimeNovo) {}

    record TransferirCapitania(
            @NotBlank(message = "Nome do novo capitão nulo") String nomeCapitaoNovo) {}
}
