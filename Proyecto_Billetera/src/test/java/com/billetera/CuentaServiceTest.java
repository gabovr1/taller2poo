package com.billetera;

import com.billetera.chain.Validador;
import com.billetera.chain.ValidadorFraude;
import com.billetera.chain.ValidadorLimite;
import com.billetera.chain.ValidadorSaldo;
import com.billetera.exception.BilleteraException;
import com.billetera.exception.FraudeException;
import com.billetera.exception.LimiteDiarioException;
import com.billetera.exception.SaldoInsuficienteException;
import com.billetera.functional.OperacionesFuncionales;
import com.billetera.generic.Caja;
import com.billetera.generic.Repositorio;
import com.billetera.generic.RepositorioMemoria;
import com.billetera.model.Cuenta;
import com.billetera.model.CuentaBasica;
import com.billetera.model.CuentaPremium;
import com.billetera.model.Movimiento;
import com.billetera.model.TipoMovimiento;
import com.billetera.proxy.CuentaProxy;
import com.billetera.proxy.CuentaReal;
import com.billetera.service.CuentaService;
import com.billetera.service.ReporteService;
import com.billetera.util.ComparadorSaldo;
import com.billetera.util.Ordenador;
import com.billetera.util.Utilidades;
import com.billetera.util.WildcardUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CuentaServiceTest {

    private CuentaService servicio;
    private ReporteService reportes;
    private Cuenta basica;
    private Cuenta premium;
    private List<Movimiento> movimientos;

    @BeforeEach
    public void preparar() {
        servicio = new CuentaService();
        reportes = new ReporteService();
        basica = new CuentaBasica("1001", 5000000);
        premium = new CuentaPremium("2002", 100000000);
        movimientos = new ArrayList<>(Arrays.asList(
                new Movimiento("M1", 300, TipoMovimiento.DEPOSITO),
                new Movimiento("M2", 100, TipoMovimiento.RETIRO),
                new Movimiento("M3", 200, TipoMovimiento.RETIRO),
                new Movimiento("M4", 50, TipoMovimiento.TRANSFERENCIA)));
    }

    // =====================================================
    // 1. Servicios principales
    // =====================================================

    @Test
    public void retiroExitosoDescuentaElSaldo() throws BilleteraException {
        servicio.realizarRetiro(basica, 1000000, true);
        assertEquals(4000000, basica.getSaldo());
    }

    @Test
    public void retiroConSaldoInsuficienteLanzaExcepcion() {
        Cuenta pobre = new CuentaBasica("1", 100);
        assertThrows(SaldoInsuficienteException.class, () -> servicio.realizarRetiro(pobre, 500, true));
        assertEquals(100, pobre.getSaldo());
    }

    @Test
    public void retiroQueSuperaElLimiteDiarioLanzaExcepcion() {
        assertThrows(LimiteDiarioException.class, () -> servicio.realizarRetiro(basica, 3000000, true));
        assertEquals(5000000, basica.getSaldo());
    }

    @Test
    public void depositarAumentaElSaldo() {
        basica.depositar(250000);
        assertEquals(5250000, basica.getSaldo());
    }

    @Test
    public void reporteTotalSumaTodosLosMontos() {
        assertEquals(650, reportes.total(movimientos));
    }

    @Test
    public void reporteBuscarMayorDevuelveElMovimientoMasGrande() {
        Optional<Movimiento> mayor = reportes.buscarMayor(movimientos);
        assertTrue(mayor.isPresent());
        assertEquals("M1", mayor.get().getId());
    }

    @Test
    public void reporteBuscarMayorEnListaVaciaDevuelveOptionalVacio() {
        assertFalse(reportes.buscarMayor(new ArrayList<>()).isPresent());
    }

    // =====================================================
    // 2. Excepciones (jerarquia)
    // =====================================================

    @Test
    public void todasLasExcepcionesHeredanDeBilleteraException() {
        assertTrue(BilleteraException.class.isAssignableFrom(SaldoInsuficienteException.class));
        assertTrue(BilleteraException.class.isAssignableFrom(LimiteDiarioException.class));
        assertTrue(BilleteraException.class.isAssignableFrom(FraudeException.class));
    }

    // =====================================================
    // 3. Patron Chain of Responsibility
    // =====================================================

    @Test
    public void validadorSaldoAceptaMontoCubierto() throws BilleteraException {
        new ValidadorSaldo().validar(basica, 1000);
        assertEquals(5000000, basica.getSaldo());
    }

    @Test
    public void validadorSaldoRechazaMontoMayorAlSaldo() {
        assertThrows(SaldoInsuficienteException.class, () -> new ValidadorSaldo().validar(basica, 6000000));
    }

    @Test
    public void validadorLimiteRechazaMontoSobreElLimiteDeLaCuenta() {
        assertThrows(LimiteDiarioException.class, () -> new ValidadorLimite().validar(basica, 2000001));
    }

    @Test
    public void validadorLimiteDependeDelTipoDeCuenta() throws BilleteraException {
        new ValidadorLimite().validar(premium, 5000000);
        assertThrows(LimiteDiarioException.class, () -> new ValidadorLimite().validar(basica, 5000000));
    }

    @Test
    public void validadorFraudeRechazaMontosExtremos() {
        assertThrows(FraudeException.class, () -> new ValidadorFraude().validar(premium, 60000000));
    }

    @Test
    public void validadorFraudeAceptaMontosNormales() throws BilleteraException {
        new ValidadorFraude().validar(premium, 1000000);
    }

    @Test
    public void cadenaPasaLaSolicitudAlSiguienteEslabon() {
        Validador saldo = new ValidadorSaldo();
        Validador limite = new ValidadorLimite();
        saldo.setSiguiente(limite);
        assertThrows(LimiteDiarioException.class, () -> saldo.validar(basica, 3000000));
    }

    @Test
    public void cadenaRespetaElOrdenSaldoPrimeroLimiteDespues() {
        Cuenta sinFondos = new CuentaBasica("3", 100);
        assertThrows(SaldoInsuficienteException.class, () -> servicio.realizarRetiro(sinFondos, 3000000, true));
    }

    @Test
    public void cadenaCompletaDetectaFraudeCuandoSeArmaConLimiteAlto() {
        Validador saldo = new ValidadorSaldo();
        Validador fraude = new ValidadorFraude();
        saldo.setSiguiente(fraude);
        assertThrows(FraudeException.class, () -> saldo.validar(premium, 60000000));
    }

    // =====================================================
    // 4. Patron Proxy
    // =====================================================

    @Test
    public void proxySinAutenticacionBloqueaLaOperacion() {
        assertThrows(BilleteraException.class, () -> servicio.realizarRetiro(basica, 1000, false));
        assertEquals(5000000, basica.getSaldo());
    }

    @Test
    public void proxyAutenticadoDelegaEnLaCuentaReal() throws BilleteraException {
        CuentaProxy proxy = new CuentaProxy(new CuentaReal(basica), true);
        proxy.retirar(500000);
        assertEquals(4500000, basica.getSaldo());
    }

    @Test
    public void proxyNoAutenticadoNoLlegaALaCuentaReal() {
        CuentaProxy proxy = new CuentaProxy(new CuentaReal(basica), false);
        assertThrows(BilleteraException.class, () -> proxy.retirar(500000));
        assertEquals(5000000, basica.getSaldo());
    }

    @Test
    public void proxyAutenticadoTambienAplicaLasValidaciones() {
        CuentaProxy proxy = new CuentaProxy(new CuentaReal(basica), true);
        assertThrows(LimiteDiarioException.class, () -> proxy.retirar(3000000));
    }

    // =====================================================
    // 5. Ordenamiento
    // =====================================================

    @Test
    public void ordenadorManualOrdenaDeMenorAMayor() {
        Ordenador.ordenar(movimientos);
        assertEquals(50, movimientos.get(0).getMonto());
        assertEquals(300, movimientos.get(3).getMonto());
    }

    @Test
    public void ordenadorManualDaElMismoResultadoQueCollectionsSort() {
        List<Movimiento> copia = new ArrayList<>(movimientos);
        Ordenador.ordenar(movimientos);
        Collections.sort(copia);
        for (int i = 0; i < copia.size(); i++) {
            assertEquals(copia.get(i).getId(), movimientos.get(i).getId());
        }
    }

    @Test
    public void ordenadorManualFuncionaConEnterosYTextos() {
        List<Integer> numeros = new ArrayList<>(Arrays.asList(5, 1, 4, 2, 3));
        List<String> palabras = new ArrayList<>(Arrays.asList("c", "a", "b"));
        Ordenador.ordenar(numeros);
        Ordenador.ordenar(palabras);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), numeros);
        assertEquals(Arrays.asList("a", "b", "c"), palabras);
    }

    @Test
    public void ordenadorManualAceptaListaVaciaYDeUnElemento() {
        List<Integer> vacia = new ArrayList<>();
        List<Integer> unica = new ArrayList<>(Collections.singletonList(7));
        Ordenador.ordenar(vacia);
        Ordenador.ordenar(unica);
        assertTrue(vacia.isEmpty());
        assertEquals(7, unica.get(0));
    }

    @Test
    public void ordenadorManualConservaListaYaOrdenadaYDuplicados() {
        List<Integer> numeros = new ArrayList<>(Arrays.asList(1, 2, 2, 3));
        Ordenador.ordenar(numeros);
        assertEquals(Arrays.asList(1, 2, 2, 3), numeros);
    }

    @Test
    public void movimientoCompareToUsaElMonto() {
        Movimiento chico = new Movimiento("a", 10, TipoMovimiento.DEPOSITO);
        Movimiento grande = new Movimiento("b", 99, TipoMovimiento.DEPOSITO);
        assertTrue(chico.compareTo(grande) < 0);
        assertTrue(grande.compareTo(chico) > 0);
        assertEquals(0, chico.compareTo(new Movimiento("c", 10, TipoMovimiento.RETIRO)));
    }

    @Test
    public void comparadorSaldoComoClaseOrdenaCuentasPorSaldo() {
        List<Cuenta> lista = new ArrayList<>(Arrays.asList(premium, basica));
        Collections.sort(lista, new ComparadorSaldo());
        assertEquals("1001", lista.get(0).getNumero());
    }

    @Test
    public void comparadorLambdaOrdenaPorMontoDescendente() {
        movimientos.sort(OperacionesFuncionales.POR_MONTO_DESC);
        assertEquals("M1", movimientos.get(0).getId());
        assertEquals("M4", movimientos.get(3).getId());
    }

    @Test
    public void comparadorCompuestoOrdenaPorTipoYMontoEnReversa() {
        movimientos.sort(OperacionesFuncionales.POR_TIPO_Y_MONTO_DESC);
        assertEquals(TipoMovimiento.TRANSFERENCIA, movimientos.get(0).getTipo());
        assertEquals(TipoMovimiento.DEPOSITO, movimientos.get(3).getTipo());
        assertEquals("M3", movimientos.get(1).getId());
        assertEquals("M2", movimientos.get(2).getId());
    }

    // =====================================================
    // 6. Genericos
    // =====================================================

    @Test
    public void cajaGuardaCualquierTipo() {
        Caja<String> texto = new Caja<>("Test");
        Caja<Integer> numero = new Caja<>(42);
        assertEquals("Test", texto.getValor());
        assertEquals(42, numero.getValor());
    }

    @Test
    public void repositorioMemoriaGuardaYDevuelveLosElementos() {
        Repositorio<Cuenta> repo = new RepositorioMemoria<>();
        repo.guardar(basica);
        repo.guardar(premium);
        assertEquals(2, repo.obtenerTodos().size());
        assertTrue(repo.obtenerTodos().contains(premium));
    }

    @Test
    public void repositorioVacioDevuelveOptionalVacio() {
        Repositorio<Cuenta> repo = new RepositorioMemoria<>();
        assertFalse(repo.buscarPorId("1001").isPresent());
    }

    @Test
    public void utilidadesGenericasFuncionanConDistintosTipos() {
        assertTrue(Utilidades.esNulo(null));
        assertFalse(Utilidades.esNulo("algo"));
        assertEquals("a", Utilidades.obtenerPrimero(Arrays.asList("a", "b")));
        assertNull(Utilidades.obtenerPrimero(new ArrayList<String>()));
    }

    // =====================================================
    // 7. Wildcards
    // =====================================================

    @Test
    public void wildcardProductorSumaMontosDeSubtipos() {
        assertEquals(650, WildcardUtil.sumarMontoTotal(movimientos));
    }

    @Test
    public void wildcardConsumidorAgregaEnListaDeSupertipos() {
        List<Object> destino = new ArrayList<>();
        WildcardUtil.agregarRetiro(destino, 500);
        assertEquals(1, destino.size());
        List<Movimiento> soloMovimientos = new ArrayList<>();
        WildcardUtil.agregarRetiro(soloMovimientos, 700);
        assertEquals(700, soloMovimientos.get(0).getMonto());
    }

    @Test
    public void wildcardLibreAceptaCualquierLista() {
        WildcardUtil.contarElementos(movimientos);
        WildcardUtil.contarElementos(Arrays.asList(1, 2, 3));
    }

    // =====================================================
    // 8. Programacion funcional y Streams
    // =====================================================

    @Test
    public void filtrarPorTipoDevuelveSoloEsosMovimientos() {
        List<Movimiento> retiros = OperacionesFuncionales.filtrar(
                movimientos, OperacionesFuncionales.esDelTipo(TipoMovimiento.RETIRO));
        assertEquals(2, retiros.size());
        assertTrue(retiros.stream().allMatch(m -> m.getTipo() == TipoMovimiento.RETIRO));
    }

    @Test
    public void filtrarPorMontoMayorA() {
        List<Movimiento> grandes = OperacionesFuncionales.filtrar(
                movimientos, OperacionesFuncionales.montoMayorA(150));
        assertEquals(2, grandes.size());
    }

    @Test
    public void extraerMontosAplicaLaFunction() {
        List<Double> montos = OperacionesFuncionales.extraer(movimientos, Movimiento::getMonto);
        assertEquals(Arrays.asList(300.0, 100.0, 200.0, 50.0), montos);
    }

    @Test
    public void ordenarConStreamNoModificaLaListaOriginal() {
        List<Movimiento> ordenados = OperacionesFuncionales.ordenar(movimientos, Comparator.comparingDouble(Movimiento::getMonto));
        assertEquals("M4", ordenados.get(0).getId());
        assertEquals("M1", movimientos.get(0).getId());
    }

    @Test
    public void sumarYPromediarMontos() {
        assertEquals(650, OperacionesFuncionales.sumarMontos(movimientos));
        assertEquals(162.5, OperacionesFuncionales.promediarMontos(movimientos));
    }

    @Test
    public void promedioDeListaVaciaEsCero() {
        assertEquals(0.0, OperacionesFuncionales.promediarMontos(new ArrayList<>()));
    }

    @Test
    public void montoMaximoConReduce() {
        assertEquals(300, OperacionesFuncionales.montoMaximo(movimientos));
        assertEquals(0, OperacionesFuncionales.montoMaximo(new ArrayList<>()));
    }

    @Test
    public void totalPorTipoAgrupaLosMontos() {
        Map<TipoMovimiento, Double> totales = OperacionesFuncionales.totalPorTipo(movimientos);
        assertEquals(300, totales.get(TipoMovimiento.DEPOSITO));
        assertEquals(300, totales.get(TipoMovimiento.RETIRO));
        assertEquals(50, totales.get(TipoMovimiento.TRANSFERENCIA));
    }

    @Test
    public void filtrarPorPeriodoIncluyeSoloLosMovimientosDentroDelRango() {
        LocalDateTime ahora = LocalDateTime.now();
        List<Movimiento> dentro = OperacionesFuncionales.filtrar(
                movimientos, OperacionesFuncionales.enPeriodo(ahora.minusHours(1), ahora.plusHours(1)));
        List<Movimiento> fuera = OperacionesFuncionales.filtrar(
                movimientos, OperacionesFuncionales.enPeriodo(ahora.minusDays(10), ahora.minusDays(5)));
        assertEquals(4, dentro.size());
        assertTrue(fuera.isEmpty());
    }

    @Test
    public void busquedaDeMovimientoUsaOptional() {
        assertTrue(OperacionesFuncionales.buscarMovimiento(movimientos, "M2").isPresent());
        assertFalse(OperacionesFuncionales.buscarMovimiento(movimientos, "XX").isPresent());
    }

    @Test
    public void busquedaDeCuentaUsaOptional() {
        List<Cuenta> lista = Arrays.asList(basica, premium);
        assertEquals(premium, OperacionesFuncionales.buscarCuenta(lista, "2002").orElse(null));
        assertTrue(OperacionesFuncionales.buscarCuenta(lista, "9999").isEmpty());
    }

    @Test
    public void consumerRecorreTodosLosMovimientos() {
        List<String> ids = new ArrayList<>();
        OperacionesFuncionales.recorrer(movimientos, m -> ids.add(m.getId()));
        assertEquals(Arrays.asList("M1", "M2", "M3", "M4"), ids);
    }

    @Test
    public void supplierGeneraIdsConsecutivos() {
        Supplier<String> ids = OperacionesFuncionales.generadorDeIds("T");
        assertEquals("T1", ids.get());
        assertEquals("T2", ids.get());
    }

    @Test
    public void interfazFuncionalPropiaSeEjecutaSobreLaCuenta() {
        OperacionesFuncionales.aplicar(basica, c -> c.depositar(1000));
        assertEquals(5001000, basica.getSaldo());
    }
}
