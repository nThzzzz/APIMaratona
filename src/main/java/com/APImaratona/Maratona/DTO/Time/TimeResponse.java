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
    private List<UsuarioResponse> usuarios;
}
