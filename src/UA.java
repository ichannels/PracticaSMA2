import mensajesSIP.*;
import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

import static java.lang.System.exit;
import static java.lang.Thread.sleep;

public class UA {
    private static boolean ok = false;
    private static boolean notFound = false;
    private static byte[] buffer = new byte[1000];
    private static boolean llamante;
    private static String pass = null;
    private static String usuarioDestino;
    private static String dominioDestino;
    private static boolean enLlamada;
    private static boolean proceeding;
    private static boolean calling;
    private static boolean completed;
    private static boolean terminated;
    private static int cSeq = 1;
    private static boolean aceptada = false;
    private static boolean rechazada = false;
    private static String usuarioSIP=null;
    private static String addressLocal=null;
    private static String puertoEscuchaUA=null;
    private static String direccion_otro_UA=null;
    private static String puerto_otro_UA=null;
    private static String IPProxy=null;
    private static String puertoEscuchaProxy=null;
    private static Boolean debug=null;
    private static Timer timer = null;
    private static boolean recordRoute=false;
    private static String cadena_record;
    private static String direccion_destino;
    private static String puerto_destino;
    private static String nombreUsuarioDestino=null;
    public static void main(String [] args)
    {
        UA.usuarioSIP = args[0];
        UA.puertoEscuchaUA = args[1];
        UA.IPProxy = args[2];
        UA.puertoEscuchaProxy = args[3];
        UA.debug = Boolean.valueOf(args[4]);
        Hilo1 h1;
        Hilo2 h2;
        Hilo3 h3;
        String parts[];
        String parts2[];
        DatagramSocket s = null;
        InetAddress address = null;
        String usuario;

        //Si el nombre de usuario tiene dos puntos para poner la contraseña, divido en nombre de usuario y contraseña
        if (UA.usuarioSIP.contains(":"))
        {
            parts=UA.usuarioSIP.split(":");
            pass = parts[1];
            usuario = parts[0];
            parts2 = pass.split("@");
            pass = parts2[0];
            usuario = usuario + "@" + parts2[1];

        }
        //Si no, lo dejo tal cual
        else
        {
            usuario = UA.usuarioSIP;
            UA.pass = null;
        }
        //Creo un objeto de tipo socket
        try {
            //Creo un objeto de tipo DatagramSocket
            s = new DatagramSocket(Integer.parseInt(UA.puertoEscuchaUA));
        } catch (SocketException se) {
            se.printStackTrace(System.out);
        }
        //Guardo la dirección como objeto de la clase InetAddress
        try {
            address = InetAddress.getByName(UA.IPProxy);
        } catch (UnknownHostException uhe) {
            uhe.printStackTrace(System.out);
        }

        DatagramPacket p = new DatagramPacket(buffer, buffer.length);
        //Inicio el Hilo 1 (Está encargado de la recepción el OK o del Not Found del Register)
        h1 = new Hilo1(s, p);
        h1.start();

        h3 = new Hilo3(s, p, usuario, address, Integer.parseInt(UA.puertoEscuchaProxy), Integer.parseInt(UA.puertoEscuchaUA), UA.pass);

        if (debug) {
            System.out.println(usuarioSIP);
            System.out.println(puertoEscuchaUA);
            System.out.println(IPProxy);
            System.out.println(puertoEscuchaProxy);
            System.out.println(args[4]);
        }
        //Bucle para enviar el Register cada 2 segundos hasta que se recibe un OK o un Not Found
        while (true) {

            registrar(s, usuario, address, Integer.parseInt(UA.puertoEscuchaProxy), Integer.parseInt(UA.puertoEscuchaUA));
            try{
                sleep(500);
            }
            catch (InterruptedException ie)
            {
                ie.printStackTrace(System.out);
            }
            if (ok)
            {
                break;
            }
            if (notFound)
            {
                System.out.println("Usuario no encontrado, se saldra de la ejecucion\n");
                exit(1);
            }
            try{
                sleep(2000);
            }
            catch (InterruptedException ie)
            {
                ie.printStackTrace(System.out);
            }
        }

        //Se supone que cuando llego aquí ya estoy registrado.
        //En el Hilo2 tengo cosas por teclado

        //Una vez registrado puedo enviar cosas por teclado. Esto lo implementa el Hilo2
        h2 = new Hilo2(s,usuario,address,Integer.parseInt(UA.puertoEscuchaProxy),Integer.parseInt(UA.puertoEscuchaUA));
        h2.start();
        h3.start();
    }

    //Método para registrar usuario en el proxy

