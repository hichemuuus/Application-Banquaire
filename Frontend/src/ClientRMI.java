

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ClientRMI {

    private IBanqueService service;
    private static ClientRMI instance;

    private ClientRMI() {}

    public static ClientRMI getInstance() {
        if (instance == null) instance = new ClientRMI();
        return instance;
    }

    public void connect(String host) throws RemoteException, NotBoundException {
        Registry registry = LocateRegistry.getRegistry(host, 1099);
        service = (IBanqueService) registry.lookup("BankService");
    }

    public IBanqueService getService() {
        return service;
    }

    public boolean isConnected() {
        return service != null;
    }
}
