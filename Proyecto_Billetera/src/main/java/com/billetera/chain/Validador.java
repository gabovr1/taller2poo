package com.billetera.chain;
import com.billetera.exception.BilleteraException;
import com.billetera.model.Cuenta;
public abstract class Validador {
    protected Validador siguiente;
    public void setSiguiente(Validador sig) { this.siguiente = sig; }
    public abstract void validar(Cuenta cuenta, double monto) throws BilleteraException;
    protected void procesarSiguiente(Cuenta c, double m) throws BilleteraException {
        if(siguiente != null) siguiente.validar(c, m);
    }
}