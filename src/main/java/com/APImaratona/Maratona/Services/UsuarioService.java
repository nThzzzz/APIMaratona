package com.APImaratona.Maratona.Services;

import com.APImaratona.Maratona.Repository.TimeRepository;
import com.APImaratona.Maratona.Repository.UsuarioRepository;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor // pra nao usar o @Autwired, lombok faz
public class UsuarioService {

    private final UsuarioRepository usuarioRepo;
    private final TimeRepository timeRepo;



}