    private static void registrar(DatagramSocket s, String usuarioSIP, InetAddress address, int portRemoto, int portLocal) {

        ArrayList<String> via = new ArrayList<>();

        try {
            UA.addressLocal = FindMyIPv4.main();
        }
        catch (SocketException | UnknownHostException se)
        {
            se.printStackTrace(System.out);
        }
        via.add(UA.addressLocal+ ":" + Integer.toString(portLocal));
        //Divido el usuario en dos partes, nombre y dominio

        String[] parts = usuarioSIP.split("@");
        String usuario = parts[0]; //
        String dominio = "@" + parts[1]; //

        //Creo el mensaje Register con todos los parámetros necesarios
        RegisterMessage registerMessage = new RegisterMessage();
        registerMessage.setDestination("sip:" + usuario + dominio);
        registerMessage.setVias(via);
        registerMessage.setMaxForwards(70);
        registerMessage.setToName(usuario.substring(0, 1).toUpperCase() + usuario.substring(1));
        registerMessage.setToUri("sip:" + usuario + dominio);
        registerMessage.setFromName(usuario.substring(0, 1).toUpperCase() + usuario.substring(1));
        registerMessage.setFromUri("sip:" + usuario + dominio);

        Random rand = new Random();
        int min = 1;
        int max = 10000;
        int randomNum = rand.nextInt((max - min) + 1) + min;

        registerMessage.setCallId(Integer.toString(randomNum));
        registerMessage.setcSeqNumber(Integer.toString(cSeq));
        registerMessage.setcSeqStr("REGISTER");
        registerMessage.setContact(UA.addressLocal+":"+Integer.toString(portLocal));
        registerMessage.setExpires("7200");
        registerMessage.setContentLength(0);

        //Prueba de que funciona el mensaje
        try {
            if (!debug)
            {
                int iend = registerMessage.toStringMessage().indexOf("\n");
                String substring;
                if (iend != -1)
                {
                    substring = registerMessage.toStringMessage().substring(0, iend);
                    System.out.println(substring);
                }
            }
            else System.out.println(registerMessage.toStringMessage());

        }
        catch (NullPointerException npe)
        {
            npe.printStackTrace(System.out);
        }

        DatagramPacket p = new DatagramPacket(registerMessage.toStringMessage().getBytes(), registerMessage.toStringMessage().getBytes().length , address, portRemoto);

        try{
            s.send(p);
        }
        catch (IOException ioe)
        {
            ioe.printStackTrace(System.out);
        }

    }

    //Hilo encargado de crear el socket
    public static class Hilo1 extends Thread {
        //Objeto de tipo Socket
        DatagramSocket s;
        DatagramPacket p;
        String mensaje;
        //Buffer donde recibe las cosas del socket
        //Constructor del hilo con el objeto de tipo socket
        Hilo1(DatagramSocket s, DatagramPacket p) {
            this.s = s;
            this.p = p;
        }

