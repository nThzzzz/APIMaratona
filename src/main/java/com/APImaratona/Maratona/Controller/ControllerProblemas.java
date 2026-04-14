package com.APImaratona.Maratona.Controller;

import com.APImaratona.Maratona.DTO.Usuario.UsuarioResponseDTO;
import com.APImaratona.Maratona.Model.Problema;
import com.APImaratona.Maratona.Services.ProblemasService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@AllArgsConstructor
public class ControllerProblemas {
    private final ProblemasService problemasService;

    @GetMapping("/{idProblema}")
    public Problema buscarProblema(@PathVariable String idProblema){
        return problemasService.buscarProblema(idProblema);
    }

    @GetMapping("/listarProblemas")
    public List<Problema> listarProblemas(){
        return problemasService.listarProblemas();
    }

    @GetMapping("/usuariosFizeramProblema/{idProblema}")
    public List<UsuarioResponseDTO> usuariosFizeramPrblema(@PathVariable String idProblema){
        return problemasService.usuariosFizeramProblema(idProblema);
    }

    @GetMapping("/problemasFeitorPor/{nomeUsuario}")
    public List<Problema> problemasFeitosPor(@PathVariable String nomeUsuario){
        return problemasService.problemasFeitosPor(nomeUsuario);
    }

}
