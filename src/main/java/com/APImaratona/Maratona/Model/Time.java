package com.APImaratona.Maratona.Model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_times")
@Data
@AllArgsConstructor
public class Time {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    // Sem CascadeType.REMOVE de proposito: excluir o time nao apaga os usuarios,
    // eles apenas ficam sem time.
    @OneToMany(mappedBy = "time", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<Usuario> usuarios;

    @OneToOne
    @JoinColumn(name = "capitao_id")
    private Usuario capitao;

    @Column (unique = true)
    private String nome;

    public Time(){
        this.nome = null;
        this.usuarios = new ArrayList<>();
    }

}
