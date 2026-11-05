package com.billetera.chain;
import com.billetera.exception.*;
import com.billetera.model.Cuenta;
public class ValidadorLimite extends Validador {
    @Override public void validar(Cuenta c, double m) throws BilleteraException {
        if(m > c.limiteDiario()) throw new LimiteDiarioException("Supera limite de cuenta.");
        procesarSiguiente(c, m);
    }
}