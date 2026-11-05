package com.billetera.model;
import java.time.LocalDateTime;
public class Movimiento implements Comparable<Movimiento> {
    private String id;
    private double monto;
    private TipoMovimiento tipo;
    private LocalDateTime fecha;
    public Movimiento(String id, double monto, TipoMovimiento tipo) {
        this.id = id; this.monto = monto; this.tipo = tipo; this.fecha = LocalDateTime.now();
    }
    public String getId() { return id; }
    public double getMonto() { return monto; }
    public TipoMovimiento getTipo() { return tipo; }
    public LocalDateTime getFecha() { return fecha; }
    
    @Override
    public int compareTo(Movimiento otro) {
        return Double.compare(this.monto, otro.monto);
    }
}
