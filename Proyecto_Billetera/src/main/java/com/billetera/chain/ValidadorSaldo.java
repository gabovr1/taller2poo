package com.billetera.chain;
import com.billetera.exception.*;
import com.billetera.model.Cuenta;
public class ValidadorSaldo extends Validador {
    @Override public void validar(Cuenta c, double m) throws BilleteraException {
        if(c.getSaldo() < m) throw new SaldoInsuficienteException("No hay fondos suficientes.");
        procesarSiguiente(c, m);
    }
}