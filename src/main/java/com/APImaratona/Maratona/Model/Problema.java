package com.APImaratona.Maratona.Model;

// Tem que ser o @Id do Spring Data, nao o do JPA: o Mongo so reconhece este e, com o
// jakarta.persistence.Id, o idProblema virava campo comum e o _id era um ObjectId aleatorio.
import org.springframework.data.annotation.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.List;

// Serial para usar o redis
@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "problemas")
public class Problema implements Serializable {
    @Id
    private String idProblema;

    private String nome;
    private String descricao;
    private List<String> tags;
    private int rating;
}
