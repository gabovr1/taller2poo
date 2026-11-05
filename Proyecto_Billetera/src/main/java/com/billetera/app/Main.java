package com.billetera.app;
import com.billetera.generic.*;
import com.billetera.model.*;
import com.billetera.service.*;
import com.billetera.util.*;
import java.util.*;
import java.util.function.*;
import java.util.logging.Logger;

public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    public static void main(String[] args) {
        Cuenta cuenta = new CuentaPremium("123", 2000000);
        cuenta.depositar(500000);
        cuenta.getMovimientos().add(new Movimiento("M1", 50000, TipoMovimiento.DEPOSITO));
        cuenta.getMovimientos().add(new Movimiento("M2", 150000, TipoMovimiento.RETIRO));

        // PRUEBA DE PATRONES Y ERRORES (try-catch-finally)
        CuentaService cs = new CuentaService();
        try {
            LOGGER.info("INFO: Intentando retiro");
            cs.realizarRetiro(cuenta, 1000000, true); // Va a pasar por el Proxy y el Chain
            LOGGER.info("INFO: Retiro exitoso. Saldo restante: " + cuenta.getSaldo());
        } catch (Exception e) {
            LOGGER.severe("ERROR (SEVERE): Fallo la transaccion -> " + e.getMessage());
        } finally {
            LOGGER.info("INFO: Cierre de conexion / Bloque Finally ejecutado.");
        }

        // STREAMS (filter, map, sorted, reduce, sum, average) Y FUNCIONALES
        Predicate<Movimiento> esRetiro = m -> m.getTipo() == TipoMovimiento.RETIRO;
        Function<Movimiento, Double> getM = Movimiento::getMonto;
        Consumer<Movimiento> print = m -> System.out.println(m.getId());
        Supplier<String> sup = () -> "Calculo completado";
        
        System.out.println("--- Streams ---");
        double sum = cuenta.getMovimientos().stream()
                .filter(esRetiro)
                .map(m -> m)
                .sorted(Comparator.comparing(Movimiento::getMonto))
                .mapToDouble(getM::apply)
                .sum(); // Primera reduccion
        System.out.println("Suma Retiros: " + sum);

        double promedioRetiros = cuenta.getMovimientos().stream()
                .filter(esRetiro)
                .mapToDouble(getM::apply)
                .average() // Segunda reduccion
                .orElse(0.0);
        System.out.println("Promedio de retiros: " + promedioRetiros + " - " + sup.get());

        // ORDENAMIENTOS (Comparable propio, algortimo manual, y Comparator compuesto y lambda)
        List<Movimiento> movs = new ArrayList<>(cuenta.getMovimientos());
        Ordenador.ordenar(movs); // Algoritmo Burbuja Generico manual sobre Comparable
        System.out.println("Ordenado manual, primer monto: " + movs.get(0).getMonto());
        
        List<Cuenta> cuentas = Arrays.asList(cuenta, new CuentaBasica("111", 500));
        cuentas.sort(Comparator.comparing(Cuenta::getSaldo).thenComparing(Cuenta::getNumero).reversed());
        
        Comparator<Cuenta> comparadorLambda = (c1, c2) -> Double.compare(c1.getSaldo(), c2.getSaldo());
        cuentas.sort(comparadorLambda); // Requisito: Lambda
        
        // WILDCARDS Y GENERICOS
        WildcardUtil.contarElementos(cuentas);
        Utilidades.imprimirElemento("Fin del programa generico estatico");
    }
}