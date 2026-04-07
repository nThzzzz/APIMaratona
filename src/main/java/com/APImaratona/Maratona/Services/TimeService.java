package com.APImaratona.Maratona.Services;

import com.APImaratona.Maratona.DTO.CadastroTimeRequisicaoDTO;
import com.APImaratona.Maratona.Model.Time;
import com.APImaratona.Maratona.Model.Usuario;
import com.APImaratona.Maratona.Repository.TimeRepository;
import com.APImaratona.Maratona.Repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TimeService {
    private final TimeRepository timeRepo;
    private final UsuarioRepository usuarioRepo;

    public void cadastrarTime(CadastroTimeRequisicaoDTO dto){
        // Fazer verificacao e tratamento

        Time time = new Time();

        if(dto.getNomeTime()==null){
            throw new RuntimeException("Nome do time NULL");
        }

        time.setNome(dto.getNomeTime());

        if(dto.getNomesUsuarios().size()>3){
            throw new RuntimeException("Lista de membros do time maior que o permitido, tamanho= " + dto.getNomesUsuarios().size());
        }

        if(!dto.getNomesUsuarios().isEmpty()) {
            List<Usuario> usuarios = usuarioRepo.findAllByNomeUsuarioIn(dto.getNomesUsuarios());
            time.setUsuarios(usuarios);

            for(Usuario u : usuarios){
                u.setTime(time);
            }
        }

        timeRepo.save(time);
    }
}
