package com.APImaratona.Maratona.Configuracao;

import com.APImaratona.Maratona.Seguranca.JwtAuthenticationEntryPoint;
import com.APImaratona.Maratona.Seguranca.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

// Wiring central de seguranca: define o que exige login (authorizeHttpRequests),
// registra o filtro de JWT (passo antes do UsernamePasswordAuthenticationFilter padrao
// do Spring) e o encoder de senha usado em todo o app.
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    // BCrypt: funcao de hash de senha de mao unica, com "sal" aleatorio embutido em
    // cada hash gerado -- por isso a mesma senha gera hashes diferentes toda vez,
    // mas passwordEncoder.matches(senhaDigitada, hashSalvo) ainda reconhece a senha certa.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Com sessao STATELESS o Spring Security carrega o SecurityContext sob demanda a cada
    // filtro (nao fica em memoria entre eles); por isso precisa de um repositorio explicito
    // aqui, tambem injetado no JwtAuthenticationFilter, para que a autenticacao setada la
    // sobreviva ate o AuthorizationFilter (senao o AnonymousAuthenticationFilter recarrega
    // um contexto vazio e sobrescreve). Estatico de proposito: JwtAuthenticationFilter
    // depende deste bean no construtor, e SecurityConfig depende de JwtAuthenticationFilter
    // no seu -- um metodo @Bean nao-estatico criaria uma dependencia circular.
    @Bean
    public static SecurityContextRepository securityContextRepository() {
        return new RequestAttributeSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityContextRepository securityContextRepository) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable) // API stateless com token, sem cookie/sessao de browser
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .securityContext(sc -> sc.securityContextRepository(securityContextRepository))
            .authorizeHttpRequests(auth -> auth
                    // Rotas que exigem token valido. Atencao ao /**: as rotas de usuario tem
                    // segmentos depois do prefixo (ex: /excluirUsuario/{nomeUsuario}/email) e um
                    // matcher sem /** NAO casa com elas -- cairiam no permitAll la embaixo e o
                    // 401 so apareceria (ou nao) la dentro do service.
                    .requestMatchers(HttpMethod.PUT, "/editarUsuario/**").authenticated()
                    .requestMatchers(HttpMethod.DELETE, "/excluirUsuario/**").authenticated()
                    .requestMatchers(HttpMethod.POST, "/cadastroTime").authenticated()
                    .requestMatchers(HttpMethod.PUT, "/adicionarUsuario").authenticated()
                    .requestMatchers(HttpMethod.PUT, "/removerUsuario").authenticated()
                    .requestMatchers(HttpMethod.PUT, "/editarTime/**").authenticated()
                    .requestMatchers(HttpMethod.DELETE, "/excluirTime").authenticated()
                    // tudo o mais (cadastro, login, listagens e consultas de problemas) continua
                    // aberto -- proteger o resto fica para uma proxima etapa
                    .anyRequest().permitAll()
            )
            .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
