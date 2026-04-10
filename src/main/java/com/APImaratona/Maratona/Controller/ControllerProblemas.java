package com.APImaratona.Maratona.Controller;

import com.APImaratona.Maratona.Model.Problema;
import com.APImaratona.Maratona.Services.ProblemasService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class ControllerProblemas {
    private final ProblemasService problemasService;

    @GetMapping("/{idProblema}")
    public Problema buscarProblema(@PathVariable String idProblema){
        return problemasService.buscarProblema(idProblema);
    }

}
