package com.APImaratona.Maratona.Repository;


import com.APImaratona.Maratona.Model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

}
