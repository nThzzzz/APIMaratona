package com.APImaratona.Maratona.Configuracao;

import com.APImaratona.Maratona.Seguranca.JwtAuthenticationEntryPoint;
import com.APImaratona.Maratona.Seguranca.JwtAuthenticationFilter;
import com.APImaratona.Maratona.Seguranca.RateLimitFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
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
    private final RateLimitFilter rateLimitFilter;

    // O Spring Boot registra automaticamente qualquer bean do tipo Filter no container
    // de servlets. Como o RateLimitFilter e posicionado a mao na cadeia de seguranca
    // logo abaixo, esse registro paralelo e desligado para ele existir num lugar so.
    @Bean
    public FilterRegistrationBean<RateLimitFilter> registroRateLimitDesligado(RateLimitFilter filtro) {
        FilterRegistrationBean<RateLimitFilter> registro = new FilterRegistrationBean<>(filtro);
        registro.setEnabled(false);
        return registro;
    }

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

    // Sem isto o navegador barra qualquer chamada de um front hospedado em outra origem: a
    // requisicao ate chega na API, mas a resposta e descartada por falta do cabecalho, e o
    // preflight OPTIONS nem passa pelo deny by default abaixo. As origens sao configuraveis
    // porque o endereco do front muda entre dev e deploy; o padrao e o Vite local.
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${cors.origens:http://localhost:5173}") List<String> origens) {

        CorsConfiguration configuracao = new CorsConfiguration();
        configuracao.setAllowedOrigins(origens);
        configuracao.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuracao.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        // Sem credenciais: a sessao e o proprio token no header, nao ha cookie a compartilhar.
        configuracao.setAllowCredentials(false);
        configuracao.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
        fonte.registerCorsConfiguration("/**", configuracao);

        return fonte;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityContextRepository securityContextRepository) throws Exception {
        http
            .cors(Customizer.withDefaults()) // usa o corsConfigurationSource acima
            .csrf(AbstractHttpConfigurer::disable) // API stateless com token, sem cookie/sessao de browser
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .securityContext(sc -> sc.securityContextRepository(securityContextRepository))
            // Deny by default: o que nao estiver listado abaixo exige token. A postura
            // anterior era a inversa (permitAll no final, protegidas por excecao) e falhava
            // do lado errado -- esquecer um matcher deixava a rota ABERTA em silencio, o que
            // ja aconteceu duas vezes com matchers sem o /**. Aqui o esquecimento fecha a
            // rota: chato, mas aparece na primeira chamada em vez de virar brecha.
            .authorizeHttpRequests(auth -> auth
                    // Entrada no sistema: sem elas ninguem consegue obter um token.
                    .requestMatchers(HttpMethod.POST, "/auth/login", "/cadastro").permitAll()

                    // Consultas de leitura, abertas de proposito -- os dados sao publicos
                    // (ranking, times e problemas do Codeforces) e nao expoem senha.
                    .requestMatchers(HttpMethod.GET,
                            "/listaUsuarios",
                            "/buscarUsuario/**",
                            "/listarTimes",
                            "/buscarTime",
                            "/listarProblemas",
                            "/buscarProblema/**",
                            "/usuariosFizeramProblema/**",
                            "/problemasFeitorPor/**",
                            "/recomendarProblemaRating/**",
                            "/recomendarProblemaSimilaridade/**").permitAll()

                    // Health check: precisa responder sem credencial para o compose e
                    // qualquer orquestrador conseguirem saber se a aplicacao subiu.
                    .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()

                    // Escrita de conta e de time, e qualquer rota nova que ninguem lembrou
                    // de classificar.
                    .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
            // Os dois sao ancorados no UsernamePasswordAuthenticationFilter porque o
            // addFilterBefore so aceita como referencia um filtro que o Spring Security
            // conheca -- filtro proprio nao tem ordem registrada. Barrar aqui ja evita
            // gastar BCrypt e ida ao banco com requisicao acima do limite.
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
