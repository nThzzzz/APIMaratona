package com.APImaratona.Maratona.Seguranca;

import com.APImaratona.Maratona.DTO.ErrorResponseDTO;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limita quantas vezes um mesmo IP pode chamar as duas rotas que nao exigem token:
 * /auth/login (alvo natural de forca bruta) e /cadastro (cada cadastro dispara uma
 * sincronizacao com o Codeforces, entao repeticao sai caro).
 *
 * O contador e uma janela fixa em memoria. Isso significa, e vale estar escrito:
 *   - nao e distribuido -- com mais de uma instancia, cada uma tem o proprio contador;
 *   - zera quando a aplicacao reinicia;
 *   - a janela e fixa, nao deslizante, entao e possivel concentrar o dobro do limite
 *     na virada de duas janelas.
 * Para valer de verdade em producao, isso viraria um contador no Redis (que ja esta
 * no projeto) ou um proxy na frente. Como mitigacao de forca bruta somada ao BCrypt,
 * que ja e lento de proposito, resolve o caso que existe hoje.
 */
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    @Value("${rate-limit.login:30}")
    private int limiteLogin;

    @Value("${rate-limit.cadastro:10}")
    private int limiteCadastro;

    @Value("${rate-limit.janela-segundos:60}")
    private long janelaSegundos;

    // Acima disso o mapa e varrido, para um ataque com IPs variados nao virar
    // vazamento de memoria.
    private static final int TAMANHO_MAXIMO = 10_000;

    private final Map<String, Janela> janelas = new ConcurrentHashMap<>();

    private static final class Janela {
        long inicio;
        int chamadas;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        int limite = limiteDaRota(request);

        if (limite > 0 && excedeu(request.getRequestURI() + "|" + request.getRemoteAddr(), limite)) {
            responderExcedido(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    /** 0 quando a rota nao e limitada. */
    private int limiteDaRota(HttpServletRequest request) {
        if (!"POST".equals(request.getMethod())) {
            return 0;
        }

        String caminho = request.getRequestURI();

        if ("/auth/login".equals(caminho)) {
            return limiteLogin;
        }

        if ("/cadastro".equals(caminho)) {
            return limiteCadastro;
        }

        return 0;
    }

    private boolean excedeu(String chave, int limite) {
        long agora = System.currentTimeMillis();
        long janelaMs = janelaSegundos * 1000;

        if (janelas.size() > TAMANHO_MAXIMO) {
            janelas.entrySet().removeIf(e -> agora - e.getValue().inicio >= janelaMs);
        }

        // compute() e atomico por chave: sem ele, duas requisicoes simultaneas do mesmo
        // IP poderiam ler o mesmo contador e gravar por cima uma da outra.
        Janela janela = janelas.compute(chave, (k, atual) -> {
            if (atual == null || agora - atual.inicio >= janelaMs) {
                Janela nova = new Janela();
                nova.inicio = agora;
                nova.chamadas = 1;
                return nova;
            }

            atual.chamadas++;
            return atual;
        });

        return janela.chamadas > limite;
    }

    // Mesmo formato de erro do resto da API. Assim como o JwtAuthenticationEntryPoint,
    // isso acontece antes do controller, entao nao passa pelo GlobalExceptionHandler.
    private void responderExcedido(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        // Sem o charset explicito o getWriter() usa o padrao da plataforma (ISO-8859-1) e
        // os acentos da mensagem chegam corrompidos no cliente.
        response.setContentType("application/json;charset=UTF-8");

        ErrorResponseDTO erro = new ErrorResponseDTO(
                LocalDateTime.now(),
                429,
                "Muitas requisições",
                "Limite de requisições excedido para este endereço. Tente novamente em instantes."
        );

        response.getWriter().write(objectMapper.writeValueAsString(erro));
    }
}
