package com.APImaratona.Maratona.Repository.Neo4j;

import com.APImaratona.Maratona.Model.UsuarioNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.List;

public interface UsuarioNodeRepository extends Neo4jRepository<UsuarioNode, String> {
    @Query("MERGE (u:Usuario {nomeUsuario: $nomeUsuario}) " +
            "MERGE (p:Problema {idProblema: $idProblema}) " +
            "MERGE (u)-[:RESOLVEU]->(p)" +
            "RETURN u")
    void registrarResolucao(String nomeUsuario, String idProblema);
}
