package com.billetera;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.billetera.model.*;
import com.billetera.service.CuentaService;
import com.billetera.exception.*;
import com.billetera.generic.*;
import com.billetera.util.*;
import java.util.*;

public class CuentaServiceTest {
    // 1. Probar Servicios / Patrones
    @Test
    public void probarProxySinAuth() {
        Cuenta c = new CuentaBasica("1", 5000);
        CuentaService cs = new CuentaService();
        assertThrows(BilleteraException.class, () -> {
            cs.realizarRetiro(c, 1000, false);
        });
    }
    
    // 2. Probar Validador (Chain of Responsibility)
    @Test
    public void probarChainSaldoInsuficiente() {
        Cuenta c = new CuentaBasica("1", 100);
        CuentaService cs = new CuentaService();
        assertThrows(SaldoInsuficienteException.class, () -> {
            cs.realizarRetiro(c, 500, true);
        });
    }

    // 3. Probar Ordenamiento Manual Generico
    @Test
    public void probarOrdenamientoBurbuja() {
        List<Movimiento> m = Arrays.asList(
            new Movimiento("1", 100, TipoMovimiento.DEPOSITO),
            new Movimiento("2", 10, TipoMovimiento.DEPOSITO)
        );
        Ordenador.ordenar(m);
        assertEquals(10, m.get(0).getMonto()); // Debe quedar primero el menor
    }

    // 4. Probar Clases Genericas
    @Test
    public void probarCajaGenerica() {
        Caja<String> c = new Caja<>("Test");
        assertEquals("Test", c.getValor());
    }
}