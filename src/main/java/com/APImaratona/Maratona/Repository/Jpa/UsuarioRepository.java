package com.APImaratona.Maratona.Repository.Jpa;


import com.APImaratona.Maratona.Model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    boolean existsByEmail(String email);
    boolean existsByNome(String nome);
    boolean existsByNomeUsuario(String nomeUsuario);

    List<Usuario> findAllByNomeUsuarioIn(List<String> nomesUsuarios);

    Usuario findByEmail(String email);
    Usuario findByNomeUsuario(String nomeUsuario);
}
