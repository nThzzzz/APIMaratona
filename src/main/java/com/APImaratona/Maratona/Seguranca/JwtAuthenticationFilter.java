package com.APImaratona.Maratona.Seguranca;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

// Roda em TODO request, antes do controller. Se vier um header
// "Authorization: Bearer <token>" valido, marca no SecurityContext que esse
// request esta autenticado como aquele usuario. Se nao vier nada ou vier
// invalido, nao faz nada aqui -- so deixa o request seguir sem autenticacao,
// e quem decide se a rota exige login ou nao e o SecurityConfig.
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final SecurityContextRepository securityContextRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            if (jwtService.tokenValido(token)) {
                String nomeUsuario = jwtService.extrairNomeUsuario(token);

                // Autenticacao minima: sabemos quem e, sem roles/permissoes (fora de escopo aqui).
                var autenticacao = new UsernamePasswordAuthenticationToken(
                        nomeUsuario, null, Collections.emptyList());

                SecurityContext contexto = SecurityContextHolder.createEmptyContext();
                contexto.setAuthentication(autenticacao);
                SecurityContextHolder.setContext(contexto);

                // Com sessao STATELESS o SecurityContextHolderFilter usa um repositorio
                // (RequestAttributeSecurityContextRepository) que carrega o contexto sob demanda;
                // sem salvar aqui, filtros seguintes (ex: AnonymousAuthenticationFilter) recarregam
                // um contexto vazio e sobrescrevem a autenticacao que acabamos de setar.
                securityContextRepository.saveContext(contexto, request, response);
            }
        }

        filterChain.doFilter(request, response);
    }
}
