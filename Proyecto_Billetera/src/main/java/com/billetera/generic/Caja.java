package com.billetera.generic;
public class Caja<T> {
    private T valor;
    public Caja(T v) {
        this.valor = v; 
    }

    public T getValor() { 
        return valor; 
    }
}