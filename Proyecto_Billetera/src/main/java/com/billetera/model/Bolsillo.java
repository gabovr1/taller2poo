package com.billetera.model;
public class Bolsillo {
    private String nombre;
    private double meta;
    private double saldoActual;
    public Bolsillo(String n, double m) { this.nombre = n; this.meta = m; this.saldoActual = 0; }
    public String getNombre() { return nombre; }
}