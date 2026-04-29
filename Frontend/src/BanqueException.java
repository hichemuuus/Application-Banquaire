

import java.rmi.RemoteException;

public class BanqueException extends RemoteException {
    public BanqueException(String message) {
        super(message);
    }
}
