package com.billetera.proxy;
import com.billetera.exception.BilleteraException;
import java.util.logging.Logger;
public class CuentaProxy implements OperacionCuenta {
    private static final Logger LOG = Logger.getLogger(CuentaProxy.class.getName());
    private CuentaReal cuentaReal;
    private boolean autenticado;
    
    public CuentaProxy(CuentaReal real, boolean auth) {
        this.cuentaReal = real; this.autenticado = auth;
    }
    @Override
    public void retirar(double monto) throws BilleteraException {
        if(!autenticado) {
            LOG.severe("ERROR: Intento de transaccion sin autenticacion.");
            throw new BilleteraException("Autenticacion denegada");
        }
        LOG.info("INFO: Autorizacion exitosa en Proxy. Procediendo a cuenta real.");
        cuentaReal.retirar(monto);
    }
}