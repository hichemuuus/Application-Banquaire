

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.List;
import java.util.ResourceBundle;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;

public class DashboardController implements Initializable {

    // ── Champs du formulaire ──────────────────────────────────────
    @FXML private TextField compteField;
    @FXML private TextField montantField;
    @FXML private TextField compteDestField;
    @FXML private TextField titulaireField;
    @FXML private TextField soldeInitialField;

    // ── Labels d'affichage ────────────────────────────────────────
    @FXML private Label soldeLabel;
    @FXML private Label userLabel;
    @FXML private Label statusLabel;
    @FXML private Button btnCreerCompte;
    @FXML private Button btnSupprimerCompte;
    @FXML private Button btnListerComptes;

    // ── Tableau historique ────────────────────────────────────────
    @FXML private TableView<String>         historiqueTable;
    @FXML private TableColumn<String,String> colHistorique;

    // ── Tableau comptes ───────────────────────────────────────────
    @FXML private TableView<String>         comptesTable;
    @FXML private TableColumn<String,String> colComptes;

    // ── Zone de log ───────────────────────────────────────────────
    @FXML private TextArea logArea;

    private IBanqueService service;
    private String loginUtilisateur;
    private String role;

    // ─────────────────────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        service = ClientRMI.getInstance().getService();

        // Configurer les colonnes TableView
        if (colHistorique != null)
            colHistorique.setCellValueFactory(
                    cell -> new SimpleStringProperty(cell.getValue()));

        if (colComptes != null)
            colComptes.setCellValueFactory(
                    cell -> new SimpleStringProperty(cell.getValue()));

        log("✓ Connecté au serveur RMI.");
    }

    public void setLoginUtilisateur(String login) {
        this.loginUtilisateur = login;
        if (userLabel != null)
            userLabel.setText("Connecté : " + login);
    }

    // ── Consulter Solde ───────────────────────────────────────────
    @FXML
    public void consulterSolde() {
        String cpte = compteField.getText().trim();
        if (cpte.isEmpty()) {
            showStatus("Entrez un numéro de compte.", false);
            return;
        }

        runAsync(() -> {
            try {

                if (!verifierAccesCompte(cpte)) {
                    Platform.runLater(() -> {
                        log(" Accès refusé : ce compte ne vous appartient pas.");
                        showStatus("Accès refusé.", false);
                    });
                    return;
                }

                // 3. Si OK → consulter le solde normalement
                double solde = service.consulterSolde(cpte);
                Platform.runLater(() -> {
                    soldeLabel.setText(String.format("%.2f DA", solde));
                    log("Solde du compte " + cpte + " : " + solde + " DA");
                    showStatus("Solde récupéré.", true);
                });

            } catch (RemoteException e) {
                Platform.runLater(() -> {
                    log("✗ Erreur : " + e.getMessage());
                    showStatus("Erreur serveur.", false);
                });
            }
        });
    }

    // ── Déposer ───────────────────────────────────────────────────
