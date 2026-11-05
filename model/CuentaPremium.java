package com.billetera.model;
public class CuentaPremium extends Cuenta {
    public CuentaPremium(String n, double s) { super(n, s); }
    @Override public double limiteDiario() { return 10000000; }
}