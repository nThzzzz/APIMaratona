package com.APImaratona.Maratona.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
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

    // Fora de equals/hashCode/toString: Time aponta de volta para ca (usuarios e
    // capitao) e percorrer os dois lados entra em recursao infinita -- mesmo cuidado
    // que UsuarioNode/ProblemaNode ja tomam. Excluir este lado ja quebra todo ciclo,
    // porque e a unica aresta que sai do Usuario.
    @ManyToOne
    @JoinColumn(name = "id_time")
    @JsonIgnore
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Time time;

}
