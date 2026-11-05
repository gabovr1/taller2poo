package com.billetera.generic;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
public class RepositorioMemoria<T> implements Repositorio<T> {
    private List<T> datos = new ArrayList<>();
    @Override 
    public void guardar(T e) {
        datos.add(e); 
    }

    @Override 
    public List<T> obtenerTodos() { 
        return datos; 
    }

    @Override 
    public Optional<T> buscarPorId(String id) {
        return datos.isEmpty() ? Optional.empty() : Optional.of(datos.get(0)); 
    }

}