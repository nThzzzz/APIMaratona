package com.APImaratona.Maratona.Repository.Mongo;

import com.APImaratona.Maratona.Model.Problema;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProblemaRepository extends MongoRepository<Problema, String> {
    boolean existsByIdProblema(String idProblema);

    Problema findByIdProblema(String idProblema);
}
