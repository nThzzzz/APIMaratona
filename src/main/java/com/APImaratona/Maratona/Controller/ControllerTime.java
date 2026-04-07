package com.APImaratona.Maratona.Controller;

import com.APImaratona.Maratona.DTO.CadastroTimeRequisicaoDTO;
import com.APImaratona.Maratona.Services.TimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class ControllerTime {
    private final TimeService timeService;

    @PostMapping("/cadastroTime")
    public String cadastroTime(@RequestBody CadastroTimeRequisicaoDTO dto){
        timeService.cadastrarTime(dto);
        return "Time: " + dto.getNomeTime() + ", cadastrado com sucesso";
    }

//    @GetMapping("/listarTimes")
//    public String listarTimes(CadastroTimeRequisicaoDTO dto){
//
//    }
//
//    @GetMapping
//    public String listarTimes(CadastroTimeRequisicaoDTO dto){
//
//    }

}
