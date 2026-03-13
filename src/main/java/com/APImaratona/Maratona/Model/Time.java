package com.APImaratona.Maratona.Model;

import java.util.List;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_times")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Time {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    // 1:N, um time tem muitos usuarios
    @OneToMany(mappedBy = "time", cascade = CascadeType.ALL)
    private List<Usuario> usuarios;

    @Column (unique = true)
    private String nome;

}
