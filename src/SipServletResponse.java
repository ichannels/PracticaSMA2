import mensajesSIP.SIPMessage;

import java.net.DatagramSocket;
import java.net.InetAddress;

public class SipServletResponse implements SipServletResponseInterface {

    private int codigoMensaje;
    private SIPMessage mensaje;
    private InetAddress address;
    private String ports;
    private DatagramSocket s = null;
    public SipServletResponse(){

    }

    public SipServletResponse(int codigoMensaje, SIPMessage mensaje){
        this.codigoMensaje=codigoMensaje;
    }


    @Override
    public void send() {
        switch (codigoMensaje){
            case 100:{
                Proxy.code=100;
            }
            case 503:{
                Proxy.code=503;
            }
        }
    }

}
