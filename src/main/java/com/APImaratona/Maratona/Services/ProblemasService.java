package com.APImaratona.Maratona.Services;

import com.APImaratona.Maratona.DTO.Codeforces.CodeforcesSubmissionResponse;
import com.APImaratona.Maratona.DTO.Usuario.UsuarioResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProblemasService {
    private final ProblemaRepository problemaRepository;
    private final ProblemaNodeRepository problemaNodeRepository;
    private final UsuarioNodeRepository usuarioNodeRepository;
    private final UsuarioRepository usuarioRepository;

    @CacheEvict(value = "cacheTodosProblemas", allEntries = true)
    public void cadastrarProblema(CodeforcesSubmissionResponse submissao, String nomeUsuario) {
        String idProblema = submissao.getProblem().getContestId() + submissao.getProblem().getIndex();
        List<String> tags = submissao.getProblem().getTags();

        try {
            usuarioNodeRepository.registrarResolucao(nomeUsuario, idProblema, submissao.getProblem().getRating());
        }catch (Exception e){
            log.info("Erro: {}", e.getMessage());
        }
        log.info("Problema: {} relcionado com sucesso", idProblema);

        if(problemaRepository.existsByIdProblema(idProblema)){
            log.info("Problema ja cadastrado: {} com as tags: {}", idProblema, tags);
            return;
        }

        Problema problema = extrairTexto(submissao);
        problemaRepository.save(problema);
        log.info("Problema cadastrado com sucesso: {} com as tags: {}", idProblema, tags);
    }

    public Problema buscarProblema(String idProblema){
        Problema problema = problemaRepository.findByIdProblema(idProblema);

        // Sem isso o findBy devolve null e a rota responde 200 com corpo vazio.
        if(problema == null){
            throw new EntidadeNaoEcontrada("Problema: " + idProblema + ", não cadastrado");
        }

        return problema;
    }

    @Cacheable(value = "cacheUsuariosProblema", key = "#idProblema")
    @Transactional(value = "neo4jTransactionManager", readOnly = true)
    public List<UsuarioResponse> usuariosFizeramProblema(String idProblema) {
        List<UsuarioResponse> usuariosDTO = new ArrayList<>();

        if(!problemaRepository.existsByIdProblema(idProblema)){
            throw new EntidadeNaoEcontrada("Problema: "+ idProblema +", não cadastrado");
        }

        Optional<ProblemaNode> problemaNode = problemaNodeRepository.findById(idProblema);

        if (problemaNode.isEmpty()) {
            return usuariosDTO; // Retorna vazio se não tiver no grafo
        }

        Set<UsuarioNode> usuariosNodes = problemaNode.get().getUsuariosResolveram();

        List<String> nomesUsuarios = new ArrayList<>();
        for(UsuarioNode uNode : usuariosNodes) {
            nomesUsuarios.add(uNode.getNomeUsuario());
        }

        // Uma consulta em lote no lugar de uma por usuario. Quem esta no grafo mas nao
        // no Postgres simplesmente nao volta, que era o que o if de null tratava antes.
        for(Usuario usuario : usuarioRepository.findAllByNomeUsuarioIn(nomesUsuarios)) {
            usuariosDTO.add(UsuarioResponse.fromEntity(usuario));
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

        if (problemas.isEmpty()) {
            return listaProblemas; // Retorna vazio se o usuario ainda nao tem nada no grafo
        }

        Set<ProblemaNode> problemasResolvidos = problemas.get().getProblemasResolvidos();

        List<String> idsProblemas = new ArrayList<>();
        for(ProblemaNode pb : problemasResolvidos){
            idsProblemas.add(pb.getIdProblema());
        }

        // Uma consulta em lote no lugar de uma por problema. Como idProblema e o _id do
        // documento, o findAllById resolve direto -- e o que estiver so no grafo, sem ter
        // sido salvo no Mongo, apenas nao volta.
        return problemaRepository.findAllById(idsProblemas);
    }

    // Paginado: o catalogo vem do Codeforces e cresce sem teto. A chave do cache passa
    // a incluir a paginacao, entao cada pagina e cacheada separadamente.
    @Cacheable(value = "cacheTodosProblemas")
    public Page<Problema> listarProblemas(Pageable paginacao){
        return problemaRepository.findAll(paginacao);
    }

    private Problema extrairTexto(CodeforcesSubmissionResponse submissao){
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
        Usuario usuario = buscarUsuarioValidado(nomeUsuario);
        int maxRating = usuario.getRating()+400;
        int minRating = usuario.getRating()-400;
        int limite = 5;

        List<String> nomeProblemas = problemaNodeRepository.recomendarPopularesPorRating(nomeUsuario, minRating, maxRating, limite);

        return buscarProblemasPorId(nomeProblemas);
    }

    public List<Problema> recomendarPorSimilaridade(String nomeUsuario){
        Usuario usuario = buscarUsuarioValidado(nomeUsuario);
        int maxRating = usuario.getRating()+400;
        int minRating = usuario.getRating()-400;
        int limite = 5;

        List<String> nomeProblemas = problemaNodeRepository.recomendarPorSimilaridade(nomeUsuario, minRating, maxRating, limite);

        return buscarProblemasPorId(nomeProblemas);
    }

    private Usuario buscarUsuarioValidado(String nomeUsuario){
        if(!usuarioRepository.existsByNomeUsuario(nomeUsuario)){
            throw new EntidadeNaoEcontrada("Usuário: " + nomeUsuario + ", não encontrado");
        }

        return usuarioRepository.findByNomeUsuario(nomeUsuario);
    }

    private List<Problema> buscarProblemasPorId(List<String> idsProblemas){
        // Busca em lote, mas o findAllById nao garante a ordem -- e aqui a ordem E o
        // ranking da recomendacao, entao a lista e remontada seguindo os ids originais.
        Map<String, Problema> porId = new HashMap<>();
        for(Problema p : problemaRepository.findAllById(idsProblemas)){
            porId.put(p.getIdProblema(), p);
        }

        List<Problema> problemas = new ArrayList<>();
        for(String id : idsProblemas){
            Problema problema = porId.get(id);

            // A recomendacao pode apontar para um problema ausente no Mongo.
            if(problema != null){
                problemas.add(problema);
            }
        }

        return problemas;
    }
}
