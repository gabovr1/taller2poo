package com.billetera.util;
import java.util.List;
public class Utilidades {
    // CUMPLE REQUISITO: 3 metodos genericos estaticos
    public static <T> void imprimirElemento(T elemento) { 
        System.out.println(elemento); 
    }

    public static <T> boolean esNulo(T elemento) { 
        return elemento == null; 
    }

    public static <T> T obtenerPrimero(List<T> lista) { 
        return lista.isEmpty() ? null : lista.get(0); 
    }
    
}