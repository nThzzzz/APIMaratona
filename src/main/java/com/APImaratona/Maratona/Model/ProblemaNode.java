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

    // Olha a relacao de tras pra frente ICOMING
    @Relationship(type = "RESOLVEU", direction = Relationship.Direction.INCOMING)
    @EqualsAndHashCode.Exclude  // tipo o json ignore
    @ToString.Exclude // tipo o json ignore
    private Set<UsuarioNode> usuariosResolveram = new HashSet<>();
}