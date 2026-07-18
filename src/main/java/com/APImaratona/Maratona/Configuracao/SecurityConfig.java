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

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable) // API stateless com token, sem cookie/sessao de browser
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    // por enquanto so essas duas rotas exigem token valido
                    .requestMatchers(HttpMethod.PUT, "/editarUsuario/**").authenticated()
                    .requestMatchers(HttpMethod.DELETE, "/excluirUsuario").authenticated()
                    // tudo o mais (cadastro, listagens, times, problemas) continua aberto --
                    // proteger o resto fica para uma proxima etapa, fora do escopo atual
                    .anyRequest().permitAll()
            )
            .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
