package com.APImaratona.Maratona.DTO.Usuario;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String tipo; // sempre "Bearer" -> o cliente usa isso pra montar o header Authorization
}
