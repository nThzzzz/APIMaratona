package com.APImaratona.Maratona.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "tb_usuarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String nome;

    @Column (unique = true)
    private String email;

    @ToString.Exclude // senha nunca deve aparecer em log
    private String senha;

    @Column (unique = true)
    private String nomeUsuario;
    private String rank;
    private int rating;

    @ManyToOne
    @JoinColumn(name = "id_time")
    @JsonIgnore
    private Time time;

}
