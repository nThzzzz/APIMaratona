package com.APImaratona.Maratona;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;

@SpringBootApplication
@EnableAsync
@EnableCaching
public class MaratonaApplication {

    //TODO: DEPOIS DE FUNCIONAL REFATORAR AS EXECPTIONS PARA RETORNAR O STATUS DA APLICACAO
	public static void main(String[] args) {
		SpringApplication.run(MaratonaApplication.class, args);
	}

	@Bean // Para fazer requisições HTTP
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}
}
