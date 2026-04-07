package com.APImaratona.Maratona.Repository;

import com.APImaratona.Maratona.Model.Time;
import org.springframework.data.domain.Example;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimeRepository extends JpaRepository<Time, Long> {
    public Time findByNome(String nome); // metodos de busca por atributo é findBy + Atributo
    public boolean existsByNome(String nome);
}
