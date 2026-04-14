package com.APImaratona.Maratona.Model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.HashSet;
import java.util.Set;

@Node("Usuario")
@Data
public class UsuarioNode {

    @Id
    private String nomeUsuario;

    // Relacao de frente pra tras, OUTGOING
    @Relationship(type = "RESOLVEU", direction = Relationship.Direction.OUTGOING)
    @EqualsAndHashCode.Exclude  // tipo o json ignore
    @ToString.Exclude // tipo o json ignore
    private Set<ProblemaNode> problemasResolvidos = new HashSet<>();

}
