package com.APImaratona.Maratona.Services;

import com.APImaratona.Maratona.DTO.Codeforces.CodeforcesSubmissionDTO;
import com.APImaratona.Maratona.DTO.Usuario.UsuarioResponseDTO;
import com.APImaratona.Maratona.Exceptions.EntidadeNaoEcontrada;
import com.APImaratona.Maratona.Model.Problema;
import com.APImaratona.Maratona.Model.ProblemaNode;
import com.APImaratona.Maratona.Model.Usuario;
import com.APImaratona.Maratona.Model.UsuarioNode;
import com.APImaratona.Maratona.Repository.Jpa.UsuarioRepository;
import com.APImaratona.Maratona.Repository.Mongo.ProblemaRepository;
import com.APImaratona.Maratona.Repository.Neo4j.ProblemaNodeRepository;
import com.APImaratona.Maratona.Repository.Neo4j.UsuarioNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j // Para usar o log.info em vez do print
@Service
@RequiredArgsConstructor
public class ProblemasService {
    private final ProblemaRepository problemaRepository;
    private final ProblemaNodeRepository problemaNodeRepository;
    private final UsuarioNodeRepository usuarioNodeRepository;
    private final UsuarioRepository usuarioRepository;

    @CacheEvict(value = "cacheTodosProblemas", allEntries = true)
    public void cadastrarProblema(CodeforcesSubmissionDTO submissao, String nomeUsuario) {
        String idProblema = submissao.getProblem().getContestId() + submissao.getProblem().getIndex();
        List<String> tags = submissao.getProblem().getTags();

        try {
            usuarioNodeRepository.registrarResolucao(nomeUsuario, idProblema, submissao.getProblem().getRating());
        }catch (Exception e){
            log.info("Erro: {}", e.getMessage());
        }
        log.info("Problema: {} relcionado com sucesso", idProblema);

        // verifica se o problema ja existe
        if(problemaRepository.existsByIdProblema(idProblema)){
            log.info("Problema ja cadastrado: {} com as tags: {}", idProblema, tags);
            return;
        }

        Problema problema = extrairTexto(submissao);
        problemaRepository.save(problema);
        log.info("Problema cadastrado com sucesso: {} com as tags: {}", idProblema, tags);
    }

    public Problema buscarProblema(String idProblema){
        return problemaRepository.findByIdProblema(idProblema);
    }

    @Cacheable(value = "cacheUsuariosProblema", key = "#idProblema")
    @Transactional(value = "neo4jTransactionManager", readOnly = true)
    public List<UsuarioResponseDTO> usuariosFizeramProblema(String idProblema) {
        List<UsuarioResponseDTO> usuariosDTO = new ArrayList<>();

        if(!problemaRepository.existsByIdProblema(idProblema)){
            throw new EntidadeNaoEcontrada("Problema: "+ idProblema +", não cadastrado");
        }

        Optional<ProblemaNode> problemaNode = problemaNodeRepository.findById(idProblema);

        if (problemaNode.isEmpty()) {
            return usuariosDTO; // Retorna vazio se não tiver no grafo
        }

        Set<UsuarioNode> usuariosNodes = problemaNode.get().getUsuariosResolveram();

        for(UsuarioNode uNode : usuariosNodes) {
            Usuario usuario = usuarioRepository.findByNomeUsuario(uNode.getNomeUsuario());

            if (usuario != null) {
                UsuarioResponseDTO dto = new UsuarioResponseDTO();
                dto.setNome(usuario.getNome());
                dto.setEmail(usuario.getEmail());
                dto.setNomeUsuario(usuario.getNomeUsuario());
                dto.setNomeTime(usuario.getTime() != null ? usuario.getTime().getNome() : "Sem time");
                dto.setRating(usuario.getRating());
                dto.setRank(usuario.getRank());

                usuariosDTO.add(dto);
            }
        }

        return usuariosDTO;
    }