        public void run() {
            try {
                //Creo un objeto de tipo paquete
                /*Recibo en un bucle las cosas del bufer. Se tendría que bloquear
                cuando invoco el método receive, pero no se bloquea porque sólo se bloquea
                el hilo
                */
                    s.receive(p);
                    mensaje = new String(UA.buffer, 0, p.getLength());
            } catch (IOException ioe) {
                ioe.printStackTrace(System.out);
            }
            try{
                SIPMessage m = SIPMessage.parseMessage(mensaje);
                if (m instanceof OKMessage)
                {
                    ok = true;
                    UA.cSeq=Integer.parseInt(((OKMessage) m).getcSeqNumber());
                    UA.cSeq++;
                }
                else {
                    notFound = true;
                }
            }
            catch (SIPException sipe)
            {
                sipe.printStackTrace(System.out);
            }
        }
    }
    //Hilo que hace cosas por teclado. A lo mejor con dejar el main a su bola valdría.
    public static class Hilo2 extends Thread {
        DatagramSocket s;
        String usuarioSIPorigen;
        String usuarioSIPdestino;
        String ports;
        InetAddress address;
        int portRemoto;
        int portLocal;
        Hilo2(DatagramSocket s, String usuarioSIPorigen, InetAddress address, int portRemoto, int portLocal) {
            this.s = s;
            this.usuarioSIPorigen = usuarioSIPorigen;
            this.address = address;
            this.portRemoto = portRemoto;
            this.portLocal = portLocal;
        }
        public void run() {
                /*
                    He probado aquí a utilizar el otro hilo para poner cosas por teclado
                    ya que voy a necesitar lo mismo aunque luego tenga que mandar cosas por
                    el socket.
                 */
            /*Este hilo deja ir poniendo cosas por teclado");*/
            while (true) {

                Scanner scanner = new Scanner(System.in);
                String mensaje = scanner.nextLine();

                if (mensaje.matches("S") || mensaje.matches("s"))
                {
                    UA.aceptada = true;
                }
                else if (mensaje.matches("N") || mensaje.matches("n"))
                {
                    UA.rechazada = true;
                }
                else if ((mensaje.contains("BYE") || (mensaje.contains("bye")))) {//Procesar mensaje BYE
                    ByeMessage byeMessage = new ByeMessage();
                    byeMessage = creaMensajeBye(byeMessage);
                    try {
                        if (!debug)
                        {
                            int iend = byeMessage.toStringMessage().indexOf("\n");
                            String substring;
                            if (iend != -1)
                            {
                                substring = byeMessage.toStringMessage().substring(0, iend);
                                System.out.println(substring);
                            }
                        }
                        else System.out.println(byeMessage.toStringMessage());
                    } catch (NullPointerException npe) {
                        npe.printStackTrace(System.out);
                    }
                    try {
                        address = InetAddress.getByName(UA.direccion_otro_UA);
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                    ports = UA.puerto_otro_UA;
                    if (UA.recordRoute)
                    {
                        try {
                            address = InetAddress.getByName(UA.IPProxy);
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        }
                        ports = UA.puertoEscuchaProxy;
                    }
                    DatagramPacket p = new DatagramPacket(byeMessage.toStringMessage().getBytes(), byeMessage.toStringMessage().getBytes().length, address, Integer.parseInt(ports));
                    try {
                        s.send(p);
                    } catch (IOException ioe) {
                        ioe.printStackTrace(System.out);
                    }
                }
                else if (mensaje.contains("INVITE")) {//Procesar mensaje INVITE
                        UA.llamante=true;
                        String[] parts = mensaje.split(" ");
                        usuarioSIPdestino = parts[1];
                        InviteMessage inviteMessage = new InviteMessage();
                        inviteMessage = creaMensajeInvite(inviteMessage);
                        //Prueba de que funciona el mensaje
                        try {
                            if (!debug)
                            {
                                int iend = inviteMessage.toStringMessage().indexOf("\n");
                                String substring;
                                if (iend != -1)
                                {
                                    substring = inviteMessage.toStringMessage().substring(0, iend);
                                    System.out.println(substring);
                                }
                            }
                            else System.out.println(inviteMessage.toStringMessage());
                        } catch (NullPointerException npe) {
                            npe.printStackTrace(System.out);
                        }
                        try {
                            address = InetAddress.getByName(UA.IPProxy);
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        }
                        ports = UA.puertoEscuchaProxy;
                        DatagramPacket p = new DatagramPacket(inviteMessage.toStringMessage().getBytes(), inviteMessage.toStringMessage().getBytes().length, address, Integer.parseInt(ports));

                        try {
                            s.send(p);
                        } catch (IOException ioe) {
                            ioe.printStackTrace(System.out);
                        }
                    }  else {
                        System.out.println("Tiene que tener un formato INVITE + nombre:pass@dominio.com");
                    }
                }
        }
        private InviteMessage creaMensajeInvite(InviteMessage inviteMessage){
            ArrayList<String> via = new ArrayList<>();

            try {
                UA.addressLocal = FindMyIPv4.main();
            }
            catch (SocketException | UnknownHostException se) {
                se.printStackTrace(System.out);
            }
            via.add(UA.addressLocal+ ":" + Integer.toString(portLocal));
            //Divido el usuario en dos partes, nombre y dominio

            String[] partsOrigen = usuarioSIPorigen.split("@");
            String usuarioOrigen = partsOrigen[0]; //
            String dominioOrigen = "@"+ partsOrigen[1]; //
            String[] partsDestino = usuarioSIPdestino.split("@");
            UA.usuarioDestino = partsDestino[0];
            UA.dominioDestino = "@" + partsDestino[1];
            inviteMessage.setVias(via);
            inviteMessage.setMaxForwards(70);
            inviteMessage.setFromName(usuarioOrigen.substring(0,1).toUpperCase() + usuarioOrigen.substring(1));
            inviteMessage.setFromUri("sip:" + usuarioOrigen + dominioOrigen);
            inviteMessage.setProxyAuthentication(null);
            //Solo quiero hacer esto si el INVITE lleva una contraseña
            inviteMessage.setToName(UA.usuarioDestino.substring(0, 1).toUpperCase() + UA.usuarioDestino.substring(1));
            inviteMessage.setToUri("sip:" + UA.usuarioDestino + UA.dominioDestino);
            inviteMessage.setDestination("sip:" + UA.usuarioDestino + UA.dominioDestino);
            Random rand = new Random();
            int min = 1;
            int max = 10000;
            int randomNum = rand.nextInt((max - min) + 1) + min;

            inviteMessage.setCallId(Integer.toString(randomNum));
            inviteMessage.setcSeqNumber(Integer.toString(UA.cSeq));
            inviteMessage.setcSeqStr("INVITE");
            inviteMessage.setContact(UA.addressLocal+":"+portLocal);
            //inviteMessage.setContact(usuarioOrigen+"@"+addressLocal);
            inviteMessage.setContentType("application/sdp");
            inviteMessage.setContentLength(0);

            SDPMessage sdp = new SDPMessage();
            //Hola
            ArrayList<Integer> options = new ArrayList<>();
            options.add(96);
            options.add(97);
            options.add(98);
            sdp.setOptions(options);
            sdp.setIp(UA.addressLocal);
            sdp.setPort(portLocal);
            inviteMessage.setSdp(sdp);

            return inviteMessage;
        }

        private ByeMessage creaMensajeBye(ByeMessage byeMessage) {
            ArrayList<String> via = new ArrayList<>();

            try {
                UA.addressLocal = FindMyIPv4.main();
            }
            catch (SocketException | UnknownHostException se)
            {
                se.printStackTrace(System.out);
            }
            via.add(UA.addressLocal+ ":" + Integer.toString(portLocal));
            //Divido el usuario en dos partes, nombre y dominio

            String[] partsOrigen = usuarioSIPorigen.split("@");
            String usuarioOrigen = partsOrigen[0]; //
            String dominioOrigen = "@"+ partsOrigen[1]; //

            byeMessage.setVias(via);
            byeMessage.setMaxForwards(70);
            byeMessage.setToName(UA.nombreUsuarioDestino);
            byeMessage.setToUri(UA.usuarioDestino);
            byeMessage.setFromName(usuarioOrigen.substring(0,1).toUpperCase() + usuarioOrigen.substring(1));
            byeMessage.setFromUri("sip:" + usuarioOrigen + dominioOrigen);
            Random rand = new Random();
            int min = 1;
            int max = 10000;
            int randomNum = rand.nextInt((max - min) + 1) + min;
            byeMessage.setCallId(Integer.toString(randomNum));
            byeMessage.setcSeqNumber(Integer.toString(UA.cSeq));
            byeMessage.setcSeqStr("BYE");
            byeMessage.setContentLength(0);
            if (UA.recordRoute) byeMessage.setRoute(UA.addressLocal + ":" + UA.puertoEscuchaUA);
            byeMessage.setDestination(usuarioDestino);

            return byeMessage;
        }
    }
    public static class Hilo3 extends Thread {
        private final InetAddress address;
        private final int portRemoto;
        private final int portLocal;
        private final String usuario;
        String pass;
        //Objeto de tipo Socket
        DatagramSocket s;
        DatagramPacket p;
        String mensaje;
        String parts[]=null;
        InetAddress direccion = null;
        //Buffer donde recibe las cosas del socket
        //Constructor del hilo con el objeto de tipo socket
        Hilo3(DatagramSocket s, DatagramPacket p, String usuario, InetAddress address, int portRemoto, int portLocal, String pass) {
            this.s = s;
            this.p = p;
            this.address = address;
            this.portRemoto = portRemoto;
            this.portLocal = portLocal;
            this.usuario = usuario;
            this.pass = pass;
        }

