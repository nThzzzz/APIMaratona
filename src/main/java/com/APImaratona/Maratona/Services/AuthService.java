package com.APImaratona.Maratona.Services;

import com.APImaratona.Maratona.DTO.Usuario.LoginResponse;
import com.APImaratona.Maratona.Exceptions.AutenticacaoInvalidaException;
import com.APImaratona.Maratona.Model.Usuario;
import com.APImaratona.Maratona.Repository.Jpa.UsuarioRepository;
import com.APImaratona.Maratona.Seguranca.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginResponse login(LoginRequest dto) {
        Usuario u = usuarioRepo.findByNomeUsuario(dto.getNomeUsuario());

        boolean senhaCorreta;
        try {
            // matches(senhaDigitada, hashSalvo) -> refaz o hash da senha digitada e compara
            senhaCorreta = (u != null) && passwordEncoder.matches(dto.getSenha(), u.getSenha());
        } catch (IllegalArgumentException e) {
            // valor salvo nao e um hash BCrypt valido (ex: linha antiga em texto puro) -> login errado
            senhaCorreta = false;
        }

        if (!senhaCorreta) {
            // Mensagem generica de proposito: nao revela se o usuario existe ou nao.
            throw new AutenticacaoInvalidaException("Usuário ou senha inválidos");
        }

        String token = jwtService.gerarToken(u.getNomeUsuario());
        return new LoginResponse(token, "Bearer");
    }
}
