package com.APImaratona.Maratona.DTO.Codeforces;

import lombok.Data;

import java.util.List;

@Data
public class CodefrocesUsuarioResponseDTO {
    private String status;
    private List<CodeforcesUsuarioDTO> result;
}