    @Cacheable(value = "cacheProblemasUsuario", key = "#nomeUsuario")
    @Transactional(value = "neo4jTransactionManager", readOnly = true)
    public List<Problema> problemasFeitosPor(String nomeUsuario){
        List<Problema> listaProblemas = new ArrayList<>();

        if(!usuarioRepository.existsByNomeUsuario(nomeUsuario)){
            throw new EntidadeNaoEcontrada("Usuário: " + nomeUsuario + ", não encontrado");
        }

        Optional<UsuarioNode> problemas = usuarioNodeRepository.findById(nomeUsuario);

        Set<ProblemaNode> problemasResolvidos = problemas.get().getProblemasResolvidos();

        for(ProblemaNode pb : problemasResolvidos){
            Problema problemaFull = problemaRepository.findByIdProblema(pb.getIdProblema());
            listaProblemas.add(problemaFull);
        }

        return listaProblemas;
    }

    @Cacheable(value = "cacheTodosProblemas")
    public List<Problema> listarProblemas(){
        return problemaRepository.findAll();
    }

    private Problema extrairTexto(CodeforcesSubmissionDTO submissao){
        Problema problema = new Problema();

        String idProblema = submissao.getProblem().getContestId() + submissao.getProblem().getIndex();
        List<String> tags = submissao.getProblem().getTags();

        problema.setIdProblema(idProblema);
        problema.setTags(tags);

        int rating = submissao.getProblem().getRating();
        problema.setRating(rating);

        String contestId = idProblema.replaceAll("[^0-9]", "");
        String index = idProblema.replaceAll("[0-9]", "");

        String url = "https://codeforces.com/problemset/problem/" + contestId + "/" + index;

        log.info("Conectando no Codeforces na URL: {}", url);

        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept-Language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                    .header("Connection", "keep-alive")
                    .timeout(1000)
                    .get();

            Element problemStatement = doc.selectFirst("div.problem-statement");
            Element titleElement = doc.selectFirst("div.header .title");

            // Se a página for lida corretamente
            problema.setNome(titleElement != null ? titleElement.text() : submissao.getProblem().getName());
            problema.setDescricao(problemStatement != null ? problemStatement.outerHtml() : "<p>Texto indisponível</p>");

            log.info("Scraping concluído com sucesso para o problema: {}", problema.getNome());

        } catch (Exception e) {
            log.warn("Web Scraping bloqueado (403) para o problema {}. Salvando com dados padrão.", idProblema);

            // Se der 403 preenche com dados básicos em vez de travar a sincronização de todos os outros problemas.
            problema.setNome(submissao.getProblem().getName() != null ? submissao.getProblem().getName() : idProblema);
            problema.setDescricao("<p>Texto indisponível devido a bloqueio de segurança do Codeforces (Erro 403).</p>");
        }

        return problema;
    }


    public List<Problema> recomendarProblemasComBaseRating(String nomeUsuario){
        Usuario usuario = usuarioRepository.findByNomeUsuario(nomeUsuario);
        List<Problema> problemas = new ArrayList<>();
        int maxRating, minRating, limite;
        maxRating = usuario.getRating()+400;
        minRating = usuario.getRating()-400;
        limite = 5;

        List<String> nomeProblemas = problemaNodeRepository.recomendarPopularesPorRating(nomeUsuario, minRating, maxRating, limite);

        for(String nome : nomeProblemas){
            problemas.add(problemaRepository.findByIdProblema(nome));
        }

        return problemas;
    }

    public List<Problema> recomendarPorSimilaridade(String nomeUsuario){
        Usuario usuario = usuarioRepository.findByNomeUsuario(nomeUsuario);
        List<Problema> problemas = new ArrayList<>();
        int maxRating, minRating, limite;
        maxRating = usuario.getRating()+400;
        minRating = usuario.getRating()-400;
        limite = 5;

        List<String> nomeProblemas = problemaNodeRepository.recomendarPorSimilaridade(nomeUsuario, minRating, maxRating, limite);

        for(String nome : nomeProblemas){
            problemas.add(problemaRepository.findByIdProblema(nome));
        }

        return problemas;
    }
}
