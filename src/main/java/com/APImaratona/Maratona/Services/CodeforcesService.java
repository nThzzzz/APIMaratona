package com.APImaratona.Maratona.Services;

import com.APImaratona.Maratona.DTO.Codeforces.CodeforcesResponse;
import com.APImaratona.Maratona.DTO.Codeforces.CodeforcesSubmissionResponse;
import com.APImaratona.Maratona.DTO.Codeforces.CodeforcesUserInfoResponse;
import com.APImaratona.Maratona.DTO.Codeforces.CodeforcesUserResponse;
import com.APImaratona.Maratona.Exceptions.RegraDeNegocio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodeforcesService {

    private final RestTemplate restTemplate;
    private final ProblemasService problemasService;
    private final CacheManager cacheManager;

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
        } finally {
            invalidarCachesDoGrafo(nomeUsuarioCodeforces);
        }
    }

    /**
     * O cadastrarProblema so invalida o cacheTodosProblemas. Sem isto, uma leitura de
     * problemasFeitosPor feita ENQUANTO a sincronizacao roda grava uma lista vazia (ou
     * parcial) no cache, e ela fica servida por 60 minutos mesmo com os dados ja gravados
     * no grafo -- o cliente que acabou de se cadastrar via um perfil vazio e nao tinha
     * como forcar a atualizacao. Roda no finally porque uma sync que falhou no meio
     * tambem pode ter gravado parte dos problemas.
     */
    private void invalidarCachesDoGrafo(String nomeUsuario) {
        evictSeExistir("cacheProblemasUsuario", nomeUsuario);

        // Este e por chave de problema, e a sync mexeu em varios: limpa inteiro.
        Cache usuariosProblema = cacheManager.getCache("cacheUsuariosProblema");
        if (usuariosProblema != null) {
            usuariosProblema.clear();
        }
    }

    private void evictSeExistir(String nomeCache, Object chave) {
        Cache cache = cacheManager.getCache(nomeCache);
        if (cache != null) {
            cache.evict(chave);
        }
    }

    /**
     * Le rank e rating do handle. Handle inexistente e erro de infraestrutura sao tratados
     * de forma diferente de proposito: o Codeforces responde 400 para handle que nao existe,
     * e isso e erro do usuario, entao propaga e barra o cadastro. Qualquer outra falha
     * (rede, 5xx, timeout) e problema deles, nao dele -- ali segue com rank/rating vazios,
     * porque recusar cadastro por indisponibilidade de terceiro seria pior.
     *
     * Antes deste desvio o catch engolia tudo e gravava rank=null, rating=0, o que deixava
     * a conta permanentemente sem dados e indistinguivel de uma sync ainda em andamento.
     */
    public CodeforcesUserInfoResponse infoPerfilUsuario(String nomeUsuarioCodeForces){
        CodeforcesUserInfoResponse cfUsuario = new CodeforcesUserInfoResponse();

        String url = "https://codeforces.com/api/user.info?handles=" + nomeUsuarioCodeForces;

        try {
            log.info("Adquirindo informações do usuário {}", nomeUsuarioCodeForces);
            CodeforcesUserResponse resposta = restTemplate.getForObject(url, CodeforcesUserResponse.class);

            if(resposta != null && "OK".equals(resposta.getStatus())){
                cfUsuario = resposta.getResult().get(0);
            }

        } catch (HttpClientErrorException e) {
            log.warn("Handle inexistente no Codeforces: {}", nomeUsuarioCodeForces);
            throw new RegraDeNegocio("Nome de usuário: " + nomeUsuarioCodeForces +
                    ", não existe no Codeforces");
        } catch (Exception e) {
            log.error("Erro ao comunicar com a API do Codeforces para o utilizador: {}", nomeUsuarioCodeForces, e);
        }

        return cfUsuario;
    }

}
