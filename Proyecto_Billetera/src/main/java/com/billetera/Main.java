package com.billetera.app;

import com.billetera.exception.BilleteraException;
import com.billetera.functional.OperacionesFuncionales;
import com.billetera.generic.Repositorio;
import com.billetera.generic.RepositorioMemoria;
import com.billetera.model.Cuenta;
import com.billetera.model.CuentaBasica;
import com.billetera.model.CuentaPremium;
import com.billetera.model.Movimiento;
import com.billetera.model.TipoMovimiento;
import com.billetera.service.CuentaService;
import com.billetera.service.ReporteService;
import com.billetera.util.ComparadorSaldo;
import com.billetera.util.Ordenador;
import com.billetera.util.Utilidades;
import com.billetera.util.WildcardUtil;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    private static final String CLAVE_SIMULADA = "1234";

    private final Scanner entrada = new Scanner(System.in);
    private final Repositorio<Cuenta> cuentas = new RepositorioMemoria<>();
    private final CuentaService servicio = new CuentaService();
    private final ReporteService reportes = new ReporteService();
    private final Supplier<String> generadorIds = OperacionesFuncionales.generadorDeIds("M");

    private Cuenta cuentaActiva;
    private boolean autenticado;

    public static void main(String[] args) {
        new Main().iniciar();
    }

    private void iniciar() {
        cargarDatosDemo();
        LOGGER.info("Aplicacion iniciada.");
        String opcion;
        do {
            mostrarMenu();
            opcion = leerTexto("Opcion: ");
            procesarOpcion(opcion);
        } while (!opcion.equals("0"));
        LOGGER.info("Aplicacion finalizada.");
    }

    // ---------------- Menu ----------------

    private void mostrarMenu() {
        System.out.println();
        System.out.println("=== BILLETERA DIGITAL ===");
        if (Utilidades.esNulo(cuentaActiva)) {
            System.out.println("Cuenta activa: ninguna");
        } else {
            System.out.println("Cuenta activa: " + cuentaActiva.getNumero()
                    + " | Saldo: " + formato(cuentaActiva.getSaldo())
                    + " | Autenticado: " + (autenticado ? "si" : "no"));
        }
        System.out.println("1. Crear cuenta");
        System.out.println("2. Listar cuentas");
        System.out.println("3. Seleccionar cuenta");
        System.out.println("4. Autenticarse");
        System.out.println("5. Depositar");
        System.out.println("6. Retirar");
        System.out.println("7. Transferir");
        System.out.println("8. Extracto");
        System.out.println("9. Reportes");
        System.out.println("0. Salir");
    }

    private void procesarOpcion(String opcion) {
        switch (opcion) {
            case "1" -> crearCuenta();
            case "2" -> listarCuentas();
            case "3" -> seleccionarCuenta();
            case "4" -> autenticarse();
            case "5" -> depositar();
            case "6" -> retirar();
            case "7" -> transferir();
            case "8" -> mostrarExtracto();
            case "9" -> mostrarReportes();
            case "0" -> System.out.println("Hasta luego.");
            default -> LOGGER.warning("Opcion invalida: " + opcion);
        }
    }

    // ---------------- Cuentas y sesion ----------------

    private void crearCuenta() {
        String tipo = leerTexto("Tipo de cuenta (1 = Basica, 2 = Premium): ");
        String numero = leerTexto("Numero de cuenta: ");
        if (numero.isEmpty() || buscarCuenta(numero).isPresent()) {
            LOGGER.warning("Numero de cuenta vacio o ya existente: " + numero);
            return;
        }
        Optional<Double> saldoInicial = leerMonto("Saldo inicial: ");
        if (saldoInicial.isEmpty()) {
            return;
        }
        Cuenta nueva;
        if (tipo.equals("1")) {
            nueva = new CuentaBasica(numero, saldoInicial.get());
        } else if (tipo.equals("2")) {
            nueva = new CuentaPremium(numero, saldoInicial.get());
        } else {
            LOGGER.warning("Tipo de cuenta invalido: " + tipo);
            return;
        }
        cuentas.guardar(nueva);
        LOGGER.info("Cuenta creada: " + numero);
        System.out.println("Cuenta " + numero + " creada.");
    }

    private void listarCuentas() {
        List<Cuenta> lista = new ArrayList<>(cuentas.obtenerTodos());
        Collections.sort(lista, new ComparadorSaldo());
        System.out.println("Cuentas (de menor a mayor saldo):");
        for (Cuenta c : lista) {
            System.out.println("- " + c.getNumero() + " | " + c.getClass().getSimpleName()
                    + " | Saldo: " + formato(c.getSaldo())
                    + " | Limite diario: " + formato(c.limiteDiario()));
        }
        WildcardUtil.contarElementos(lista);
    }

    private void seleccionarCuenta() {
        String numero = leerTexto("Numero de cuenta: ");
        buscarCuenta(numero).ifPresentOrElse(
                c -> {
                    cuentaActiva = c;
                    autenticado = false;
                    LOGGER.info("Cuenta seleccionada: " + numero);
                },
                () -> LOGGER.warning("No existe la cuenta " + numero));
    }

    private void autenticarse() {
        if (!haySesion()) {
            return;
        }
        String clave = leerTexto("Clave: ");
        if (clave.equals(CLAVE_SIMULADA)) {
            autenticado = true;
            LOGGER.info("Autenticacion exitosa en la cuenta " + cuentaActiva.getNumero());
        } else {
            autenticado = false;
            LOGGER.warning("Clave incorrecta para la cuenta " + cuentaActiva.getNumero());
        }
    }

    // ---------------- Operaciones ----------------

    private void depositar() {
        if (!haySesion()) {
            return;
        }
        if (!autenticado) {
            LOGGER.severe("Deposito rechazado: la cuenta no esta autenticada.");
            return;
        }
        Optional<Double> monto = leerMonto("Monto a depositar: ");
        if (monto.isEmpty()) {
            return;
        }
        double valor = monto.get();
        OperacionesFuncionales.aplicar(cuentaActiva, c -> c.depositar(valor));
        registrar(cuentaActiva, valor, TipoMovimiento.DEPOSITO);
        LOGGER.info("Deposito de " + formato(valor) + " en la cuenta " + cuentaActiva.getNumero());
        System.out.println("Deposito realizado. Saldo: " + formato(cuentaActiva.getSaldo()));
    }

    private void retirar() {
        if (!haySesion()) {
            return;
        }
        Optional<Double> monto = leerMonto("Monto a retirar: ");
        if (monto.isEmpty()) {
            return;
        }
        double valor = monto.get();
        try {
            LOGGER.info("Intentando retiro de " + formato(valor));
            servicio.realizarRetiro(cuentaActiva, valor, autenticado);
            registrar(cuentaActiva, valor, TipoMovimiento.RETIRO);
            System.out.println("Retiro realizado. Saldo: " + formato(cuentaActiva.getSaldo()));
        } catch (BilleteraException e) {
            LOGGER.severe("Retiro rechazado: " + e.getMessage());
        } finally {
            LOGGER.info("Fin del intento de retiro.");
        }
    }

    private void transferir() {
        if (!haySesion()) {
            return;
        }
        String numeroDestino = leerTexto("Cuenta destino: ");
        Optional<Cuenta> destino = buscarCuenta(numeroDestino);
        if (destino.isEmpty()) {
            LOGGER.warning("No existe la cuenta destino " + numeroDestino);
            return;
        }
        if (destino.get() == cuentaActiva) {
            LOGGER.warning("La cuenta destino debe ser distinta a la de origen.");
            return;
        }
        Optional<Double> monto = leerMonto("Monto a transferir: ");
        if (monto.isEmpty()) {
            return;
        }
        double valor = monto.get();
        try {
            LOGGER.info("Intentando transferencia de " + formato(valor) + " a " + numeroDestino);
            servicio.realizarRetiro(cuentaActiva, valor, autenticado);
            destino.get().depositar(valor);
            registrar(cuentaActiva, valor, TipoMovimiento.TRANSFERENCIA);
            registrar(destino.get(), valor, TipoMovimiento.DEPOSITO);
            System.out.println("Transferencia realizada. Saldo: " + formato(cuentaActiva.getSaldo()));
        } catch (BilleteraException e) {
            LOGGER.severe("Transferencia rechazada: " + e.getMessage());
        } finally {
            LOGGER.info("Fin del intento de transferencia.");
        }
    }

    // ---------------- Extracto y reportes ----------------

    private void mostrarExtracto() {
        if (!haySesion()) {
            return;
        }
        List<Movimiento> movimientos = cuentaActiva.getMovimientos();
        if (movimientos.isEmpty()) {
            System.out.println("La cuenta no tiene movimientos.");
            return;
        }
        System.out.println("Ordenar por:");
        System.out.println("1. Monto ascendente (Comparable + burbuja propia)");
        System.out.println("2. Monto descendente (Comparator lambda)");
        System.out.println("3. Tipo y monto (Comparator compuesto)");
        List<Movimiento> ordenados;
        switch (leerTexto("Criterio: ")) {
            case "1" -> {
                ordenados = new ArrayList<>(movimientos);
                Ordenador.ordenar(ordenados);
            }
            case "2" -> ordenados = OperacionesFuncionales.ordenar(movimientos, OperacionesFuncionales.POR_MONTO_DESC);
            case "3" -> ordenados = OperacionesFuncionales.ordenar(movimientos, OperacionesFuncionales.POR_TIPO_Y_MONTO_DESC);
            default -> {
                LOGGER.warning("Criterio de orden invalido.");
                return;
            }
        }
        System.out.println("Extracto de la cuenta " + cuentaActiva.getNumero());
        OperacionesFuncionales.recorrer(ordenados, m -> System.out.println(
                m.getId() + " | " + m.getTipo() + " | " + formato(m.getMonto()) + " | " + m.getFecha()));
        System.out.println("Suma de montos: " + formato(WildcardUtil.sumarMontoTotal(ordenados)));
    }

    private void mostrarReportes() {
        if (!haySesion()) {
            return;
        }
        List<Movimiento> movimientos = cuentaActiva.getMovimientos();
        System.out.println("1. Total por tipo de movimiento");
        System.out.println("2. Movimientos de las ultimas N horas");
        System.out.println("3. Promedio, total y mayor movimiento");
        System.out.println("4. Movimientos mayores a un monto");
        switch (leerTexto("Reporte: ")) {
            case "1" -> OperacionesFuncionales.totalPorTipo(movimientos)
                    .forEach((tipo, total) -> System.out.println(tipo + ": " + formato(total)));
            case "2" -> reportePorPeriodo(movimientos);
            case "3" -> {
                System.out.println("Promedio: " + formato(OperacionesFuncionales.promediarMontos(movimientos)));
                System.out.println("Total: " + formato(reportes.total(movimientos)));
                reportes.buscarMayor(movimientos).ifPresentOrElse(
                        m -> System.out.println("Mayor movimiento: " + m.getId() + " por " + formato(m.getMonto())),
                        () -> System.out.println("No hay movimientos."));
            }
            case "4" -> leerMonto("Monto minimo: ").ifPresent(minimo ->
                    OperacionesFuncionales.recorrer(
                            OperacionesFuncionales.filtrar(movimientos, OperacionesFuncionales.montoMayorA(minimo)),
                            m -> System.out.println(m.getId() + " | " + m.getTipo() + " | " + formato(m.getMonto()))));
            default -> LOGGER.warning("Reporte invalido.");
        }
    }

    private void reportePorPeriodo(List<Movimiento> movimientos) {
        String texto = leerTexto("Horas hacia atras: ");
        try {
            long horas = Long.parseLong(texto);
            LocalDateTime hasta = LocalDateTime.now();
            LocalDateTime desde = hasta.minusHours(horas);
            List<Movimiento> enPeriodo = OperacionesFuncionales.filtrar(
                    movimientos, OperacionesFuncionales.enPeriodo(desde, hasta));
            System.out.println("Movimientos en el periodo: " + enPeriodo.size()
                    + " | Total: " + formato(OperacionesFuncionales.sumarMontos(enPeriodo)));
        } catch (NumberFormatException e) {
            LOGGER.warning("Cantidad de horas invalida: " + texto);
        }
    }

    // ---------------- Utilidades internas ----------------

    private void cargarDatosDemo() {
        Cuenta basica = new CuentaBasica("1001", 500000);
        Cuenta premium = new CuentaPremium("2002", 3000000);
        cuentas.guardar(basica);
        cuentas.guardar(premium);
        registrar(basica, 300000, TipoMovimiento.DEPOSITO);
        registrar(basica, 80000, TipoMovimiento.RETIRO);
        registrar(premium, 2000000, TipoMovimiento.DEPOSITO);
        registrar(premium, 150000, TipoMovimiento.TRANSFERENCIA);
    }

    private void registrar(Cuenta cuenta, double monto, TipoMovimiento tipo) {
        cuenta.getMovimientos().add(new Movimiento(generadorIds.get(), monto, tipo));
    }

    private Optional<Cuenta> buscarCuenta(String numero) {
        return OperacionesFuncionales.buscarCuenta(cuentas.obtenerTodos(), numero);
    }

    private boolean haySesion() {
        if (Utilidades.esNulo(cuentaActiva)) {
            LOGGER.warning("Primero seleccione una cuenta (opcion 3).");
            return false;
        }
        return true;
    }

    private Optional<Double> leerMonto(String mensaje) {
        String texto = leerTexto(mensaje);
        try {
            double monto = Double.parseDouble(texto);
            if (monto <= 0) {
                LOGGER.warning("El monto debe ser mayor que cero: " + texto);
                return Optional.empty();
            }
            return Optional.of(monto);
        } catch (NumberFormatException e) {
            LOGGER.warning("Monto invalido: " + texto);
            return Optional.empty();
        }
    }

    private String leerTexto(String mensaje) {
        System.out.print(mensaje);
        if (!entrada.hasNextLine()) {
            return "0";
        }
        return entrada.nextLine().trim();
    }

    private String formato(double valor) {
        return String.format("%,.0f", valor);
    }
}
