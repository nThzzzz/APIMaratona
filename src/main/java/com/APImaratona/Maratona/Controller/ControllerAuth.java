package com.APImaratona.Maratona.Controller;

import com.APImaratona.Maratona.DTO.Usuario.LoginResponse;
import com.APImaratona.Maratona.DTO.Usuario.UsuarioRequest;
import com.APImaratona.Maratona.Services.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class ControllerAuth {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(@RequestBody UsuarioRequest.Login dto) {
        return authService.login(dto);
    }
}
