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

    // 1:N, um time tem muitos usuarios, se o time for excluido o usuario nao é
    @OneToMany(mappedBy = "time", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<Usuario> usuarios;

    @Column (unique = true)
    private String nome;

    public Time(){
        this.nome = null;
        this.usuarios = new ArrayList<>();
    }

}
