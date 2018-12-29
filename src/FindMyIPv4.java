import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

public class FindMyIPv4 {
	public static String main() throws SocketException, UnknownHostException {
		return(findMyIPv4Address().getHostAddress());
	}

	/**
	 * Returns my non-loopback IPv4 address. If more than one is found, an
	 * UnknownHostException is raised. If no matching is found, the address of
	 * InetAddress.getLocalhost() is returned.
	 * 
	 * @return
	 * @throws SocketException
	 * @throws UnknownHostException
	 */
	private static Inet4Address findMyIPv4Address() throws SocketException, UnknownHostException {
		Inet4Address myAddr = null;
		Enumeration<NetworkInterface> n = NetworkInterface.getNetworkInterfaces();
		while (n.hasMoreElements()) {
			NetworkInterface e = n.nextElement();
			Enumeration<InetAddress> a = e.getInetAddresses();
			while (a.hasMoreElements()) {
				InetAddress addr = a.nextElement();
				if (!(addr instanceof Inet4Address)) {
					continue;
				}
				if (addr.isLoopbackAddress()) {
					continue;
				}
				if (myAddr != null) {
					throw new UnknownHostException("More than one non-loopback IPv4 address found");
				}
				if (!addr.isLoopbackAddress()) {
					myAddr = (Inet4Address) addr;
					return myAddr;
				}
			}
		}
		if (myAddr == null) {
			InetAddress loopback = InetAddress.getLocalHost();
			if (loopback instanceof Inet4Address) {
				myAddr = (Inet4Address) loopback;
			}
		}
		return myAddr;
	}
}