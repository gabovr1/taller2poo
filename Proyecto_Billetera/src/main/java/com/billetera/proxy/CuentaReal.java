package com.billetera.proxy;
import com.billetera.model.Cuenta;
import com.billetera.exception.BilleteraException;
import com.billetera.chain.*;
public class CuentaReal implements OperacionCuenta {
    private Cuenta cuenta;
    private Validador cadenaValidacion;
    public CuentaReal(Cuenta c) {
        this.cuenta = c;
        Validador saldo = new ValidadorSaldo();
        Validador limite = new ValidadorLimite();
        Validador fraude = new ValidadorFraude();
        saldo.setSiguiente(limite); limite.setSiguiente(fraude);
        this.cadenaValidacion = saldo;
    }
    @Override
    public void retirar(double monto) throws BilleteraException {
        cadenaValidacion.validar(cuenta, monto); // Uso del Chain of Responsibility
        cuenta.setSaldo(cuenta.getSaldo() - monto);
    }
}