        public void run() {
            while (true) {
                if (UA.completed)
                {
                    UA.timer = new Timer (1000, null);
                    UA.timer.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            UA.terminated=true;
                            UA.completed=false;
                            UA.timer.stop();
                        }
                    });
                    UA.timer.start();
                }
                try {
                    //Creo un objeto de tipo paquete
                /*Recibo en un bucle las cosas del bufer. Se tendría que bloquear
                cuando invoco el método receive, pero no se bloquea porque sólo se bloquea
                el hilo
                */
                    s.receive(p);
                    mensaje = new String(UA.buffer, 0, p.getLength());
                } catch (IOException ioe) {
                    ioe.printStackTrace(System.out);
                }
                try {
                    SIPMessage m = SIPMessage.parseMessage(mensaje);
                    try{
                        sleep(100);
                    }
                    catch (InterruptedException ie)
                    {
                        ie.printStackTrace(System.out);
                    }
                    if (m instanceof OKMessage) {
                        if (UA.calling)
                        {
                            UA.calling=false;
                            UA.terminated=true;
                            try{
                                sleep(500);
                            }
                            catch (InterruptedException ie)
                            {
                                ie.printStackTrace(System.out);
                            }

                        }
                        if (UA.proceeding)
                        {
                            UA.proceeding=false;
                            UA.terminated=true;
                            try{
                                sleep(500);
                            }
                            catch (InterruptedException ie)
                            {
                                ie.printStackTrace(System.out);
                            }
                        }
                        if (UA.terminated)
                        {

                        }
                        else {
                            UA.cSeq = Integer.parseInt(((OKMessage) m).getcSeqNumber());
                            UA.cSeq++;
                            if (UA.llamante) {
                                parts = ((OKMessage) m).getVias().get(0).split(":");
                                UA.direccion_otro_UA = parts[0];
                                UA.puerto_otro_UA = parts[1];
                            }
                            try {
                                direccion = InetAddress.getByName(parts[0]);
                            } catch (UnknownHostException e) {
                                e.printStackTrace();
                            }
                            UA.direccion_destino = parts[0];
                            UA.puerto_destino = parts[1];
                            ACKMessage ackMessage = new ACKMessage();
                            ArrayList<String> via = new ArrayList<>();
                            via.add(0, UA.addressLocal + ":" + UA.puertoEscuchaUA);
                            ackMessage.setVias(via);
                            ackMessage.setFromName(((OKMessage) m).getFromName());
                            ackMessage.setFromUri(((OKMessage) m).getFromUri());
                            ackMessage.setToUri(((OKMessage) m).getToUri());
                            ackMessage.setToName(((OKMessage) m).getToName());
                            ackMessage.setMaxForwards(76);
                            ackMessage.setCallId(((OKMessage) m).getCallId());
                            ackMessage.setcSeqStr("ACK");
                            ackMessage.setcSeqNumber(Integer.toString(UA.cSeq));
                            ackMessage.setContentLength(((OKMessage) m).getContentLength());
                            ackMessage.setDestination(((OKMessage) m).getToUri());
                            if (UA.recordRoute) {
                                UA.cadena_record = ((OKMessage) m).getRecordRoute();
                                ackMessage.setRoute(((OKMessage) m).getRecordRoute());
                                parts = ((OKMessage) m).getRecordRoute().split(":");
                                try {
                                    direccion = InetAddress.getByName(parts[0]);
                                } catch (UnknownHostException e) {
                                    e.printStackTrace();
                                }
                            }
                            DatagramPacket p = new DatagramPacket(ackMessage.toStringMessage().getBytes(), ackMessage.toStringMessage().getBytes().length, direccion, Integer.parseInt(parts[1]));
                            try {
                                s.send(p);
                            } catch (IOException ioe) {
                                ioe.printStackTrace(System.out);
                            }
                            if (!debug)
                            {
                                int iend = ackMessage.toStringMessage().indexOf("\n");
                                String substring;
                                if (iend != -1)
                                {
                                    substring = ackMessage.toStringMessage().substring(0, iend);
                                    System.out.println(substring);
                                }
                            }
                            else System.out.println(ackMessage.toStringMessage());
                        }
                    }
                    if (m instanceof BusyHereMessage) {
                        if (UA.calling)
                        {
                            UA.calling=false;
                            UA.completed=true;
                            try{
                                sleep(500);
                            }
                            catch (InterruptedException ie)
                            {
                                ie.printStackTrace(System.out);
                            }
                        }
                        if (UA.proceeding)
                        {
                            UA.proceeding=false;
                            UA.completed=true;
                            try{
                                sleep(500);
                            }
                            catch (InterruptedException ie)
                            {
                                ie.printStackTrace(System.out);
                            }
                        }
                        UA.cSeq=Integer.parseInt(((BusyHereMessage) m).getcSeqNumber());
                        UA.cSeq++;
                        ACKMessage ackMessage = new ACKMessage();
                        ackMessage.setVias(((BusyHereMessage) m).getVias());
                        ackMessage.setFromName(((BusyHereMessage) m).getFromName());
                        ackMessage.setFromUri(((BusyHereMessage) m).getFromUri());
                        ackMessage.setToUri(((BusyHereMessage) m).getToUri());
                        ackMessage.setFromUri(((BusyHereMessage) m).getFromUri());
                        ackMessage.setMaxForwards(76);
                        ackMessage.setCallId(((BusyHereMessage) m).getCallId());
                        ackMessage.setcSeqStr("ACK");
                        ackMessage.setcSeqNumber(Integer.toString(UA.cSeq));
                        ackMessage.setContentLength(((BusyHereMessage) m).getContentLength());
                        try {
                            direccion = InetAddress.getByName(UA.IPProxy);
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        }
                        ackMessage.setDestination(((BusyHereMessage) m).getToUri());
                        DatagramPacket p = new DatagramPacket(ackMessage.toStringMessage().getBytes(), ackMessage.toStringMessage().getBytes().length, direccion, Integer.parseInt(UA.puertoEscuchaProxy));
                        try {
                            s.send(p);
                        } catch (IOException ioe) {
                            ioe.printStackTrace(System.out);
                        }
                        if (!debug)
                        {
                            int iend = ackMessage.toStringMessage().indexOf("\n");
                            String substring;
                            if (iend != -1)
                            {
                                substring = ackMessage.toStringMessage().substring(0, iend);
                                System.out.println(substring);
                            }
                        }
                        else System.out.println(ackMessage.toStringMessage());
                    }
                    if (m instanceof TryingMessage)
                    {
                        if (UA.calling)
                        {
                            UA.calling=false;
                            UA.proceeding=true;
                            try{
                                sleep(500);
                            }
                            catch (InterruptedException ie)
                            {
                                ie.printStackTrace(System.out);
                            }
                        }
                    }
                    if (m instanceof RingingMessage)
                    {
                        UA.cadena_record=((RingingMessage) m).getRecordRoute();
                        if (UA.calling)
                        {
                            UA.calling=false;
                            UA.proceeding=true;
                            try{
                                sleep(500);
                            }
                            catch (InterruptedException ie)
                            {
                                ie.printStackTrace(System.out);
                            }
                        }
                    }
                    if (m instanceof RequestTimeoutMessage) {
                        UA.cSeq=Integer.parseInt(((RequestTimeoutMessage) m).getcSeqNumber());
                        UA.cSeq++;

                        ACKMessage ackMessage = new ACKMessage();
                        ackMessage.setVias(((RequestTimeoutMessage) m).getVias());
                        ackMessage.setFromName(((RequestTimeoutMessage) m).getFromName());
                        ackMessage.setFromUri(((RequestTimeoutMessage) m).getFromUri());
                        ackMessage.setToUri(((RequestTimeoutMessage) m).getToUri());
                        ackMessage.setFromUri(((RequestTimeoutMessage) m).getFromUri());
                        ackMessage.setMaxForwards(76);
                        ackMessage.setCallId(((RequestTimeoutMessage) m).getCallId());
                        ackMessage.setcSeqStr("ACK");
                        ackMessage.setcSeqNumber(Integer.toString(UA.cSeq));
                        ackMessage.setContentLength(((RequestTimeoutMessage) m).getContentLength());

                        try {
                            direccion = InetAddress.getByName(UA.IPProxy);
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        }
                        ackMessage.setDestination(((RequestTimeoutMessage) m).getToUri());
                        DatagramPacket p = new DatagramPacket(ackMessage.toStringMessage().getBytes(), ackMessage.toStringMessage().getBytes().length, direccion, Integer.parseInt(UA.puertoEscuchaProxy));
                        try {
                            s.send(p);
                        } catch (IOException ioe) {
                            ioe.printStackTrace(System.out);
                        }
                        if (!debug)
                        {
                            int iend = ackMessage.toStringMessage().indexOf("\n");
                            String substring;
                            if (iend != -1)
                            {
                                substring = ackMessage.toStringMessage().substring(0, iend);
                                System.out.println(substring);
                            }
                        }
                        else System.out.println(ackMessage.toStringMessage());
                    }
                    if (m instanceof ProxyAuthenticationMessage) {
                        UA.cSeq=Integer.parseInt(((ProxyAuthenticationMessage) m).getcSeqNumber());
                        UA.cSeq++;
                        InviteMessage inviteMessage = new InviteMessage();
                        inviteMessage = creaMensajeInviteSegundo(inviteMessage, (ProxyAuthenticationMessage) m);
                        DatagramPacket p = new DatagramPacket(inviteMessage.toStringMessage().getBytes(), inviteMessage.toStringMessage().getBytes().length, address, portRemoto);
                        try {
                            s.send(p);
                        } catch (IOException ioe) {
                            ioe.printStackTrace(System.out);
                        }
                        try {
                            if (!debug)
                            {
                                int iend = inviteMessage.toStringMessage().indexOf("\n");
                                String substring;
                                if (iend != -1)
                                {
                                    substring = inviteMessage.toStringMessage().substring(0, iend);
                                    System.out.println(substring);
                                }
                            }
                            else System.out.println(inviteMessage.toStringMessage());
                        } catch (NullPointerException npe) {
                            npe.printStackTrace(System.out);
                        }
                    }
                    if (m instanceof ACKMessage)
                    {
                        if (UA.completed)
                        {
                            UA.completed=false;
                            UA.terminated=true;
                            UA.timer.stop();
                            try{
                                sleep(500);
                            }
                            catch (InterruptedException ie)
                            {
                                ie.printStackTrace(System.out);
                            }
                        }
                    }
                    if (m instanceof ByeMessage)
                    {
                        UA.terminated=true;
                        try{
                            sleep(500);
                        }
                        catch (InterruptedException ie)
                        {
                            ie.printStackTrace(System.out);
                        }
                        OKMessage message = new OKMessage();
                        message.setVias(((ByeMessage) m).getVias());
                        message.setToName(((ByeMessage) m).getToName());
                        message.setToUri(((ByeMessage) m).getToUri());
                        message.setContentLength(((ByeMessage) m).getContentLength());
                        message.setCallId(((ByeMessage) m).getCallId());
                        message.setcSeqNumber(Integer.toString(UA.cSeq));
                        message.setcSeqStr(((ByeMessage) m).getcSeqStr());
                        message.setFromName(((ByeMessage) m).getFromName());
                        message.setFromUri(((ByeMessage) m).getFromUri());
                        String [] parts = ((ByeMessage) m).getVias().get(0).split(":");
                        InetAddress address = null;
                        try {
                            address = InetAddress.getByName(parts[0]);
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        }
                        DatagramPacket p = new DatagramPacket(message.toStringMessage().getBytes(), message.toStringMessage().getBytes().length, address, Integer.parseInt(parts[1]));
                        try {
                            s.send(p);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        if (!debug)
                        {
                            int iend = message.toStringMessage().indexOf("\n");
                            String substring;
                            if (iend != -1)
                            {
                                substring = message.toStringMessage().substring(0, iend);
                                System.out.println(substring);
                            }
                        }
                        else System.out.println(message.toStringMessage());
                    }
                    if (m instanceof InviteMessage)
                    {
                        if (!UA.llamante)
                        {
                            UA.usuarioDestino=((InviteMessage) m).getFromUri();
                            UA.nombreUsuarioDestino=((InviteMessage) m).getFromName();
                            parts=((InviteMessage) m).getVias().get(0).split(":");
                            UA.direccion_otro_UA = parts[0];
                            UA.puerto_otro_UA = parts[1];
                        }
                        UA.cSeq=Integer.parseInt(((InviteMessage) m).getcSeqNumber());
                        UA.cSeq++;
                        if(((InviteMessage) m).getRecordRoute()!=null)
                        {
                            UA.recordRoute=true;
                        }
                        if (UA.enLlamada)
                        {
                            //Busy
                            envia486((InviteMessage) m);
                        }
                        else {
                            crearRinging((InviteMessage) m);
                            UA.calling=true;
                            try{
                                sleep(500);
                            }
                            catch (InterruptedException ie)
                            {
                                ie.printStackTrace(System.out);
                            }
                            System.out.println("Estas recibiendo una llamada de " + ((InviteMessage) m).getFromUri().split(":")[1]
                                    + " Deseas contestar? (S/N)");
                            Timer timer = new Timer (10000, null);
                            timer.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    if (!UA.rechazada && !UA.aceptada)
                                    {
                                        envia408((InviteMessage) m);
                                    }
                                    timer.stop();
                                }
                            });
                            timer.start();
                            while (true) {
                                try{
                                    sleep(500);
                                }
                                catch (InterruptedException ie)
                                {
                                    ie.printStackTrace(System.out);
                                }
                                if (UA.rechazada) {
                                    //Busy
                                    envia486((InviteMessage) m);
                                    timer.stop();
                                    break;
                                } else if (UA.aceptada) {
                                    //OK
                                    envia200OK((InviteMessage) m);
                                    UA.enLlamada = true;
                                    timer.stop();
                                    break;
                                }
                            }
                        }
                    }
                } catch (SIPException sipe) {
                    sipe.printStackTrace(System.out);
                }
            }
        }
        void crearRinging(InviteMessage message)
        {
            RingingMessage ringingMessage = new RingingMessage();
            ringingMessage.setVias(message.getVias());
            ringingMessage.setCallId(message.getCallId());
            ringingMessage.setContact(message.getContact());
            ringingMessage.setContentLength(message.getContentLength());
            ringingMessage.setcSeqNumber(Integer.toString(UA.cSeq));
            ringingMessage.setcSeqStr(message.getcSeqStr());
            ringingMessage.setFromName(message.getFromName());
            ringingMessage.setToUri(message.getToUri());
            ringingMessage.setFromUri(message.getFromUri());
            ringingMessage.setToName(message.getToName());
            if (UA.recordRoute) ringingMessage.setRecordRoute(message.getRecordRoute());
            String via = ringingMessage.getVias().get(0);
            String parts[] = via.split(":");
            String direccion = parts[0];
            String ports = parts[1];
            InetAddress address = null;
            try {
                address = InetAddress.getByName(direccion);
            } catch (UnknownHostException uhe) {
                uhe.printStackTrace(System.out);
            }
            DatagramPacket p = new DatagramPacket(ringingMessage.toStringMessage().getBytes(), ringingMessage.toStringMessage().getBytes().length, address, Integer.parseInt(ports));
            try {
                s.send(p);
            } catch (IOException ioe) {
                ioe.printStackTrace(System.out);
            }
            if (!debug)
            {
                int iend = ringingMessage.toStringMessage().indexOf("\n");
                String substring;
                if (iend != -1)
                {
                    substring = ringingMessage.toStringMessage().substring(0, iend);
                    System.out.println(substring);
                }
            }
            else System.out.println(ringingMessage.toStringMessage());
        }
        void envia200OK(InviteMessage m){
            OKMessage message = new OKMessage();
            message.setVias(m.getVias());
            message.setToName(m.getToName());
            message.setToUri(m.getToUri());
            message.setContentLength(m.getContentLength());
            message.setCallId(m.getCallId());
            message.setContact(m.getContact());
            message.setcSeqNumber(Integer.toString(UA.cSeq));
            message.setcSeqStr(m.getcSeqStr());
            message.setFromName(m.getFromName());
            message.setFromUri(m.getFromUri());
            if (UA.recordRoute) message.setRecordRoute(m.getRecordRoute());
            String [] parts = m.getVias().get(0).split(":");
            InetAddress address = null;
            try {
                address = InetAddress.getByName(parts[0]);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            DatagramPacket p = new DatagramPacket(message.toStringMessage().getBytes(), message.toStringMessage().getBytes().length, address, Integer.parseInt(parts[1]));
            try {
                s.send(p);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (!debug)
            {
                int iend = message.toStringMessage().indexOf("\n");
                String substring;
                if (iend != -1)
                {
                    substring = message.toStringMessage().substring(0, iend);
                    System.out.println(substring);
                }
            }
            else System.out.println(message.toStringMessage());

        }
        void envia486(InviteMessage m){
            ArrayList<String> via;
            via = m.getVias();
            BusyHereMessage message = new BusyHereMessage();
            message.setVias(via);
            message.setToName(m.getToName());
            message.setToUri(m.getToUri());
            message.setContentLength(m.getContentLength());
            message.setCallId(m.getCallId());
            message.setcSeqNumber(m.getcSeqNumber());
            message.setcSeqStr(m.getcSeqStr());
            message.setFromName(m.getFromName());
            message.setFromUri(m.getFromUri());
            String [] parts = via.get(0).split(":");
            InetAddress address = null;
            try {
                address = InetAddress.getByName(parts[0]);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            DatagramPacket p = new DatagramPacket(message.toStringMessage().getBytes(), message.toStringMessage().getBytes().length, address, Integer.parseInt(parts[1]));
            try {
                s.send(p);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (!debug)
            {
                int iend = message.toStringMessage().indexOf("\n");
                String substring;
                if (iend != -1)
                {
                    substring = message.toStringMessage().substring(0, iend);
                    System.out.println(substring);
                }
            }
            else System.out.println(message.toStringMessage());
        }
        void envia408(InviteMessage m){
            ArrayList<String> via = m.getVias();
            RequestTimeoutMessage message = new RequestTimeoutMessage();
            message.setVias(via);
            message.setToName(m.getToName());
            message.setToUri(m.getToUri());
            message.setContentLength(m.getContentLength());
            message.setCallId(m.getCallId());
            message.setcSeqNumber(m.getcSeqNumber());
            message.setcSeqStr(m.getcSeqStr());
            message.setFromName(m.getFromName());
            message.setFromUri(m.getFromUri());
            String [] parts = via.get(0).split(":");
            InetAddress address = null;
            try {
                address = InetAddress.getByName(parts[0]);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            DatagramPacket p = new DatagramPacket(message.toStringMessage().getBytes(), message.toStringMessage().getBytes().length, address, Integer.parseInt(parts[1]));
            try {
                s.send(p);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (!debug)
            {
                int iend = message.toStringMessage().indexOf("\n");
                String substring;
                if (iend != -1)
                {
                    substring = message.toStringMessage().substring(0, iend);
                    System.out.println(substring);
                }
            }
            else System.out.println(message.toStringMessage());
        }
        private InviteMessage creaMensajeInviteSegundo(InviteMessage mensaje,ProxyAuthenticationMessage m) {
            ArrayList<String> via = new ArrayList<>();
            String[] partsPass;

            try {
                UA.addressLocal = FindMyIPv4.main();
            }
            catch (SocketException | UnknownHostException se)
            {
                se.printStackTrace(System.out);
            }

            via.add(UA.addressLocal+ ":" + Integer.toString(portLocal));
            //Divido el usuario en dos partes, nombre y dominio

            String[] partsOrigen = usuario.split("@");
            String usuarioOrigen = partsOrigen[0]; //
            String dominioOrigen = "@"+ partsOrigen[1]; //
            String[] partsDestino = UA.usuarioDestino.split("@");
            String usuarioDestino1 = partsDestino[0];
            if (usuarioDestino1.contains(":")) {
                partsPass = usuarioDestino1.split(":");
                usuarioDestino = partsPass[0];
                pass = partsPass[1];
                try{
                    mensaje.setToName(UA.usuarioDestino.substring(0, 1).toUpperCase() + UA.usuarioDestino.substring(1));
                    mensaje.setToUri("sip:" + UA.usuarioDestino + UA.dominioDestino);
                    mensaje.setDestination(UA.usuarioDestino + UA.dominioDestino);
                } catch(Exception e){e.printStackTrace(System.out);}
            }
            mensaje.setVias(via);
            mensaje.setMaxForwards(70);
            mensaje.setFromName(usuarioOrigen.substring(0,1).toUpperCase() + usuarioOrigen.substring(1));
            mensaje.setFromUri("sip:" + usuarioOrigen + dominioOrigen);
            //Solo quiero hacer esto si el INVITE lleva una contraseña
            if (!usuarioDestino1.contains(":")){
                mensaje.setToName(usuarioDestino1.substring(0, 1).toUpperCase() + usuarioDestino1.substring(1));
                mensaje.setToUri("sip:" + usuarioDestino1 + UA.dominioDestino);
                mensaje.setDestination("sip:" + UA.usuarioDestino + UA.dominioDestino);
            }
            Random rand = new Random();
            int min = 1;
            int max = 10000;
            int randomNum = rand.nextInt((max - min) + 1) + min;

            mensaje.setCallId(Integer.toString(randomNum));
            mensaje.setcSeqNumber(Integer.toString(UA.cSeq));
            mensaje.setcSeqStr("INVITE");
            mensaje.setContact(UA.addressLocal+":"+portLocal);
            mensaje.setContentType("application/sdp");
            mensaje.setContentLength(0);

            SDPMessage sdp = new SDPMessage();
            ArrayList<Integer> options = new ArrayList<>();
            options.add(96);
            options.add(97);
            options.add(98);
            sdp.setOptions(options);
            sdp.setIp(UA.addressLocal);
            sdp.setPort(portLocal);
            mensaje.setSdp(sdp);
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] dataBytes = ((m.getproxyAuthenticate() + pass).getBytes());
                int nread = dataBytes.length;
                md.update(dataBytes, 0, nread);
                byte[] mdbytes = md.digest();

                //convert the byte to hex format
                StringBuffer sb = new StringBuffer();
                for (byte mdbyte : mdbytes) {
                    sb.append(Integer.toString((mdbyte & 0xff) + 0x100, 16).substring(1));
                }
                mensaje.setProxyAuthentication(sb.toString());
            }
            catch (NoSuchAlgorithmException nsae)
                {
                    nsae.printStackTrace(System.out);
                }
            mensaje.setContentLength(m.getContentLength());
            return mensaje;
        }
    }
}
