package com.APImaratona.Maratona.Model;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "problemas")
public class Problema {
    @Id
    private String idProblema;

    private String nome;
    private String descricao;
    private List<String> tags;
    private int rating;
}
