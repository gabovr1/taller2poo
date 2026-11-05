package com.billetera.proxy;
import com.billetera.exception.BilleteraException;
public interface OperacionCuenta {
    void retirar(double monto) throws BilleteraException;
}