package com.billetera.service;
import com.billetera.model.Cuenta;
import com.billetera.proxy.*;
import com.billetera.exception.BilleteraException;
public class CuentaService {
    public void realizarRetiro(Cuenta cuenta, double monto, boolean auth) throws BilleteraException {
        CuentaReal real = new CuentaReal(cuenta);
        CuentaProxy proxy = new CuentaProxy(real, auth);
        proxy.retirar(monto);
    }
}