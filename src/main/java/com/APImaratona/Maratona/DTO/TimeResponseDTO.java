package com.APImaratona.Maratona.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeResponseDTO {
    private String nomeTime;
    private List<UsuarioResponseDTO> usuarios;
}
