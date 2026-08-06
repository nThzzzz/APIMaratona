package com.APImaratona.Maratona.DTO.Time;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EditarTimeRequest {
    private String nomeTimeAtual;
    private String nomeTimeNovo;

    private String nomeCapitaoAtual;
    private String nomeCapitaoNovo;
}
