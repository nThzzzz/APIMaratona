package com.APImaratona.Maratona.Configuracao;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ConfigChecker {
    @Value("${spring.data.mongodb.uri:NOT_FOUND}")
    private String mongoUri;

    @PostConstruct
    public void check() {
        System.out.println("\n========== MONGO URI CARREGADA ==========");
        System.out.println(mongoUri);
        System.out.println("=========================================\n");
    }
}
