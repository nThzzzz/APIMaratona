package com.APImaratona.Maratona.DTO.Time;

import com.APImaratona.Maratona.DTO.Usuario.UsuarioResponseDTO;
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
