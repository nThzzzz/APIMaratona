package com.APImaratona.Maratona;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;

@SpringBootApplication
@EnableAsync
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
