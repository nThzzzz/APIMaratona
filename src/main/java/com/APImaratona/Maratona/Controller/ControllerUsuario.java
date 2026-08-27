package com.APImaratona.Maratona.Controller;

import com.APImaratona.Maratona.DTO.Usuario.*;
import com.APImaratona.Maratona.Services.CodeforcesService;
import com.APImaratona.Maratona.Services.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class ControllerUsuario {

    private final UsuarioService usuarioService;
    private final CodeforcesService codeforcesService;

    @PostMapping("/cadastro")
    public String cadastroUsuario(@Valid @RequestBody UsuarioRequest.CadastrarUsuario dto){
        usuarioService.cadastrarUsuario(dto);
        codeforcesService.sincronizarPerfilCodeforces(dto.nomeUsuario());
        return "Usuario cadastrado com sucesso";
    }

    // O Pageable e resolvido pelo Spring a partir de ?page=&size=&sort= (padrao: 20 itens).
    @GetMapping("/listaUsuarios")
    public Page<UsuarioResponse> listaUsuarios(Pageable paginacao){
        return usuarioService.listarUsuarios(paginacao);
    }

    @GetMapping("/buscarUsuario/{nomeUsuario}")
    public UsuarioResponse mostraUsuario(@PathVariable String nomeUsuario) {
        return usuarioService.buscarUsuarioNome(nomeUsuario);
    }

    // Quem e o dono do token. Sem isto o cliente precisa decodificar o JWT so para
    // descobrir o proprio nome de usuario e so entao chamar /buscarUsuario -- ou seja,
    // fica acoplado ao formato do token para uma informacao que a API ja tem.
    @GetMapping("/me")
    public UsuarioResponse meuPerfil(Authentication authentication) {
        return usuarioService.buscarUsuarioNome(authentication.getName());
    }

    // "Authentication authentication" e resolvido automaticamente pelo Spring a partir
    // do token JWT validado no JwtAuthenticationFilter -- nao precisa de anotacao extra.
    @PutMapping("/editarUsuario/credenciais/{nomeUsuario}/nomeUsuario")
    public LoginResponse editarUsuarioNomeUsuario(@PathVariable String nomeUsuario,@Valid @RequestBody UsuarioRequest.AlterarNomeUsuario dto,
                                 Authentication authentication){
        return usuarioService.editarNomeUsuario(nomeUsuario, dto, authentication.getName());
    }

    @PutMapping("/editarUsuario/credenciais/{nomeUsuario}/email")
    public String editarUsuarioEmail(@PathVariable String nomeUsuario,@Valid @RequestBody UsuarioRequest.AlterarEmail dto,
                                     Authentication authentication){
        usuarioService.editarEmailUsuario(nomeUsuario, dto, authentication.getName());
        return "Usuário: " + nomeUsuario + ", email alterado com sucesso";
    }

    @PutMapping("/editarUsuario/credenciais/{nomeUsuario}/senha")
    public String editarUsuarioSenha(@PathVariable String nomeUsuario,@Valid @RequestBody UsuarioRequest.AlterarSenha dto,
                                     Authentication authentication){
        usuarioService.editarSenhaUsuario(nomeUsuario, dto, authentication.getName());
        return "Usuário: " + nomeUsuario + ", senha alterada com sucesso";
    }

    @PutMapping("/editarUsuario/perfil/{nomeUsuario}/nome")
    public String editarPerfilNome(@PathVariable String nomeUsuario, @Valid @RequestBody UsuarioRequest.AlterarNome dto,
                                   Authentication authentication){
        usuarioService.editarPerfilNome(nomeUsuario, dto, authentication.getName());
        return "Usuário: " + nomeUsuario + ", nome alterado com sucesso";
    }

    @DeleteMapping("/excluirUsuario/{nomeUsuario}/email")
    public String excluirUsuarioEmail(@PathVariable String nomeUsuario, @Valid @RequestBody UsuarioRequest.ExcluirUsuarioEmail dto,
                                 Authentication authentication){
        usuarioService.excluirUsuarioEmail(nomeUsuario, dto, authentication.getName());
        return "Usuário: " + nomeUsuario + ", deletado com sucesso";
    }

    @DeleteMapping("/excluirUsuario/{nomeUsuario}/nomeUsuario")
    public String excluirUsuarioNomeUsuario(@PathVariable String nomeUsuario, @Valid @RequestBody UsuarioRequest.ExcluirUsuarioNomeUsuario dto, Authentication authentication){
        usuarioService.excluirUsuarioNomeUsuario(nomeUsuario, dto, authentication.getName());
        return "Usuário: " + nomeUsuario + ", deletado com sucesso";
    }
}
