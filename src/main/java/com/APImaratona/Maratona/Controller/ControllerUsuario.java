package com.APImaratona.Maratona.Controller;

import com.APImaratona.Maratona.DTO.CadastroRequisicaoDTO;
import com.APImaratona.Maratona.DTO.UsuarioResponseDTO;
import com.APImaratona.Maratona.Model.Usuario;
import com.APImaratona.Maratona.Services.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class ControllerUsuario {

    @GetMapping("/teste")
    public String teste(){
        return "teste";
    }

    private final UsuarioService usuarioService;

    @PostMapping("/cadastro")
    public String cadastroUsuario(@RequestBody CadastroRequisicaoDTO cr){
        usuarioService.cadastrarUsuario(cr);
        return "Usuario cadastrado com sucesso";
    }

    @GetMapping("/listaUsuarios")
    public List<UsuarioResponseDTO> listaUsuarios(){
        return usuarioService.listarUsuarios();
    }

    // RequestParam signifca que pode ser fornecido ou nao no caso de nada dispara uma exception
    @GetMapping("/buscarUsuario")
    public UsuarioResponseDTO mostraUsuario(@RequestParam(required = false) String nomeUsuario,
                                            @RequestParam(required = false) String email){
        if(nomeUsuario != null){
            return usuarioService.buscarUsuarioNome(nomeUsuario);
        }else if (email != null){
            return usuarioService.buscarUsuarioEmail(email);
        }

        throw new RuntimeException("Nenhum parametro fornecido");
    }

}
