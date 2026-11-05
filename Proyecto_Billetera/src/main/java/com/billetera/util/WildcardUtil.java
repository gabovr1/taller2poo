package com.billetera.util;
import java.util.List;
import com.billetera.model.*;
public class WildcardUtil {

    public static void contarElementos(List<?> lista) { 
        System.out.println("Total elementos: " + lista.size());
    }
    
    public static double sumarMontoTotal(List<? extends Movimiento> lista) { 
        double suma = 0;
        for (Movimiento m : lista) suma += m.getMonto();
        return suma;
    }

    public static void agregarRetiro(List<? super Movimiento> lista, double monto) { 
        lista.add(new Movimiento("EXT", monto, TipoMovimiento.RETIRO));
    }
}