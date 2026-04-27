package com.APImaratona.Maratona.Repository.Neo4j;

import com.APImaratona.Maratona.Model.ProblemaNode;
import com.APImaratona.Maratona.Model.Usuario;
import com.APImaratona.Maratona.Model.UsuarioNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.List;

public interface ProblemaNodeRepository extends Neo4jRepository<ProblemaNode, String> {
    // Nao precisa dessa query pq ja é mapeada a relacao inversa no node
    //@Query("MATCH (u:UsuarioGrafo)-[:RESOLVEU]->(p:ProblemaGrafo {idCodeforces: $idProblema}) RETURN u")
    //List<UsuarioNode> encontrarUsuariosQueResolveram(String idProblema);

//    @Query("")
//    List<ProblemaNode> recomendacaoComBaseTime(String nomeUsuario, int rating);
}
