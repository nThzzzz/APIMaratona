package com.APImaratona.Maratona;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.time.Duration;

@SpringBootApplication
@EnableAsync
@EnableCaching
public class MaratonaApplication {

	public static void main(String[] args) {
		SpringApplication.run(MaratonaApplication.class, args);
	}

	// Com timeout porque o /cadastro chama o Codeforces de forma sincrona: sem limite, uma
	// indisponibilidade la segurava a requisicao de cadastro indefinidamente, e o cliente
	// nao tinha como saber se ainda valia esperar.
	@Bean
	public RestTemplate restTemplate() {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(Duration.ofSeconds(5));
		factory.setReadTimeout(Duration.ofSeconds(10));

		return new RestTemplate(factory);
	}
}
