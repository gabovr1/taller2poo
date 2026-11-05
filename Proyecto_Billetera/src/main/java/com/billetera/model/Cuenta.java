package com.billetera.model;
import java.util.ArrayList;
import java.util.List;
public abstract class Cuenta {
    protected String numero;
    protected double saldo;
    protected List<Movimiento> movimientos;
    protected List<Bolsillo> bolsillos;
    
    public Cuenta(String numero, double saldo) {
        this.numero = numero; this.saldo = saldo;
        this.movimientos = new ArrayList<>();
        this.bolsillos = new ArrayList<>();
    }
    public String getNumero() { return numero; }
    public double getSaldo() { return saldo; }
    public List<Movimiento> getMovimientos() { return movimientos; }
    public void depositar(double monto) { this.saldo += monto; }
    public void setSaldo(double saldo) { this.saldo = saldo; }
    public abstract double limiteDiario();
}