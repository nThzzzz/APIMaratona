package com.APImaratona.Maratona.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private String email;
    private String senha;
    private String nomeUsuario;

    // N:1 varias pessoas pertencem a um time
    @ManyToOne
    @JoinColumn(name = "id_time")
    @JsonIgnore
    private Time time;

}
