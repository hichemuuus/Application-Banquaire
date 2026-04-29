package com.example.demo1;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface IBanqueService extends Remote {
    boolean authentifier(String login, String motDePasse) throws RemoteException;
    double consulterSolde(String numeroCompte) throws RemoteException;
    boolean deposer(String numeroCompte, double montant) throws RemoteException;
    boolean retirer(String numeroCompte, double montant) throws RemoteException;
    boolean virement(String compteSource, String compteDest, double montant) throws RemoteException;
    List<String> getHistorique(String numeroCompte) throws RemoteException;
    boolean creerCompte(String titulaire, double soldeInitial) throws RemoteException;
    boolean supprimerCompte(String numeroCompte) throws RemoteException;
    List<String> listerComptes() throws RemoteException;
    String getRoleUtilisateur(String login) throws RemoteException;
    List<String> getComptesUtilisateur(String login) throws RemoteException;
}
