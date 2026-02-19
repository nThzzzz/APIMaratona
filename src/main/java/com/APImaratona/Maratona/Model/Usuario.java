package com.APImaratona.Maratona.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "tb_usuarios")
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

    public Usuario() {}

    public Usuario(long id, String nome, String email, String senha, String nomeUsuario, Time time) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.nomeUsuario = nomeUsuario;
        this.time = time;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Time getTime() {
        return time;
    }

    public void setTime(Time time) {
        this.time = time;
    }
}
