package com.APImaratona.Maratona.Repository.Neo4j;

import com.APImaratona.Maratona.Model.ProblemaNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.List;

public interface ProblemaNodeRepository extends Neo4jRepository<ProblemaNode, String> {
    // Nao precisa dessa query pq ja é mapeada a relacao inversa no node
    // @Query("MATCH (u:UsuarioGrafo)-[:RESOLVEU]->(p:ProblemaGrafo {idCodeforces: $idProblema}) RETURN u")
    // List<UsuarioNode> encontrarUsuariosQueResolveram(String idProblema);

    // Para quem já tem histórico
    @Query("MATCH (eu:Usuario {nomeUsuario: $nomeUsuario})-[:RESOLVEU]->(pComum:Problema)<-[:RESOLVEU]-(outro:Usuario)-[:RESOLVEU]->(pRecomendado:Problema) " +
            "WHERE NOT (eu)-[:RESOLVEU]->(pRecomendado) " +
            "  AND pRecomendado.rating >= $ratingMin " +
            "  AND pRecomendado.rating <= $ratingMax " +
            "WITH pRecomendado.idProblema AS idProblema, COUNT(outro) AS frequencia " +
            "ORDER BY frequencia DESC " +
            "LIMIT $limite " +
            "RETURN idProblema")
    List<String> recomendarPorSimilaridade(String nomeUsuario, Integer ratingMin, Integer ratingMax, Integer limite);

    // Pega os mais resolvidos por todos na faixa de rating
    @Query("MATCH (pRecomendado:Problema)<-[:RESOLVEU]-(outros:Usuario) " +
            "WHERE pRecomendado.rating >= $ratingMin " +
            "  AND pRecomendado.rating <= $ratingMax " +
            "  AND NOT (:Usuario {nomeUsuario: $nomeUsuario})-[:RESOLVEU]->(pRecomendado) " +
            "WITH pRecomendado.idProblema AS idProblema, COUNT(outros) AS frequencia " +
            "ORDER BY frequencia DESC " +
            "LIMIT $limite " +
            "RETURN idProblema")
    List<String> recomendarPopularesPorRating(String nomeUsuario, Integer ratingMin, Integer ratingMax, Integer limite);

}