// ── Déposer ───────────────────────────────────────────────────
    @FXML
    public void deposer() {
        String cpte = compteField.getText().trim();
        double montant = parseMontant();
        if (cpte.isEmpty() || montant <= 0) {
            showStatus("Compte ou montant invalide.", false); return;
        }

        runAsync(() -> {
            try {
                // Vérifier que le compte appartient à l'utilisateur
                List<String> mesComptes = service.getComptesUtilisateur(loginUtilisateur);
                if (!mesComptes.contains(cpte)) {
                    Platform.runLater(() -> {
                        log("✗ Accès refusé : ce compte ne vous appartient pas.");
                        showStatus("Accès refusé.", false);
                    });
                    return;
                }

                boolean ok = service.deposer(cpte, montant);
                Platform.runLater(() -> {
                    if (ok) {
                        log("✓ Dépôt de " + montant + " DA sur " + cpte);
                        showStatus("Dépôt effectué.", true);
                        rafraichirSolde(cpte);
                    } else {
                        log("✗ Dépôt échoué sur " + cpte);
                        showStatus("Dépôt échoué.", false);
                    }
                });
            } catch (RemoteException e) {
                Platform.runLater(() -> showStatus("Erreur serveur : " + e.getMessage(), false));
            }
        });
    }

    // ── Retirer ───────────────────────────────────────────────────
    @FXML
    public void retirer() {
        String cpte = compteField.getText().trim();
        double montant = parseMontant();
        if (cpte.isEmpty() || montant <= 0) {
            showStatus("Compte ou montant invalide.", false); return;
        }

        runAsync(() -> {
            try {
                // Vérifier que le compte appartient à l'utilisateur
                List<String> mesComptes = service.getComptesUtilisateur(loginUtilisateur);
                if (!mesComptes.contains(cpte)) {
                    Platform.runLater(() -> {
                        log("✗ Accès refusé : ce compte ne vous appartient pas.");
                        showStatus("Accès refusé.", false);
                    });
                    return;
                }

                boolean ok = service.retirer(cpte, montant);
                Platform.runLater(() -> {
                    if (ok) {
                        log("✓ Retrait de " + montant + " DA depuis " + cpte);
                        showStatus("Retrait effectué.", true);
                        rafraichirSolde(cpte);
                    } else {
                        log("✗ Solde insuffisant sur " + cpte);
                        showStatus("Solde insuffisant.", false);
                    }
                });
            } catch (RemoteException e) {
                Platform.runLater(() -> showStatus("Erreur serveur.", false));
            }
        });
    }

    // ── Virement ──────────────────────────────────────────────────
    @FXML
    public void effectuerVirement() {
        String src  = compteField.getText().trim();
        String dest = compteDestField.getText().trim();
        double montant = parseMontant();

        if (src.isEmpty() || dest.isEmpty() || montant <= 0) {
            showStatus("Remplissez tous les champs.", false); return;
        }

        runAsync(() -> {
            try {
                // Vérifier que le compte SOURCE appartient à l'utilisateur
                List<String> mesComptes = service.getComptesUtilisateur(loginUtilisateur);
                if (!mesComptes.contains(src)) {
                    Platform.runLater(() -> {
                        log("✗ Accès refusé : le compte source ne vous appartient pas.");
                        showStatus("Accès refusé.", false);
                    });
                    return;
                }

                boolean ok = service.virement(src, dest, montant);
                Platform.runLater(() -> {
                    if (ok) {
                        log("✓ Virement de " + montant + " DA : " + src + " → " + dest);
                        showStatus("Virement effectué.", true);
                    } else {
                        log("✗ Virement échoué (solde insuffisant ou compte invalide)");
                        showStatus("Virement échoué.", false);
                    }
                });
            } catch (RemoteException e) {
                Platform.runLater(() -> showStatus("Erreur serveur.", false));
            }
        });
    }

    // ── Historique ────────────────────────────────────────────────
    // ── Historique ────────────────────────────────────────────────
    @FXML
    public void voirHistorique() {
        String cpte = compteField.getText().trim();
        if (cpte.isEmpty()) { showStatus("Entrez un numéro de compte.", false); return; }

        runAsync(() -> {
            try {

                if (!verifierAccesCompte(cpte)) {
                    Platform.runLater(() -> {
                        log("✗ Accès refusé : ce compte ne vous appartient pas.");
                        showStatus("Accès refusé.", false);
                    });
                    return;
                }

                List<String> hist = service.getHistorique(cpte);
                Platform.runLater(() -> {
                    ObservableList<String> data = FXCollections.observableArrayList(hist);
                    historiqueTable.setItems(data);
                    log("✓ Historique chargé : " + hist.size() + " opération(s)");
                    showStatus("Historique chargé.", true);
                });
            } catch (RemoteException e) {
                Platform.runLater(() -> showStatus("Erreur serveur.", false));
            }
        });
    }
    // ── Lister Comptes ────────────────────────────────────────────
    @FXML
    public void listerComptes() {
        runAsync(() -> {
            try {
                List<String> comptes = service.listerComptes();
                Platform.runLater(() -> {
                    ObservableList<String> data =
                            FXCollections.observableArrayList(comptes);
                    comptesTable.setItems(data);
                    log("✓ " + comptes.size() + " compte(s) trouvé(s)");
                    showStatus(comptes.size() + " comptes.", true);
                });
            } catch (RemoteException e) {
                Platform.runLater(() -> showStatus("Erreur serveur.", false));
            }
        });
    }

    // ── Créer Compte ──────────────────────────────────────────────
    @FXML
    public void creerCompte() {
        String titulaire = titulaireField.getText().trim();
        String soldeStr  = soldeInitialField.getText().trim();

        if (titulaire.isEmpty()) { showStatus("Entrez le login du titulaire.", false); return; }

        double soldeInit = 0;
        try { soldeInit = Double.parseDouble(soldeStr); }
        catch (NumberFormatException e) { showStatus("Solde initial invalide.", false); return; }

        final double soldeInitFinal = soldeInit;

        runAsync(() -> {
            try {
                boolean ok = service.creerCompte(titulaire, soldeInitFinal);
                final String msg = ok
                        ? " Compte créé pour " + titulaire
                        : " Création échouée (utilisateur introuvable ?)";
                Platform.runLater(() -> {
                    log(msg);
                    showStatus(ok ? "Compte créé." : "Échec création.", ok);
                });
            } catch (RemoteException e) {
                Platform.runLater(() -> showStatus("Erreur serveur.", false));
            }
        });
    }

    // ── Supprimer Compte ──────────────────────────────────────────
    @FXML
    public void supprimerCompte() {
        String cpte = compteField.getText().trim();
        if (cpte.isEmpty()) { showStatus("Entrez un numéro de compte.", false); return; }

        // Confirmation avant suppression
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer le compte " + cpte + " ?");
        alert.setContentText("Cette action est irréversible.");
        alert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                runAsync(() -> {
                    try {
                        boolean ok = service.supprimerCompte(cpte);
                        Platform.runLater(() -> {
                            if (ok) {
                                log("✓ Compte " + cpte + " supprimé.");
                                showStatus("Compte supprimé.", true);
                            } else {
                                log("✗ Suppression échouée.");
                                showStatus("Compte introuvable.", false);
                            }
                        });
                    } catch (RemoteException e) {
                        Platform.runLater(() -> showStatus("Erreur serveur.", false));
                    }
                });
            }
        });
    }

    // ── Déconnexion ───────────────────────────────────────────────
    @FXML
    public void seDeconnecter() {
        try {
            FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/Login.fxml"));
            Stage stage = (Stage) logArea.getScene().getWindow();
            stage.setScene(new javafx.scene.Scene(loader.load(), 480, 560));
            stage.setTitle("BanqueRMI — Connexion");
            stage.setResizable(false);
        } catch (Exception e) {
            log("Erreur déconnexion : " + e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────
    private void rafraichirSolde(String cpte) {
        runAsync(() -> {
            try {
                double s = service.consulterSolde(cpte);
                Platform.runLater(() ->
                        soldeLabel.setText(String.format("%.2f DA", s)));
            } catch (RemoteException ignored) {}
        });
    }

    private double parseMontant() {
        try { return Double.parseDouble(montantField.getText().trim()); }
        catch (NumberFormatException e) { return -1; }
    }

    private void runAsync(Runnable task) {
        new Thread(task).start();
    }

    private void log(String msg) {
        if (logArea != null) logArea.appendText(msg + "\n");
    }

    private void showStatus(String msg, boolean success) {
        if (statusLabel != null) {
            statusLabel.setText(msg);
            statusLabel.setStyle(success
                    ? "-fx-text-fill: #38a169;"
                    : "-fx-text-fill: #e53e3e;");
        }
    }
    public void setRole(String role) {
        if (!"ADMIN".equals(role)) {
            btnCreerCompte.setVisible(false);
            btnCreerCompte.setManaged(false);

            btnSupprimerCompte.setVisible(false);
            btnSupprimerCompte.setManaged(false);

            btnListerComptes.setVisible(false);
            btnListerComptes.setManaged(false);

            titulaireField.setVisible(false);
            titulaireField.setManaged(false);

            soldeInitialField.setVisible(false);
            soldeInitialField.setManaged(false);
        }
    }
    private boolean verifierAccesCompte(String numeroCompte) throws RemoteException {
        if ("ADMIN".equals(role)) {
            return true; // Admin voit tout
        }
        List<String> mesComptes = service.getComptesUtilisateur(loginUtilisateur);
        return mesComptes.contains(numeroCompte);
    }
}
