
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;



public class BankServiceImpl extends UnicastRemoteObject implements IBanqueService {
    private Connection conn;
    private static final long serialVersionUID = 1L;
    public BankServiceImpl() throws RemoteException {
         super();
         try{
              conn = DriverManager.getConnection(
                     "jdbc:sqlserver://HICHEM\\SQLEXPRESS:63135;" +
                             "databaseName=banque_db;encrypt=false",
                     "sa",
                     "1234"
             );
         }catch (SQLException e){
             throw new RemoteException("Erreur connection BDD" ,e);
         }
    }

    @Override
    public boolean authentifier(String login, String motDePasse) throws RemoteException {
        String sql = "SELECT mot_de_passe FROM UTILISATEUR WHERE login = ?";
        try(PreparedStatement pr = conn.prepareStatement(sql)){
            pr.setString(1,login);

            ResultSet rs = pr.executeQuery();
            if(rs.next()){
                String storedHash = rs.getString("mot_de_passe");
                return Utils.sha256(motDePasse).equals(storedHash);                       //verification de mot de passe
            }

        } catch (SQLException e) {
            throw new RemoteException();
        }
        return false;
    }

    @Override
    public double consulterSolde(String numeroCompte) throws RemoteException {
        String sql = "SELECT solde FROM COMPTE WHERE numero_compte = ? ";
        try{
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1,numeroCompte);
            ResultSet rs = ps.executeQuery();
            if(rs.next()){
                return rs.getDouble("solde");
            }



        } catch (SQLException e) {
            throw new RemoteException();
        }
        return 0;
    }

    @Override
    public boolean deposer(String numeroCompte, double montant) throws RemoteException {
        if(montant <= 0){
            return false;
        }
        String sql = "UPDATE COMPTE SET solde = solde + ?  WHERE numero_compte = ?";
        String log = "INSERT INTO [TRANSACTION](numero_compte,type_op,montant,compte_dest) VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             PreparedStatement plog = conn.prepareStatement(log)){
            conn.setAutoCommit(false);

            ps.setDouble(1,montant);
            ps.setString(2,numeroCompte);

            int updated = ps.executeUpdate();
            if(updated == 0){
                conn.rollback();
                return false;
            }


            plog.setNull(4, java.sql.Types.VARCHAR);
            plog.setString(2, "DEPOT");
            plog.setDouble(3, montant);
            plog.setString(1, numeroCompte);

            int succ = plog.executeUpdate();
            if (succ == 0) {
                conn.rollback();
                return false;
            }

            conn.commit();
            return true;
        }catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException ignored) {}
            throw new RemoteException();
        }finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException ignored){}
        }

    }

    @Override
    public boolean retirer(String numeroCompte, double montant) throws RemoteException {
        if(montant <= 0){
            return false;
        }
        String sql ="UPDATE COMPTE SET solde = solde - ? WHERE numero_compte = ? AND solde >= ?";
        String log = "INSERT INTO [TRANSACTION](numero_compte,type_op,montant,compte_dest) VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             PreparedStatement plog = conn.prepareStatement(log)){
            conn.setAutoCommit(false);

            ps.setDouble(1 ,montant);
            ps.setString(2,numeroCompte);
            ps.setDouble(3,montant);

            int updated = ps.executeUpdate();
            if(updated == 0){
                conn.rollback();
                return false;
            }


            plog.setString(1, numeroCompte);
            plog.setString(2, "RETRAIT");
            plog.setDouble(3, montant);
            plog.setNull(4, java.sql.Types.VARCHAR);
            int succ = plog.executeUpdate();
            if (succ == 0) {
                conn.rollback();
                return false;
            }
            conn.commit();
            return true;

        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException ignored) {}
            throw new RemoteException();
        }finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException ignored){}
        }
    }

    @Override
    public boolean virement(String compteSource, String compteDest, double montant) throws RemoteException {
        String retirer = "UPDATE COMPTE SET solde = solde - ? WHERE numero_compte = ? AND solde >= ?";
        String deposer = "UPDATE COMPTE SET solde = solde + ?  WHERE numero_compte = ?";
        String log = "INSERT INTO [TRANSACTION](numero_compte,type_op,montant,compte_dest) VALUES (?,?,?,?)";
        try {
            conn.setAutoCommit(false);
            PreparedStatement ps = conn.prepareStatement(retirer);
            ps.setDouble(1, montant);
            ps.setString(2, compteSource);
            ps.setDouble(3, montant);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                conn.rollback();
                return false;
            }

            PreparedStatement pr = conn.prepareStatement(deposer);
            pr.setDouble(1, montant);
            pr.setString(2, compteDest);

            int d = pr.executeUpdate();
            if (d == 0) {
                conn.rollback();
                return false;
            }

            PreparedStatement plog = conn.prepareStatement(log);
            plog.setString(1, compteSource);
            plog.setString(2, "VIREMENT");
            plog.setDouble(3, montant);
            plog.setString(4, compteDest);
            plog.executeUpdate();

            conn.commit();
            return true;

        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException ignored) {}
            throw new RemoteException();
        }finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException ignored){}
        }
    }

    @Override
    public List<String> getHistorique(String numeroCompte) throws RemoteException {
        String sql ="SELECT * FROM [TRANSACTION] WHERE numero_compte = ? OR compte_dest = ? ORDER BY date_op DESC ";
        List<String> his = new ArrayList<>();
        try{
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1,numeroCompte);
            ps.setString(2,numeroCompte);
            ResultSet rs = ps.executeQuery();
            while(rs.next()){
                String trans = rs.getInt("id_transaction")+" | " +rs.getString("numero_compte")+" | "
                        +rs.getString("type_op")+" | " + rs.getDouble("montant")+" | " +rs.getTimestamp("date_op")+" | "
                        +rs.getString("compte_dest");
                his.add(trans);

            }
        } catch (SQLException e) {
            throw new RemoteException("Erreur SQL",e);
        }
        return his;
    }

    @Override
    public boolean creerCompte(String titulaire, double soldeInitial) throws RemoteException {
        try{
            conn.setAutoCommit(false);
            String id = "SELECT id_utilisateur FROM UTILISATEUR WHERE Login = ?";
            PreparedStatement ps = conn.prepareStatement(id);
            ps.setString(1 ,titulaire);
            ResultSet d = ps.executeQuery();
            if(!d.next()){
                conn.rollback();
                throw new RemoteException("User not found");
            }
            int iduser = d.getInt("id_utilisateur");
            String numeroCompte = "CPT" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String create = "INSERT INTO COMPTE (numero_compte, id_utilisateur ,solde ,date_creation, actif) VALUES (?,?,?,?,?)";
            PreparedStatement pr = conn.prepareStatement(create);
            pr.setString(1,numeroCompte);
            pr.setInt(2,iduser);
            pr.setDouble(3,soldeInitial);
            pr.setDate(4,java.sql.Date.valueOf(LocalDate.now()));
            pr.setBoolean(5,true);
            pr.executeUpdate();

            conn.commit();
            return true;


        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException ignored) {}
            throw new RemoteException();
        }finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException ignored){}
        }

    }

    @Override
    public boolean supprimerCompte(String numeroCompte) throws RemoteException {

        String sql = "UPDATE COMPTE SET actif = 0 WHERE numero_compte = ?";
        try(PreparedStatement ps = conn.prepareStatement(sql)){
            conn.setAutoCommit(false);
            ps.setString(1 ,numeroCompte);
            int d = ps.executeUpdate();
            if(d==0){
                conn.rollback();
                return false;
            }
            conn.commit();
            return true;

        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException ignored) {}
            throw new RemoteException("Error deleting account" ,e);
        }finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException ignored){}
        }
    }

    @Override
    public List<String> listerComptes() throws RemoteException {
        String sql ="SELECT * FROM COMPTE";
        List<String> compte = new ArrayList<>();
        try(PreparedStatement ps = conn.prepareStatement(sql)){
            ResultSet rs = ps.executeQuery();
            while(rs.next()) {
                String trans = rs.getString("numero_compte")+" | " + rs.getInt("id_utilisateur")+" | "
                        + rs.getDouble("solde")+" | " + rs.getTimestamp("date_creation")+" | "
                        + rs.getBoolean("actif");
                compte.add(trans);
            }

        }catch (SQLException e) {
            throw new RemoteException("Error listing accounts", e);
        }
        return compte;
    }
    @Override
    public String getRoleUtilisateur(String login) throws RemoteException {
        String sql = "SELECT role FROM UTILISATEUR WHERE login = ?";
        try{
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1,login);
            ResultSet rs = ps.executeQuery();
            if(!rs.next()){
                throw new RemoteException("Error");
            }
            return rs.getString("role");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> getComptesUtilisateur(String login) throws RemoteException {
        String sql = "SELECT numero_compte FROM COMPTE C JOIN UTILISATEUR U ON C.id_utilisateur = U.id_utilisateur WHERE U.login = ? ";
        List<String> comptes = new ArrayList<>();
        try(PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setString(1 ,login);
            try(ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String trans = rs.getString("numero_compte");
                    comptes.add(trans);
                }
            }

        }catch (SQLException e) {
            throw new RemoteException("Error listing accounts", e);
        }
        return comptes;

    }
}
