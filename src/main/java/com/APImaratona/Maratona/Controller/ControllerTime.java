package com.APImaratona.Maratona.Controller;

import com.APImaratona.Maratona.DTO.Time.TimeRequisicaoDTO;
import com.APImaratona.Maratona.DTO.Time.TimeResponseDTO;
import com.APImaratona.Maratona.Services.TimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class ControllerTime {
    private final TimeService timeService;

    @PostMapping("/cadastroTime")
    public String cadastroTime(@RequestBody TimeRequisicaoDTO dto){
        timeService.cadastrarTime(dto);
        return "Time: " + dto.getNomeTime() + ", cadastrado com sucesso";
    }

    @GetMapping("/listarTimes")
    public List<TimeResponseDTO> listarTimes(){
        return timeService.listarTimes();
    }

    @GetMapping("/buscarTime")
    public TimeResponseDTO buscarTimes(@RequestParam String nome){
        return timeService.buscarTime(nome);
    }

    @PutMapping("/adicionarUsuario")
    public String adicionarUsuarioAoTime(@RequestBody TimeRequisicaoDTO dto){
        timeService.adicionarUsuarioNoTime(dto);
        return "Usuario(s): " + dto.getNomesUsuarios() + " adicionado(s) com sucesso ao Time: " + dto.getNomeTime();
    }

    @PutMapping("/removerUsuario")
    public String removerUsuario(@RequestBody TimeRequisicaoDTO dto){
        timeService.removerUsuarioNoTime(dto);
        return "Usuario(s): " + dto.getNomesUsuarios() + " removido(s) com sucesso do Time: " + dto.getNomeTime();
    }

    @DeleteMapping("/excluirTime")
    public String excluirTime(@RequestParam String nome){
        timeService.excluirTime(nome);
        return "time excluido com sucesso";
    }

}
