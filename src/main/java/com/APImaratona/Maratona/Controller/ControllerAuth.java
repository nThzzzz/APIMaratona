package com.APImaratona.Maratona.Controller;

import com.APImaratona.Maratona.DTO.Usuario.LoginRequisicaoDTO;
import com.APImaratona.Maratona.DTO.Usuario.LoginResponseDTO;
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
    public LoginResponseDTO login(@RequestBody LoginRequisicaoDTO dto) {
        return authService.login(dto);
    }
}
