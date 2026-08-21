package com.APImaratona.Maratona.Controller;

import com.APImaratona.Maratona.DTO.Time.CriarTimeRequest;
import com.APImaratona.Maratona.DTO.Time.TimeRequest;
import com.APImaratona.Maratona.DTO.Time.TimeResponse;
import com.APImaratona.Maratona.Services.TimeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class ControllerTime {
    private final TimeService timeService;

    @PostMapping("/cadastroTime")
    public String cadastroTime(@RequestBody CriarTimeRequest dto, Authentication authentication){
        timeService.cadastrarTime(dto, authentication.getName());
        return "Time: " + dto.getNomeTime() + ", cadastrado com sucesso";
    }

    @GetMapping("/listarTimes")
    public List<TimeResponse> listarTimes(){
        return timeService.listarTimes();
    }

    @GetMapping("/buscarTime")
    public TimeResponse buscarTimes(@RequestParam String nome){
        return timeService.buscarTime(nome);
    }

    @PutMapping("/adicionarUsuario")
    public String adicionarUsuarioAoTime(@RequestBody CriarTimeRequest dto, Authentication authentication){
        timeService.adicionarUsuarioNoTime(dto, authentication.getName());
        return "Usuario(s): " + dto.getNomesUsuarios() + " adicionado(s) com sucesso ao Time: " + dto.getNomeTime();
    }

    @PutMapping("/removerUsuario")
    public String removerUsuario(@RequestBody CriarTimeRequest dto, Authentication authentication){
        timeService.removerUsuarioNoTime(dto, authentication.getName());
        return "Usuario(s): " + dto.getNomesUsuarios() + " removido(s) com sucesso do Time: " + dto.getNomeTime();
    }

    @PutMapping("/editarTime/{nomeTime}/nome")
    public String editarNomeTime(@PathVariable String nomeTime, @Valid @RequestBody TimeRequest.AlterarNomeTime dto,
                                 Authentication authentication){
        timeService.editarNomeTime(nomeTime, dto, authentication.getName());
        return "Time: " + nomeTime + ", nome alterado com sucesso";
    }

    @PutMapping("/editarTime/{nomeTime}/capitao")
    public String transferirCapitania(@PathVariable String nomeTime, @Valid @RequestBody TimeRequest.TransferirCapitania dto,
                                      Authentication authentication){
        timeService.transferirCapitania(nomeTime, dto, authentication.getName());
        return "Time: " + nomeTime + ", capitania transferida para: " + dto.nomeCapitaoNovo();
    }

    @DeleteMapping("/excluirTime")
    public String excluirTime(@RequestParam String nome, Authentication authentication){
        timeService.excluirTime(nome, authentication.getName());
        return "time excluido com sucesso";
    }

}
