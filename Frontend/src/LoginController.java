package com.example.demo1;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class LoginController {

    @FXML private TextField     serverField;
    @FXML private TextField     loginField;
    @FXML private PasswordField passwordField;
    @FXML private Label         messageLabel;
    @FXML private Button        btnConnecter;

    @FXML
    public void seConnecter() {
        String server = serverField.getText().trim();
        String login  = loginField.getText().trim();
        String mdp    = passwordField.getText();

        // Validation des champs
        if (server.isEmpty()) { showError("Entrez l'adresse IP du serveur."); return; }
        if (login.isEmpty())  { showError("Entrez votre login."); return; }
        if (mdp.isEmpty())    { showError("Entrez votre mot de passe."); return; }

        // Désactiver le bouton pendant la connexion
        btnConnecter.setDisable(true);
        showInfo("Connexion en cours...");

        // Appel RMI dans un thread séparé pour ne pas bloquer l'UI
        new Thread(() -> {
            try {
                // 1. Connexion au registre RMI
                ClientRMI.getInstance().connect(server);

                // 2. Authentification
                boolean ok = ClientRMI.getInstance()
                        .getService()
                        .authentifier(login, mdp);

                Platform.runLater(() -> {
                    if (ok) {
                        try {
                            // 3. Charger le dashboard
                            FXMLLoader loader = new FXMLLoader(
                                    getClass().getResource("/dashboard.fxml"));
                            Stage stage = (Stage) serverField.getScene().getWindow();
                            stage.setScene(new Scene(loader.load(), 900, 650));
                            stage.setTitle("FAKE BANK — Tableau de bord");
                            stage.setResizable(true);

                            // 4. Passer le login au dashboard
                            DashboardController dc = loader.getController();
                            dc.setLoginUtilisateur(login);
                            String role = ClientRMI.getInstance().getService().getRoleUtilisateur(login);
                            dc.setRole(role);

                        } catch (Exception e) {
                            showError("Erreur chargement dashboard : " + e.getMessage());
                            btnConnecter.setDisable(false);
                        }
                    } else {
                        showError("Login ou mot de passe incorrect.");
                        btnConnecter.setDisable(false);
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Serveur indisponible : " + e.getMessage());
                    btnConnecter.setDisable(false);
                });
            }
        }).start();
    }

    private void showError(String msg) {
        messageLabel.setStyle("-fx-text-fill: #e53e3e; -fx-font-size: 12px;");
        messageLabel.setText("✗  " + msg);
    }

    private void showInfo(String msg) {
        messageLabel.setStyle("-fx-text-fill: #718096; -fx-font-size: 12px;");
        messageLabel.setText(msg);
    }
}
