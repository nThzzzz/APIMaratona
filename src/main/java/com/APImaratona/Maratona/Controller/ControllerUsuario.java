package com.APImaratona.Maratona.Controller;

import com.APImaratona.Maratona.DTO.Usuario.EditarUsuarioRequisicaoDTO;
import com.APImaratona.Maratona.DTO.Usuario.ExcluirUsuarioRequisicaoDTO;
import com.APImaratona.Maratona.DTO.Usuario.UsuarioRequisicaoDTO;
import com.APImaratona.Maratona.DTO.Usuario.UsuarioResponseDTO;
import com.APImaratona.Maratona.Services.CodeforcesService;
import com.APImaratona.Maratona.Services.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
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
    private final CodeforcesService codeforcesService;

    @PostMapping("/cadastro")
    public String cadastroUsuario(@RequestBody UsuarioRequisicaoDTO cr){
        usuarioService.cadastrarUsuario(cr);
        codeforcesService.sincronizarPerfilCodeforces(cr.getNomeUsuario());
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

    // "Authentication authentication" e resolvido automaticamente pelo Spring a partir
    // do token JWT validado no JwtAuthenticationFilter -- nao precisa de anotacao extra.
    @PutMapping("/editarUsuario/{nomeUsuario}")
    public String editarUsuario(@PathVariable String nomeUsuario, @RequestBody EditarUsuarioRequisicaoDTO dto,
                                 Authentication authentication){
        String resultado = usuarioService.editarUsuario(nomeUsuario, dto, authentication.getName());
        return "Usuario: " + nomeUsuario + ", Modificacoes (" + resultado + ")";
    }

    @DeleteMapping("/excluirUsuario")
    public String excluirUsuario(@RequestBody ExcluirUsuarioRequisicaoDTO dto, Authentication authentication){
        usuarioService.excluirUsuario(dto, authentication.getName());
        return "Usuario excluido com sucesso";
    }

}
