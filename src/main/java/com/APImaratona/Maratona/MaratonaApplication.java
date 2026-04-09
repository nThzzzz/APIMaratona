package com.APImaratona.Maratona;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.sql.DataSource;

@SpringBootApplication
public class MaratonaApplication {

    //TODO: DEPOIS DE FUNCIONAL REFATORAR AS EXECPTIONS PARA RETORNAR O STATUS DA APLICACAO
	public static void main(String[] args) {
		SpringApplication.run(MaratonaApplication.class, args);
	}

}
