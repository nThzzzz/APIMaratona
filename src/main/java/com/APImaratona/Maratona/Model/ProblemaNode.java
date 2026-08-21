package com.APImaratona.Maratona.Model;


import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.HashSet;
import java.util.Set;

@Node("Problema")
@Data
public class ProblemaNode {

    @Id
    private String idProblema;
    private int rating;


    // Fora de equals/hashCode/toString: UsuarioNode aponta de volta para ca e percorrer
    // os dois lados entraria em recursao infinita.
    @Relationship(type = "RESOLVEU", direction = Relationship.Direction.INCOMING)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<UsuarioNode> usuariosResolveram = new HashSet<>();
}