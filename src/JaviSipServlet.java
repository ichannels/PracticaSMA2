import java.util.Calendar;

public class JaviSipServlet implements SIPServletInterface {

    //Tareas del servlet:
    //-Responder con TRYING (hacia UA1) al primer INVITE
    //-Reenviar INVITE al UA2
    //-Recibir RINGING de UA2 y reenviar al UA1
    //-Recibir 200OK/408/486 del UA2 y reenviarlo al UA1
    @Override
    public void doInvite(SipServletRequestInterface request) {
        SipServletResponse response = new SipServletResponse();
        ProxyImpl proxy = new ProxyImpl();
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);

        if(//request.getCallerURI().split(":")[1].equals("boss") &&
                hour>=9 && hour<17) {
            response = (SipServletResponse) request.createResponse(100);
            response.send();
        }
        else{
            response = (SipServletResponse) request.createResponse(503);
            response.send();
        }
    }

}
