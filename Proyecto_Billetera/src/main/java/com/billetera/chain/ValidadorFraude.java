package com.billetera.chain;
import com.billetera.exception.*;
import com.billetera.model.Cuenta;
import java.util.logging.Logger;
public class ValidadorFraude extends Validador {
    private static final Logger LOG = Logger.getLogger(ValidadorFraude.class.getName());
    @Override public void validar(Cuenta c, double m) throws BilleteraException {
        if(m > 8000000) {
            LOG.warning("ADVERTENCIA: Movimiento inusualmente grande detectado para revision."); // Nivel WARNING exigido
        }
        if(m > 50000000) throw new FraudeException("Rechazo por politicas antifraude.");
        procesarSiguiente(c, m);
    }
}