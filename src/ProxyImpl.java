public class ProxyImpl implements ProxyInterface {

    private Proxy proxy;

    @Override
    public void proxyTo(String uri) {

    }

    public Proxy getProxy() {
        return proxy;
    }

    public void setProxy(Proxy proxy) {
        this.proxy = proxy;
    }
}
