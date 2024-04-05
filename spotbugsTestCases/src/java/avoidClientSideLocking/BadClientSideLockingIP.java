package avoidClientSideLocking;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// This class could change its locking policy in the future,
// for example, if new non-atomic methods are added

public class BadClientSideLockingIP {
    private final List<InetAddress> ips = Collections.synchronizedList(new ArrayList<InetAddress>());

    public List<InetAddress> getList() {
        return ips; // No defensive copies required
                    // as visibility is package-private
    }

    public void addIPAddress(InetAddress address) {
        ips.add(address);
    }
}

class PrintableIPAddressList extends BadClientSideLockingIP {
    public void addAndPrintIPAddresses(InetAddress address) {
        // Error: If the underlying locking policy of BadClientSideLockingIP changes in the future, this client-side locking may not align with the actual locking strategy, leading to potential issues
        synchronized (getList()) { 
            addIPAddress(address);
            InetAddress[] ia = (InetAddress[]) getList().toArray(new InetAddress[0]);
            // ...
        }
    }

    public void test(InetAddress address) {
        // Error: If the underlying locking policy of BadClientSideLockingIP changes in the future, this client-side locking may not align with the actual locking strategy, leading to potential issues
        synchronized (getList()) { 
            addIPAddress(address);
            InetAddress[] ia = (InetAddress[]) getList().toArray(new InetAddress[0]);
            // ...
        }
    }
}
