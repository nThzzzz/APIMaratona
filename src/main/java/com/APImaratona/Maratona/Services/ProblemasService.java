package com.APImaratona.Maratona.Services;

import com.APImaratona.Maratona.DTO.Codeforces.CodeforcesSubmissionDTO;
import com.APImaratona.Maratona.Model.Problema;
import com.APImaratona.Maratona.Repository.Mongo.ProblemaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j // Para usar o log.info em vez do print
@Service
@RequiredArgsConstructor
public class ProblemasService {
    private final ProblemaRepository problemaRepository;

    public void cadastrarProblema(CodeforcesSubmissionDTO submissao) {
        String idProblema = submissao.getProblem().getContestId() + submissao.getProblem().getIndex();
        List<String> tags = submissao.getProblem().getTags();

        // verifica se o probleja ja existe
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

    private Problema extrairTexto(CodeforcesSubmissionDTO submissao){
        Problema problema = new Problema();

        String idProblema = submissao.getProblem().getContestId() + submissao.getProblem().getIndex();
        List<String> tags = submissao.getProblem().getTags();

        problema.setIdProblema(idProblema);
        problema.setTags(tags);

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
                    .timeout(10000)
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
}
