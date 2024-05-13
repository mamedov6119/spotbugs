package avoidClientSideLocking;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class IPAddressList {
  private final List<InetAddress> ips =
      Collections.synchronizedList(new ArrayList<InetAddress>());
 
  public List<InetAddress> getList() {
    return ips; // No defensive copies required
                // as visibility is package-private
  }
 
  public void addIPAddress(InetAddress address) {
    ips.add(address);
  }
}

public class GoodClientSideLockingIP1 {
    private final IPAddressList ips;
 
  public GoodClientSideLockingIP1(IPAddressList list) {
    this.ips = list;
  }
 
  public synchronized void addIPAddress(InetAddress address) {
    ips.addIPAddress(address);
  }
 
  public synchronized void addAndPrintIPAddresses(InetAddress address) {
    addIPAddress(address);
    InetAddress[] ia =
        (InetAddress[]) ips.getList().toArray(new InetAddress[0]);
    // ...
  }
}
