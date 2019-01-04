import mensajesSIP.SIPMessage;

public class SipServletRequest implements SipServletRequestInterface {

    private String callerURI;
    private String calleeURI;
    private ProxyImpl proxy;
    private SIPMessage mensaje;

    public SipServletRequest(String callerURI, String calleeURI) {
        this.callerURI = callerURI;
        this.calleeURI = calleeURI;
    }

    public void setCalleeURI(String calleeURI) {
        this.calleeURI = calleeURI;
    }

    public void setCallerURI(String callerURI) {
        this.callerURI = callerURI;
    }

    public void setProxy(ProxyImpl proxy) {
        this.proxy = proxy;
    }

    @Override
    public String getCallerURI() {
        return callerURI;
    }

    @Override
    public String getCalleeURI() {
        return calleeURI;
    }

    @Override
    public SipServletResponseInterface createResponse(int statuscode) {
        SipServletResponse response = new SipServletResponse(statuscode,mensaje);
        return response;
    }

    @Override
    public ProxyInterface getProxy() {
        return null;
    }
}
