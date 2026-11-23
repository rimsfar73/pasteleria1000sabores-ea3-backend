package cl.milsabores.api.catalogo.repository;

import cl.milsabores.api.catalogo.entity.Categoria;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, String> {}
