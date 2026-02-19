package com.APImaratona.Maratona.Model;

import java.util.List;

import jakarta.persistence.*;

@Entity
@Table(name = "tb_times")
public class Time {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    // 1:N, um time tem muitos usuarios
    @OneToMany(mappedBy = "time", cascade = CascadeType.ALL)
    private List<Usuario> usuarios;
    private String nome;

    public Time() {}

    public Time(long id, List<Usuario> usuarios, String nome) {
        this.id = id;
        this.usuarios = usuarios;
        this.nome = nome;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public List<Usuario> getPessoas() {
        return usuarios;
    }

    public void setPessoas(List<Usuario> usuarios) {
        this.usuarios = usuarios;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }
}
