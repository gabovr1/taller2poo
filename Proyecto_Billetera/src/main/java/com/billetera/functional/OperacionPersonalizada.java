package com.billetera.functional;
import com.billetera.model.Cuenta;
@FunctionalInterface
public interface OperacionPersonalizada {
    void ejecutar(Cuenta cuenta);
}