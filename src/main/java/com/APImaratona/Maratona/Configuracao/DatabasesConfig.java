package com.APImaratona.Maratona.Configuracao;

import jakarta.persistence.EntityManagerFactory;
import org.neo4j.driver.Driver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;

@Configuration
@EnableJpaRepositories(basePackages = "com.APImaratona.Maratona.Repository.Jpa")
@EnableMongoRepositories(basePackages = "com.APImaratona.Maratona.Repository.Mongo")
@EnableNeo4jRepositories(
        basePackages = "com.APImaratona.Maratona.Repository.Neo4j",
        transactionManagerRef = "neo4jTransactionManager"
)
public class DatabasesConfig {
    @Primary
    @Bean(name = "transactionManager")
    public JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    @Bean(name = "neo4jTransactionManager")
    public Neo4jTransactionManager neo4jTransactionManager(Driver driver) {
        return new Neo4jTransactionManager(driver);
    }
}