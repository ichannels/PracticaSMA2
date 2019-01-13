import mensajesSIP.*;

import java.io.IOException;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Proxy {
    private static Boolean looseRouting;
    private static InetAddress addressLocal;
    private static String puertoEscuchaProxy;
    private static int cSeq;
    private static boolean debug;
    private static boolean procesando;
    public static int code;
    public static void main(String[] args) {
        DatagramSocket s = null;
        //Atributos de UA
        if (args.length==0)
        {
            puertoEscuchaProxy = "5060";
            looseRouting=true;
            debug = true;
        }
        else
        {
            puertoEscuchaProxy = args[0];
            looseRouting = Boolean.valueOf(args[1]);
            Boolean debug = Boolean.valueOf(args[2]);
        }
        Hilo1 h1;
        try {
            String address = FindMyIPv4.main();
            addressLocal = InetAddress.getByName(address);
        }
        catch (SocketException | UnknownHostException se)
        {
            se.printStackTrace(System.out);
        }

        //Creo un objeto de tipo socket
        try {
            //Creo un objeto de tipo DatagramSocket
            s = new DatagramSocket(Integer.parseInt(puertoEscuchaProxy));
        } catch (SocketException se) {
            se.printStackTrace(System.out);
        }
        h1 = new Hilo1(s);
        h1.run();
    }

    //Hilo encargado de crear el socket
    public static class Hilo1 extends Thread {
        //Objeto de tipo Socket
        DatagramSocket s;
        //Buffer donde recibe las cosas del socket
        byte[] buffer = new byte[1000];
        String mensaje;
        InetAddress address = null;
        ArrayList<ArrayList> lista = new ArrayList<>();
        ArrayList<String> usuario1 = new ArrayList<>();
        ArrayList<String> usuario2 = new ArrayList<>();
        ArrayList<String> usuario3 = new ArrayList<>();
        String numero_secuencia = null;

        //Constructor del hilo con el objeto de tipo socket
        Hilo1(DatagramSocket s) {
            this.s = s;
        }

        public void run() {
            lista.add(usuario1);
            lista.add(usuario2);
            lista.add(usuario3);
            usuario1.add("javier");
            usuario1.add("pass");
            usuario1.add("");
            usuario1.add("");
            usuario2.add("ignacio");
            usuario2.add("pass");
            usuario2.add("");
            usuario2.add("");
            usuario3.add("boss");
            usuario3.add("pass");
            usuario3.add("");
            usuario3.add("");
            try {
                while (true) {
                    //Creo un objeto de tipo paquete
                    DatagramPacket p = new DatagramPacket(buffer, buffer.length);
                    s.receive(p);
                    mensaje = new String(buffer, 0, p.getLength());
                    procesarMensaje(mensaje);
                }
            } catch (IOException ioe) {
                ioe.printStackTrace(System.out);
            }
        }

        void procesarMensaje(String mensaje) {
            //Array de usuarios
            String parts[];
            String ports = null;
            int indice = 0;
            int i = 0;

            try {
                //Cojo el mensaje
                SIPMessage m = SIPMessage.parseMessage(mensaje);
                //Si el mensaje es de tipo REGISTER

                if (m instanceof RegisterMessage) {
                    Proxy.cSeq=Integer.parseInt(((RegisterMessage) m).getcSeqNumber());
                    Proxy.cSeq++;
                    parts = ((RegisterMessage) m).getContact().split(":");
                    String address_str = parts[0]; //
                    String portRemoto = parts[1]; //

                    try {
                        address = InetAddress.getByName(address_str);
                    } catch (UnknownHostException uhe) {
                        uhe.printStackTrace(System.out);
                    }


                    parts = ((RegisterMessage) m).destination.split(":");
                    String uri = parts[1];
                    parts = uri.split("@");

                    if (lista.get(0).get(0).equals(parts[0]) || lista.get(1).get(0).equals(parts[0]) || lista.get(2).get(0).equals(parts[0])) {
                        OKMessage okMessage = new OKMessage();
                        okMessage.setVias(((RegisterMessage) m).getVias());
                        okMessage.setToName(((RegisterMessage) m).getToName());
                        okMessage.setToUri(((RegisterMessage) m).getToUri());
                        okMessage.setFromName(((RegisterMessage) m).getFromName());
                        okMessage.setFromUri(((RegisterMessage) m).getFromUri());
                        okMessage.setCallId(((RegisterMessage) m).getCallId());
                        okMessage.setcSeqNumber(Integer.toString(Proxy.cSeq));
                        okMessage.setcSeqStr(((RegisterMessage) m).getcSeqStr());
                        okMessage.setContact(((RegisterMessage) m).getContact());
                        okMessage.setExpires(((RegisterMessage) m).getExpires());
                        okMessage.setContentLength(((RegisterMessage) m).getContentLength());

                        DatagramPacket p = new DatagramPacket(okMessage.toStringMessage().getBytes(), okMessage.toStringMessage().getBytes().length, address, Integer.parseInt(portRemoto));

                        try {
                            s.send(p);
                        } catch (IOException ioe) {
                            ioe.printStackTrace(System.out);
                        }
                        if (!debug)
                        {
                            int iend = okMessage.toStringMessage().indexOf("\n");
                            String substring;
                            if (iend != -1)
                            {
                                substring = okMessage.toStringMessage().substring(0, iend);
                                System.out.println(substring);
                            }
                        }
                        else System.out.println(okMessage.toStringMessage());
                        i=0;
                        while (i < lista.size()) {
                            if (lista.get(i).get(0).equals(parts[0])) {
                                lista.get(i).set(2, address_str);
                                lista.get(i).set(3, portRemoto);
                            }
                            i++;
                        }
                    } else {
                        NotFoundMessage notFoundMessage = new NotFoundMessage();
                        notFoundMessage.setVias(((RegisterMessage) m).getVias());
                        notFoundMessage.setToName(((RegisterMessage) m).getToName());
                        notFoundMessage.setToUri(((RegisterMessage) m).getToUri());
                        notFoundMessage.setFromName(((RegisterMessage) m).getFromName());
                        notFoundMessage.setFromUri(((RegisterMessage) m).getFromUri());
                        notFoundMessage.setCallId(((RegisterMessage) m).getCallId());
                        notFoundMessage.setcSeqNumber(Integer.toString(Proxy.cSeq));
                        notFoundMessage.setcSeqStr(((RegisterMessage) m).getcSeqStr());
                        notFoundMessage.setContact(((RegisterMessage) m).getContact());
                        notFoundMessage.setExpires(Integer.toString(7200));
                        notFoundMessage.setContentLength(0);

                        DatagramPacket p = new DatagramPacket(notFoundMessage.toStringMessage().getBytes(), notFoundMessage.toStringMessage().getBytes().length, address, Integer.parseInt(portRemoto));

                        try {
                            s.send(p);
                        } catch (IOException ioe) {
                            ioe.printStackTrace(System.out);
                        }
                        if (!debug)
                        {
                            int iend = notFoundMessage.toStringMessage().indexOf("\n");
                            String substring;
                            if (iend != -1)
                            {
                                substring = notFoundMessage.toStringMessage().substring(0, iend);
                                System.out.println(substring);
                            }
                        }
                        else System.out.println(notFoundMessage.toStringMessage());
                    }
                }
                if (m instanceof RingingMessage) {
                    ((RingingMessage) m).deleteVia();
                    parts = ((RingingMessage) m).getVias().get(0).split(":");
                    try {
                        address = InetAddress.getByName(parts[0]);
                    } catch (UnknownHostException uhe) {
                        uhe.printStackTrace(System.out);
                    }
                    ports = parts[1];
                    DatagramPacket p = new DatagramPacket(m.toStringMessage().getBytes(), m.toStringMessage().getBytes().length, address, Integer.parseInt(ports));

                    try {
                        s.send(p);
                    } catch (IOException ioe) {
                        ioe.printStackTrace(System.out);
                    }
                    if (!debug)
                    {
                        int iend = m.toStringMessage().indexOf("\n");
                        String substring;
                        if (iend != -1)
                        {
                            substring = m.toStringMessage().substring(0, iend);
                            System.out.println(substring);
                        }
                    }
                    else System.out.println(m.toStringMessage());
                }
                if (m instanceof RequestTimeoutMessage) {

                    //Los mensajes que no sean 200 se contestan salto a salto
                    Proxy.cSeq=Integer.parseInt(((RequestTimeoutMessage) m).getcSeqNumber());
                    Proxy.cSeq++;
                    i=0;
                    while (i < lista.size()) {
                        if (lista.get(i).get(0).equals(((RequestTimeoutMessage) m).getToName().toLowerCase())) {
                            indice = i;
                            try {
                                address = InetAddress.getByName(lista.get(indice).get(2).toString());
                            } catch (UnknownHostException uhe) {
                                uhe.printStackTrace(System.out);
                            }
                            ports = lista.get(indice).get(3).toString();
                        }
                        i++;
                    }
                    ACKMessage ACKmessage = new ACKMessage();
                    ((RequestTimeoutMessage) m).deleteVia();
                    ACKmessage.setDestination(((RequestTimeoutMessage) m).getToUri());
                    ACKmessage.setVias(((RequestTimeoutMessage) m).getVias());
                    ACKmessage.setFromName(((RequestTimeoutMessage) m).getFromName());
                    ACKmessage.setFromUri(((RequestTimeoutMessage) m).getFromUri());
                    ACKmessage.setToName(((RequestTimeoutMessage) m).getToName());
                    ACKmessage.setToUri(((RequestTimeoutMessage) m).getToUri());
                    ACKmessage.setCallId(((RequestTimeoutMessage) m).getCallId());
                    ACKmessage.setcSeqNumber(Integer.toString(Proxy.cSeq));
                    ACKmessage.setContentLength(((RequestTimeoutMessage) m).getContentLength());
                    ACKmessage.setcSeqStr("ACK");
                    DatagramPacket p = new DatagramPacket(ACKmessage.toStringMessage().getBytes(), ACKmessage.toStringMessage().getBytes().length, address, Integer.parseInt(ports));
                    try {
                        s.send(p);
                    } catch (IOException ioe) {
                        ioe.printStackTrace(System.out);
                    }
                    if (!debug)
                    {
                        int iend = ACKmessage.toStringMessage().indexOf("\n");
                        String substring;
                        if (iend != -1)
                        {
                            substring = ACKmessage.toStringMessage().substring(0, iend);
                            System.out.println(substring);
                        }
                    }
                    else System.out.println(ACKmessage.toStringMessage());
                    parts = ((RequestTimeoutMessage) m).getVias().get(0).split(":");
                    try {
                        address = InetAddress.getByName(parts[0]);
                    } catch (UnknownHostException uhe) {
                        uhe.printStackTrace(System.out);
                    }
                    Proxy.procesando=false;
                    ports = parts[1];
                    DatagramPacket p1 = new DatagramPacket(m.toStringMessage().getBytes(), m.toStringMessage().getBytes().length, address, Integer.parseInt(ports));

                    try {
                        s.send(p1);
                    } catch (IOException ioe) {
                        ioe.printStackTrace(System.out);
                    }
                    if (!debug)
                    {
                        int iend = m.toStringMessage().indexOf("\n");
                        String substring;
                        if (iend != -1)
                        {
                            substring = m.toStringMessage().substring(0, iend);
                            System.out.println(substring);
                        }
                    }
                    else System.out.println(m.toStringMessage());
                }
                if (m instanceof OKMessage) {
                    ((OKMessage) m).deleteVia();
                    parts = ((OKMessage) m).getVias().get(0).split(":");
                    try {
                        address = InetAddress.getByName(parts[0]);
                    } catch (UnknownHostException uhe) {
                        uhe.printStackTrace(System.out);
                    }
                    ports = parts[1];
                    DatagramPacket p = new DatagramPacket(m.toStringMessage().getBytes(), m.toStringMessage().getBytes().length, address, Integer.parseInt(ports));

                    try {
                        s.send(p);
                    } catch (IOException ioe) {
                        ioe.printStackTrace(System.out);
                    }
                    if (!debug)
                    {
                        int iend = m.toStringMessage().indexOf("\n");
                        String substring;
                        if (iend != -1)
                        {
                            substring = m.toStringMessage().substring(0, iend);
                            System.out.println(substring);
                        }
                    }
                    else System.out.println(m.toStringMessage());
                    if (!Proxy.looseRouting)
                    {
                        Proxy.procesando=false;
                    }
                }
                if (m instanceof BusyHereMessage) {

                    //Los mensajes que no sean 200 se contestan salto a salto
                    Proxy.cSeq=Integer.parseInt(((BusyHereMessage) m).getcSeqNumber());
                    Proxy.cSeq++;
                    i=0;
                    while (i < lista.size()) {
                        if (lista.get(i).get(0).equals(((BusyHereMessage) m).getToName().toLowerCase())) {
                            indice = i;
                            try {
                                address = InetAddress.getByName(lista.get(indice).get(2).toString());
                            } catch (UnknownHostException uhe) {
                                uhe.printStackTrace(System.out);
                            }
                            ports = lista.get(indice).get(3).toString();
                        }
                        i++;
                    }
                    ((BusyHereMessage) m).deleteVia();
                    ACKMessage ACKmessage = new ACKMessage();
                    ACKmessage.setDestination(((BusyHereMessage) m).getToUri());
                    ACKmessage.setVias(((BusyHereMessage) m).getVias());
                    ACKmessage.setFromName(((BusyHereMessage) m).getFromName());
                    ACKmessage.setFromUri(((BusyHereMessage) m).getFromUri());
                    ACKmessage.setToName(((BusyHereMessage) m).getToName());
                    ACKmessage.setToUri(((BusyHereMessage) m).getToUri());
                    ACKmessage.setCallId(((BusyHereMessage) m).getCallId());
                    ACKmessage.setcSeqNumber(Integer.toString(Proxy.cSeq));
                    ACKmessage.setContentLength(((BusyHereMessage) m).getContentLength());
                    ACKmessage.setcSeqStr("ACK");
                    DatagramPacket p = new DatagramPacket(ACKmessage.toStringMessage().getBytes(), ACKmessage.toStringMessage().getBytes().length, address, Integer.parseInt(ports));
                    try {
                        s.send(p);
                    } catch (IOException ioe) {
                        ioe.printStackTrace(System.out);
                    }
                    if (!debug)
                    {
                        int iend = m.toStringMessage().indexOf("\n");
                        String substring;
                        if (iend != -1)
                        {
                            substring = m.toStringMessage().substring(0, iend);
                            System.out.println(substring);
                        }
                    }
                    else System.out.println(m.toStringMessage());

                    //Borro una vía y le envío el mensaje
                    parts = ((BusyHereMessage) m).getVias().get(0).split(":");
                    try {
                        address = InetAddress.getByName(parts[0]);
                    } catch (UnknownHostException uhe) {
                        uhe.printStackTrace(System.out);
                    }
                    ports = parts[1];
                    DatagramPacket p1 = new DatagramPacket(m.toStringMessage().getBytes(), m.toStringMessage().getBytes().length, address, Integer.parseInt(ports));

                    try {
                        s.send(p1);
                    } catch (IOException ioe) {
                        ioe.printStackTrace(System.out);
                    }
                    if (!debug)
                    {
                        int iend = m.toStringMessage().indexOf("\n");
                        String substring;
                        if (iend != -1)
                        {
                            substring = m.toStringMessage().substring(0, iend);
                            System.out.println(substring);
                        }
                    }
                    else System.out.println(m.toStringMessage());
                    Proxy.procesando=false;
                }
                if (m instanceof ByeMessage)
                {
                    ((ByeMessage) m).addVia(Proxy.addressLocal.toString().substring(1)+":"+Proxy.puertoEscuchaProxy);
                    ByeMessage byeMessage = new ByeMessage();
                    byeMessage.setCallId(((ByeMessage) m).getCallId());
                    byeMessage.setContentLength(((ByeMessage) m).getContentLength());
                    byeMessage.setcSeqNumber(((ByeMessage) m).getcSeqNumber());
                    byeMessage.setcSeqStr(((ByeMessage) m).getcSeqStr());
                    byeMessage.setDestination(((ByeMessage) m).getDestination());
                    byeMessage.setFromName(((ByeMessage) m).getFromName());
                    byeMessage.setFromUri(((ByeMessage) m).getFromUri());
                    byeMessage.setToName(((ByeMessage) m).getToName());
                    byeMessage.setMaxForwards(((ByeMessage) m).getMaxForwards());
                    byeMessage.setVias(((ByeMessage) m).getVias());
                    byeMessage.setToUri(((ByeMessage) m).getToUri());

                    i = 0;
                    while (i < lista.size()) {
                        if (lista.get(i).get(0).equals(((ByeMessage) m).getToName().toLowerCase())) {
                            indice = i;
                            try {
                                address = InetAddress.getByName(lista.get(indice).get(2).toString());
                            } catch (UnknownHostException uhe) {
                                uhe.printStackTrace(System.out);
                            }
                            ports = lista.get(indice).get(3).toString();
                        }
                        i++;
                    }

                    DatagramPacket p1 = new DatagramPacket(byeMessage.toStringMessage().getBytes(), byeMessage.toStringMessage().getBytes().length, address, Integer.parseInt(ports));
                    try {
                       s.send(p1);
                    } catch (IOException ioe) {
                        ioe.printStackTrace(System.out);
                    }
                    if (!debug)
                    {
                        int iend = m.toStringMessage().indexOf("\n");
                        String substring;
                        if (iend != -1)
                        {
                            substring = m.toStringMessage().substring(0, iend);
                            System.out.println(substring);
                        }
                    }
                    else System.out.println(m.toStringMessage());

                }
                if (m instanceof InviteMessage) {
                    if (Proxy.procesando)
                    {
                        envia503(m);

                    }
                    else {
                        Proxy.cSeq = Integer.parseInt(((InviteMessage) m).getcSeqNumber());
                        Proxy.cSeq++;
                        i = 0;
                        while (i < lista.size()) {
                            if (lista.get(i).get(0).equals(((InviteMessage) m).getFromName().toLowerCase())) {
                                indice = i;
                                try {
                                    address = InetAddress.getByName(lista.get(indice).get(2).toString());
                                } catch (UnknownHostException uhe) {
                                    uhe.printStackTrace(System.out);
                                }
                                ports = lista.get(indice).get(3).toString();
                            }
                            i++;
                        }
                        i = 0;
                        while (i < lista.size()) {
                            if (lista.get(i).get(0).equals(((InviteMessage) m).getToName().toLowerCase())) {
                                if (lista.get(i).get(2).equals("")) {
                                    //Esto significa que el usuario todavía no está registrado. Tengo que mandar un notFound
                                    NotFoundMessage notFoundMessage = new NotFoundMessage();
                                    notFoundMessage.setVias(((InviteMessage) m).getVias());
                                    notFoundMessage.setToName(((InviteMessage) m).getToName());
                                    notFoundMessage.setToUri(((InviteMessage) m).getToUri());
                                    notFoundMessage.setFromName(((InviteMessage) m).getFromName());
                                    notFoundMessage.setFromUri(((InviteMessage) m).getFromUri());
                                    notFoundMessage.setCallId(((InviteMessage) m).getCallId());
                                    notFoundMessage.setcSeqNumber(Integer.toString(Proxy.cSeq));
                                    notFoundMessage.setcSeqStr(((InviteMessage) m).getcSeqStr());
                                    notFoundMessage.setContact(((InviteMessage) m).getContact());
                                    notFoundMessage.setExpires(Integer.toString(7200));
                                    notFoundMessage.setContentLength(0);

                                    DatagramPacket p = new DatagramPacket(notFoundMessage.toStringMessage().getBytes(), notFoundMessage.toStringMessage().getBytes().length, address, Integer.parseInt(ports));

                                    try {
                                        s.send(p);
                                    } catch (IOException ioe) {
                                        ioe.printStackTrace(System.out);
                                    }
                                    if (!debug)
                                    {
                                        int iend = notFoundMessage.toStringMessage().indexOf("\n");
                                        String substring;
                                        if (iend != -1)
                                        {
                                            substring = notFoundMessage.toStringMessage().substring(0, iend);
                                            System.out.println(substring);
                                        }
                                    }
                                    else System.out.println(notFoundMessage.toStringMessage());
                                    return;
                                }
                            }
                            i++;
                        }

                        if (((InviteMessage) m).getProxyAuthentication() == null) {
                            i = 0;
                            while (i < lista.size()) {
                                if (lista.get(i).get(0).equals(((InviteMessage) m).getFromName().toLowerCase())) {
                                    indice = i;
                                    try {
                                        address = InetAddress.getByName(lista.get(indice).get(2).toString());
                                    } catch (UnknownHostException uhe) {
                                        uhe.printStackTrace(System.out);
                                    }
                                    ports = lista.get(indice).get(3).toString();
                                }
                                i++;
                            }
                            ProxyAuthenticationMessage proxyAuthenticationMessage = new ProxyAuthenticationMessage();
                            proxyAuthenticationMessage = creaMensajeAutentication(proxyAuthenticationMessage, (InviteMessage) m);
                            numero_secuencia = proxyAuthenticationMessage.getproxyAuthenticate();
                            DatagramPacket p = new DatagramPacket(proxyAuthenticationMessage.toStringMessage().getBytes(), proxyAuthenticationMessage.toStringMessage().getBytes().length, address, Integer.parseInt(ports));
                            try {
                                s.send(p);
                            } catch (IOException ioe) {
                                ioe.printStackTrace(System.out);
                            }
                            if (!debug)
                            {
                                int iend = proxyAuthenticationMessage.toStringMessage().indexOf("\n");
                                String substring;
                                if (iend != -1)
                                {
                                    substring = proxyAuthenticationMessage.toStringMessage().substring(0, iend);
                                    System.out.println(substring);
                                }
                            }
                            else System.out.println(proxyAuthenticationMessage.toStringMessage());
                        }
                        if ((((InviteMessage) m).getProxyAuthentication()) != null) {
                            Proxy.procesando=true;
                            //Aqui es donde hago lo de ver si la autenticacion está bien
                            i = 0;
                            while (i < lista.size()) {
                                if (lista.get(i).get(0).equals(((InviteMessage) m).getFromName())) {
                                    indice = i;
                                }
                                i++;
                            }
                            try {
                                address = InetAddress.getByName(lista.get(indice).get(2).toString());
                            } catch (UnknownHostException uhe) {
                                uhe.printStackTrace(System.out);
                            }
                            ports = lista.get(indice).get(3).toString();
                            try {
                                MessageDigest md = MessageDigest.getInstance("MD5");
                                byte[] dataBytes = (numero_secuencia + lista.get(indice).get(1).toString()).getBytes();
                                int nread = dataBytes.length;
                                md.update(dataBytes, 0, nread);
                                byte[] mdbytes = md.digest();
                                StringBuffer sb = new StringBuffer();
                                for (i = 0; i < mdbytes.length; i++) {
                                    sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
                                }
                                //Si la autenticación está bien, mando un trying a la primera máquina y mando un Invite a la maquina llamada
                                if (((InviteMessage) m).getProxyAuthentication().equals(sb.toString())) {
                                    //Aquí empieza a trabajar el servlet
                                    servletDecide((InviteMessage) m);
                                    if(code==100) {
                                        TryingMessage tryingMessage = new TryingMessage();
                                        tryingMessage.setVias(((InviteMessage) m).getVias());
                                        tryingMessage.setToName(((InviteMessage) m).getToName());
                                        tryingMessage.setFromName(((InviteMessage) m).getFromName());
                                        tryingMessage.setFromUri(((InviteMessage) m).getFromUri());
                                        tryingMessage.setToUri(((InviteMessage) m).getToUri());
                                        tryingMessage.setCallId(((InviteMessage) m).getCallId());
                                        tryingMessage.setcSeqNumber(Integer.toString(Proxy.cSeq));
                                        tryingMessage.setcSeqStr(((InviteMessage) m).getcSeqStr());
                                        tryingMessage.setContentLength(0);
                                        DatagramPacket p = new DatagramPacket(tryingMessage.toStringMessage().getBytes(), tryingMessage.toStringMessage().getBytes().length, address, Integer.parseInt(ports));

                                        try {
                                            s.send(p);
                                        } catch (IOException ioe) {
                                            ioe.printStackTrace(System.out);
                                        }
                                        if (!debug) {
                                            int iend = tryingMessage.toStringMessage().indexOf("\n");
                                            String substring;
                                            if (iend != -1) {
                                                substring = tryingMessage.toStringMessage().substring(0, iend);
                                                System.out.println(substring);
                                            }
                                        } else System.out.println(tryingMessage.toStringMessage());
                                        ArrayList<String> via;
                                        via = ((InviteMessage) m).getVias();
                                        via.add(0, addressLocal.toString().substring(1) + ":" + puertoEscuchaProxy);
                                        InviteMessage message = new InviteMessage();
                                        message.setDestination(((InviteMessage) m).getDestination());
                                        message.setVias(via);
                                        message.setToName(((InviteMessage) m).getToName());
                                        message.setToUri(((InviteMessage) m).getToUri());
                                        message.setContentLength(((InviteMessage) m).getContentLength());
                                        message.setMaxForwards(((InviteMessage) m).getMaxForwards());
                                        message.setCallId(((InviteMessage) m).getCallId());
                                        message.setContact(((InviteMessage) m).getContact());
                                        message.setcSeqNumber(Integer.toString(Proxy.cSeq));
                                        message.setcSeqStr(((InviteMessage) m).getcSeqStr());
                                        message.setSdp(((InviteMessage) m).getSdp());
                                        message.setFromName(((InviteMessage) m).getFromName());
                                        message.setFromUri(((InviteMessage) m).getFromUri());
                                        if (Proxy.looseRouting) {
                                            message.setRecordRoute(addressLocal.toString().substring(1) + ":" + puertoEscuchaProxy);
                                        }

                                        //Tengo que crear un mensaje nuevo sin autenticación para que no me ponga autenticación incorrecta.
                                        i = 0;
                                        while (i < lista.size()) {
                                            if (lista.get(i).get(0).equals(message.getToName().toLowerCase())) {
                                                indice = i;
                                            }
                                            i++;
                                        }
                                        try {
                                            address = InetAddress.getByName(lista.get(indice).get(2).toString());
                                        } catch (UnknownHostException uhe) {
                                            uhe.printStackTrace(System.out);
                                        }
                                        ports = lista.get(indice).get(3).toString();
                                        DatagramPacket p2 = new DatagramPacket(message.toStringMessage().getBytes(), message.toStringMessage().getBytes().length, address, Integer.parseInt(ports));
                                        try {
                                            s.send(p2);
                                        } catch (IOException ioe) {
                                            ioe.printStackTrace(System.out);
                                        }
                                        if (!debug) {
                                            int iend = m.toStringMessage().indexOf("\n");
                                            String substring;
                                            if (iend != -1) {
                                                substring = m.toStringMessage().substring(0, iend);
                                                System.out.println(substring);
                                            }
                                        } else System.out.println(m.toStringMessage());
                                    }else if(code==503){
                                        envia503(m);
                                    }
                                }
                            } catch (NoSuchAlgorithmException nsae) {
                                nsae.printStackTrace(System.out);
                            }
                        }
                    }
                }
            }
            catch (SIPException sipe) {
                sipe.printStackTrace(System.out);
            }
        }

        private void envia503(SIPMessage m) {
            Proxy.cSeq = Integer.parseInt(((InviteMessage) m).getcSeqNumber());
            Proxy.cSeq++;
            ServiceUnavailableMessage serviceUnavailableMessage = new ServiceUnavailableMessage();
            serviceUnavailableMessage.setCallId(((InviteMessage) m).getCallId());
            serviceUnavailableMessage.setContentLength(((InviteMessage) m).getContentLength());
            serviceUnavailableMessage.setcSeqNumber(Integer.toString(Proxy.cSeq));
            serviceUnavailableMessage.setcSeqStr(((InviteMessage) m).getcSeqStr());
            serviceUnavailableMessage.setFromName(((InviteMessage) m).getFromName());
            serviceUnavailableMessage.setToName(((InviteMessage) m).getToName());
            serviceUnavailableMessage.setFromUri(((InviteMessage) m).getFromUri());
            serviceUnavailableMessage.setToUri(((InviteMessage) m).getToUri());
            serviceUnavailableMessage.setVias(((InviteMessage) m).getVias());
            try {
                address = InetAddress.getByName(((InviteMessage) m).getVias().get(0).split(":")[0]);
            } catch (UnknownHostException uhe) {
                uhe.printStackTrace(System.out);
            }
            String ports=((InviteMessage) m).getVias().get(0).split(":")[1];
            DatagramPacket p1 = new DatagramPacket(serviceUnavailableMessage.toStringMessage().getBytes(), serviceUnavailableMessage.toStringMessage().getBytes().length, address, Integer.parseInt(ports));

            try {
                s.send(p1);
            } catch (IOException ioe) {
                ioe.printStackTrace(System.out);
            }
            if (!debug)
            {
                int iend = serviceUnavailableMessage.toStringMessage().indexOf("\n");
                String substring;
                if (iend != -1)
                {
                    substring = serviceUnavailableMessage.toStringMessage().substring(0, iend);
                    System.out.println(substring);
                }
            }
            else System.out.println(serviceUnavailableMessage.toStringMessage());
        }

        private void servletDecide(InviteMessage m) {
            //Leer user.xml y guardar en listas de users y servlets
            UsersServletReader usr = new UsersServletReader();
            Users users = usr.getUsers();
            List<String> usuarios = new ArrayList<String>();
            List<ServletClass> servlets = new ArrayList<ServletClass>();
            for(int i=0;i<users.getListUsers().size();i++){
                usuarios.add(users.getListUsers().get(i).getId());
                servlets.add(users.getListUsers().get(i).getServletClass());
            }
            for(int j=0;j<usuarios.size();j++){
                if(usuarios.get(j).equals(m.getFromUri())){

                    try {
                        Class<?> clase = Class.forName(servlets.get(j).getName());
                        SIPServletInterface servlet = (SIPServletInterface) clase.newInstance();
                        SipServletRequestInterface request = new SipServletRequest(m.getFromUri(),m.getToUri());
                        servlet.doInvite(request);
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }else if (usuarios.get(j).equals(m.getToUri())){
                    try {
                        Class<?> clase = Class.forName(servlets.get(j).getName());
                        SIPServletInterface servlet = (SIPServletInterface) clase.newInstance();
                        SipServletRequestInterface request = new SipServletRequest(m.getFromUri(),m.getToUri());
                        servlet.doInvite(request);
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }else {
                    //No existe en el users.xml
                    System.out.println("\nNo hay servlet asociado ni al llamante ni al llamado\n");
                    code=100;
                }
            }
        }

        private static ProxyAuthenticationMessage creaMensajeAutentication(ProxyAuthenticationMessage mensaje, InviteMessage m) {
            mensaje.setVias(m.getVias());
            mensaje.setToName(m.getToName());
            mensaje.setToUri(m.getToUri());
            mensaje.setFromName(m.getFromName());
            mensaje.setFromUri(m.getFromUri());
            mensaje.setCallId(m.getCallId());
            mensaje.setcSeqNumber(Integer.toString(Proxy.cSeq));
            mensaje.setcSeqStr(m.getcSeqStr());
            Random rand = new Random();
            int min = 1;
            int max = 10000;
            int randomNum = rand.nextInt((max - min) + 1) + min;
            mensaje.setproxyAuthenticate(Integer.toString(randomNum));
            mensaje.setExpires("7200");
            mensaje.setContentLength(m.getContentLength());
            return mensaje;
        }
    }
}
