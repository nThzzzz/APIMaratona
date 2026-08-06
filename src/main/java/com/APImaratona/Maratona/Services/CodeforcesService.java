package com.APImaratona.Maratona.Services;

import com.APImaratona.Maratona.DTO.Codeforces.CodeforcesResponse;
import com.APImaratona.Maratona.DTO.Codeforces.CodeforcesSubmissionResponse;
import com.APImaratona.Maratona.DTO.Codeforces.CodeforcesUserInfoResponse;
import com.APImaratona.Maratona.DTO.Codeforces.CodeforcesUserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j // Para usar o log.info em vez do print
@Service
@RequiredArgsConstructor
public class CodeforcesService {

    private final RestTemplate restTemplate;
    private final ProblemasService problemasService;

    @Async
    public void sincronizarPerfilCodeforces(String nomeUsuarioCodeforces) {
        log.info("Iniciando a sincronização para: {}", nomeUsuarioCodeforces);
        String url = "https://codeforces.com/api/user.status?handle=" + nomeUsuarioCodeforces;

        try {
            CodeforcesResponse resposta = restTemplate.getForObject(url, CodeforcesResponse.class);

            if (resposta != null && "OK".equals(resposta.getStatus())) {
                for (CodeforcesSubmissionResponse submissao : resposta.getResult()) {
                    if ("OK".equals(submissao.getVerdict())) {
                        String idProblema = submissao.getProblem().getContestId() + submissao.getProblem().getIndex();
                        List<String> tags = submissao.getProblem().getTags();
                        log.info("Problema resolvido encontrado: {} com as tags: {}", idProblema, tags);

                        // Salva o problema no mongo e faz a relacao no neo4j
                        // TODO (se eu estiver muito afim): usar uma API pra burlar o cloudflare
                        problemasService.cadastrarProblema(submissao, nomeUsuarioCodeforces);

                    }
                }
                log.info("Sincronização concluída para: {}", nomeUsuarioCodeforces);
            }
        } catch (Exception e) {
            log.error("Erro ao comunicar com a API do Codeforces para o utilizador: {}", nomeUsuarioCodeforces, e);
        }
    }

    public CodeforcesUserInfoResponse infoPerfilUsuario(String nomeUsuarioCodeForces){
        CodeforcesUserInfoResponse cfUsuario = new CodeforcesUserInfoResponse();

        String url = "https://codeforces.com/api/user.info?handles=" + nomeUsuarioCodeForces;

        try {
            log.info("Adquirindo informações do usuário {}", nomeUsuarioCodeForces);
            CodeforcesUserResponse resposta = restTemplate.getForObject(url, CodeforcesUserResponse.class);

            if(resposta != null && "OK".equals(resposta.getStatus())){
                cfUsuario = resposta.getResult().get(0);
            }

        } catch (Exception e) {
            log.error("Erro ao comunicar com a API do Codeforces para o utilizador: {}", nomeUsuarioCodeForces, e);
        }

        return cfUsuario;
    }

}