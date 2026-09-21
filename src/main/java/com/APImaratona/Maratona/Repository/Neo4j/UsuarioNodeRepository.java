package com.APImaratona.Maratona.Repository.Neo4j;

import com.APImaratona.Maratona.Model.UsuarioNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

public interface UsuarioNodeRepository extends Neo4jRepository<UsuarioNode, String> {
    @Query("MERGE (u:Usuario {nomeUsuario: $nomeUsuario}) " +
            // So o idProblema identifica o no. Com o rating dentro do MERGE, um problema
            // sincronizado antes sem rating (a API do Codeforces omite, e o int vira 0) e
            // depois com o rating real viraria DOIS nos com o mesmo idProblema, e a
            // contagem de frequencia das recomendacoes passaria a somar errado.
            "MERGE (p:Problema {idProblema: $idProblema}) " +
            "SET p.rating = $rating " +
            "MERGE (u)-[:RESOLVEU]->(p) " +
            "RETURN u")
    void registrarResolucao(String nomeUsuario, String idProblema, int rating);

    @Query("MATCH (u:Usuario {nomeUsuario: $nomeAntigo}) SET u.nomeUsuario = $nomeNovo")
    void atualizarNomeUsuarioNode(String nomeAntigo, String nomeNovo);
}
