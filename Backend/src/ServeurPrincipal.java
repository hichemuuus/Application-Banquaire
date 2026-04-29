package com.example.demo1;  // ← ajouter cette ligne
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ServeurPrincipal {
    private static final long serialVersionUID = 1L;
    public static void main(String[] args) throws Exception{
        try {
            Registry registry = LocateRegistry.createRegistry(1099);
            IBanqueService service = new BankServiceImpl();
            registry.rebind("BankService", service);
            System.out.println("Server ON port 1099");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
