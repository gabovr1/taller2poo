package com.billetera.generic;
import java.util.List;
import java.util.Optional;
public interface Repositorio<T> {
    void guardar(T entidad);
    List<T> obtenerTodos();
    Optional<T> buscarPorId(String id);
}