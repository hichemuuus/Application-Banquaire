package com.example.demo1;

import java.rmi.RemoteException;

public class BanqueException extends RemoteException {
    public BanqueException(String message) {
        super(message);
    }
}
