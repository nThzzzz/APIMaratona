package com.APImaratona.Maratona.Configuracao;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Confirma no boot que a MONGO_URI foi realmente carregada do ambiente.
@Slf4j
@Component
public class ConfigChecker {

    @Value("${spring.data.mongodb.uri:NOT_FOUND}")
    private String mongoUri;

    @PostConstruct
    public void check() {
        log.info("Mongo URI carregada: {}", mascararCredenciais(mongoUri));
    }

    // mongodb+srv://usuario:senha@cluster/db -> mongodb+srv://***:***@cluster/db
    // A URI traz usuario e senha do banco embutidos; logar ela crua vazaria as duas.
    private String mascararCredenciais(String uri) {
        return uri.replaceAll("://[^:/?#@]+:[^@/?#]+@", "://***:***@");
    }
}
