package com.billetera.functional;

import com.billetera.model.Cuenta;
import com.billetera.model.Movimiento;
import com.billetera.model.TipoMovimiento;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class OperacionesFuncionales {

    // Comparator como lambda
    public static final Comparator<Movimiento> POR_MONTO_DESC =
            (a, b) -> Double.compare(b.getMonto(), a.getMonto());

    // Comparator compuesto: comparing + thenComparing + reversed
    public static final Comparator<Movimiento> POR_TIPO_Y_MONTO_DESC =
            Comparator.comparing(Movimiento::getTipo)
                    .thenComparing(Movimiento::getMonto)
                    .reversed();

    private OperacionesFuncionales() {
    }

    // ---------- Predicate ----------
    public static Predicate<Movimiento> esDelTipo(TipoMovimiento tipo) {
        return m -> m.getTipo() == tipo;
    }

    public static Predicate<Movimiento> montoMayorA(double minimo) {
        return m -> m.getMonto() > minimo;
    }

    public static Predicate<Movimiento> enPeriodo(LocalDateTime desde, LocalDateTime hasta) {
        return m -> !m.getFecha().isBefore(desde) && !m.getFecha().isAfter(hasta);
    }

    // ---------- Streams ----------

    // Stream 1: filter
    public static List<Movimiento> filtrar(List<Movimiento> movimientos, Predicate<Movimiento> condicion) {
        return movimientos.stream()
                .filter(condicion)
                .collect(Collectors.toList());
    }

    // Stream 2: map (con Function)
    public static List<Double> extraer(List<Movimiento> movimientos, Function<Movimiento, Double> extractor) {
        return movimientos.stream()
                .map(extractor)
                .collect(Collectors.toList());
    }

    // Stream 3: sorted (sin modificar la lista original)
    public static List<Movimiento> ordenar(List<Movimiento> movimientos, Comparator<Movimiento> criterio) {
        return movimientos.stream()
                .sorted(criterio)
                .collect(Collectors.toList());
    }

    // Stream 4: reduccion con sum
    public static double sumarMontos(List<Movimiento> movimientos) {
        return movimientos.stream()
                .mapToDouble(Movimiento::getMonto)
                .sum();
    }

    // Stream 5: reduccion con average
    public static double promediarMontos(List<Movimiento> movimientos) {
        return movimientos.stream()
                .mapToDouble(Movimiento::getMonto)
                .average()
                .orElse(0.0);
    }

    // Stream 6: reduccion con reduce
    public static double montoMaximo(List<Movimiento> movimientos) {
        return movimientos.stream()
                .map(Movimiento::getMonto)
                .reduce(Double::max)
                .orElse(0.0);
    }

    // Stream 7: groupingBy (reporte por tipo)
    public static Map<TipoMovimiento, Double> totalPorTipo(List<Movimiento> movimientos) {
        return movimientos.stream()
                .collect(Collectors.groupingBy(
                        Movimiento::getTipo,
                        () -> new EnumMap<>(TipoMovimiento.class),
                        Collectors.summingDouble(Movimiento::getMonto)));
    }

    // ---------- Optional en busquedas ----------
    public static Optional<Movimiento> buscarMovimiento(List<Movimiento> movimientos, String id) {
        return movimientos.stream()
                .filter(m -> m.getId().equals(id))
                .findFirst();
    }

    public static Optional<Cuenta> buscarCuenta(List<Cuenta> cuentas, String numero) {
        return cuentas.stream()
                .filter(c -> c.getNumero().equals(numero))
                .findFirst();
    }

    // ---------- Consumer ----------
    public static void recorrer(List<Movimiento> movimientos, Consumer<Movimiento> accion) {
        movimientos.forEach(accion);
    }

    // ---------- Supplier ----------
    public static Supplier<String> generadorDeIds(String prefijo) {
        AtomicInteger contador = new AtomicInteger(0);
        return () -> prefijo + contador.incrementAndGet();
    }

    // ---------- Interfaz funcional propia ----------
    public static void aplicar(Cuenta cuenta, OperacionPersonalizada operacion) {
        operacion.ejecutar(cuenta);
    }
}
