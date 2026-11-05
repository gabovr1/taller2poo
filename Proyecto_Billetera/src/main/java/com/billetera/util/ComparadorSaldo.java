package com.billetera.util;
import com.billetera.model.Cuenta;
import java.util.Comparator;
public class ComparadorSaldo implements Comparator<Cuenta> {
    @Override 
    public int compare(Cuenta c1, Cuenta c2) { 
        return Double.compare(c1.getSaldo(), c2.getSaldo()); 
    }
}