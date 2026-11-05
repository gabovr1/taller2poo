package com.billetera.model;
public class CuentaBasica extends Cuenta {
    public CuentaBasica(String n, double s) { super(n, s); }
    @Override public double limiteDiario() { return 2000000; }
}