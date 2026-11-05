package com.billetera.service;
import com.billetera.model.Movimiento;
import java.util.List;
import java.util.Optional;
import java.util.Comparator;
public class ReporteService {
    public double total(List<Movimiento> movimientos) {
        return movimientos.stream().mapToDouble(Movimiento::getMonto).sum();
    }
    public Optional<Movimiento> buscarMayor(List<Movimiento> movimientos) {
        return movimientos.stream().max(Comparator.comparing(Movimiento::getMonto));
    }
}