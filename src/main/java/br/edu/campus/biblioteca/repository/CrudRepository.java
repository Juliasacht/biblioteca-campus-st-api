package br.edu.campus.biblioteca.repository;

import java.util.List;
import java.util.Optional;

public interface CrudRepository<T> {
    List<T> findAll();
    Optional<T> findById(Long id);
    T save(T entidade);
    void deleteById(Long id);
    boolean existsById(Long id);
}
