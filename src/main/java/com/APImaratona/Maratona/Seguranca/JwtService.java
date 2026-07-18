package com.APImaratona.Maratona.Seguranca;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

// Responsavel por criar e validar o token JWT.
// Um JWT tem 3 partes separadas por ponto: quem-e (subject) + quando-expira, mais uma
// assinatura calculada com uma chave secreta que so o servidor conhece. Se alguem alterar
// o conteudo do token sem saber a chave, a assinatura nao bate mais e o token e rejeitado.
@Component
public class JwtService {

    @Value("${jwt.secret}")
    private String secretBase64;

    @Value("${jwt.expiracao-ms}")
    private long expiracaoMs;

    // Deriva a chave de assinatura a partir do segredo configurado (JWT_SECRET) em Base64.
    private SecretKey chave() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretBase64));
    }

    // Gera um token novo para um usuario que acabou de logar com sucesso.
    public String gerarToken(String nomeUsuario) {
        Date agora = new Date();
        Date expira = new Date(agora.getTime() + expiracaoMs);

        return Jwts.builder()
                .subject(nomeUsuario)      // "de quem" e esse token
                .issuedAt(agora)
                .expiration(expira)
                .signWith(chave())         // assina com a chave secreta -> ninguem consegue forjar
                .compact();
    }

    // Extrai o nomeUsuario de dentro de um token (assumindo que ja foi validado por tokenValido).
    public String extrairNomeUsuario(String token) {
        return Jwts.parser()
                .verifyWith(chave())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    // Confere assinatura + validade (expiracao) sem deixar nenhuma excecao escapar.
    public boolean tokenValido(String token) {
        try {
            Jwts.parser().verifyWith(chave()).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false; // token expirado, malformado, assinatura errada, nulo/vazio etc.
        }
    }
}
