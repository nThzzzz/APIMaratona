package com.APImaratona.Maratona.Controller;

import com.APImaratona.Maratona.DTO.Usuario.UsuarioResponse;
import com.APImaratona.Maratona.Model.Problema;
import com.APImaratona.Maratona.Services.ProblemasService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@RestController
@AllArgsConstructor
public class ControllerProblemas {
    private final ProblemasService problemasService;

    // Prefixo explicito de proposito: como "/{idProblema}" na raiz, esta rota capturava
    // qualquer GET de um segmento so e engolia caminhos digitados errado.
    @GetMapping("/buscarProblema/{idProblema}")
    public Problema buscarProblema(@PathVariable String idProblema){
        return problemasService.buscarProblema(idProblema);
    }

    @GetMapping("/listarProblemas")
    public Page<Problema> listarProblemas(Pageable paginacao){
        return problemasService.listarProblemas(paginacao);
    }

    @GetMapping("/usuariosFizeramProblema/{idProblema}")
    public List<UsuarioResponse> usuariosFizeramPrblema(@PathVariable String idProblema){
        return problemasService.usuariosFizeramProblema(idProblema);
    }

    @GetMapping("/problemasFeitorPor/{nomeUsuario}")
    public List<Problema> problemasFeitosPor(@PathVariable String nomeUsuario){
        return problemasService.problemasFeitosPor(nomeUsuario);
    }

    @GetMapping("/recomendarProblemaSimilaridade/{nomeUsuario}")
    public List<Problema> recomendarProblemaSimilaridade(@PathVariable String nomeUsuario){
        return problemasService.recomendarPorSimilaridade(nomeUsuario);
    }

    @GetMapping("/recomendarProblemaRating/{nomeUsuario}")
    public List<Problema> recomendarProblemaRating(@PathVariable String nomeUsuario){
        return problemasService.recomendarProblemasComBaseRating(nomeUsuario);
    }
}
