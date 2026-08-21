package com.APImaratona.Maratona.Repository.Jpa;

import com.APImaratona.Maratona.Model.Time;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimeRepository extends JpaRepository<Time, Long> {
    public Time findByNome(String nome);
    public boolean existsByNome(String nome);
}
