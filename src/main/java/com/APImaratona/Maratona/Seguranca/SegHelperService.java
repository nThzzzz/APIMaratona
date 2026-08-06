package com.APImaratona.Maratona.Seguranca;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Data
@AllArgsConstructor
@Service
public class SegHelperService {
    private final PasswordEncoder passwordEncoder;

    public boolean verificaSenha(String senhaCerto, String senha){
        try {
            return passwordEncoder.matches(senha, senhaCerto);
        } catch (IllegalArgumentException e) {
            return false; // hash salvo nao e um BCrypt valido (conta antiga em texto puro)
        }
    }

    public boolean saoMesmoUsuario(String nomeUsuario, String nomeUsuarioAutenticado){
        return nomeUsuario.equals(nomeUsuarioAutenticado);
    }

    public String encodarSenha(String senha){
        return passwordEncoder.encode(senha);
    }
}
