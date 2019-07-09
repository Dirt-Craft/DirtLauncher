package net.dirtcraft.dirtlauncher.backend.data;

import javafx.application.Platform;
import javafx.geometry.VPos;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import net.cydhra.nidhogg.exception.InvalidCredentialsException;
import net.cydhra.nidhogg.exception.UserMigratedException;
import net.dirtcraft.dirtlauncher.Controller;
import net.dirtcraft.dirtlauncher.backend.Utils.Verification;
import net.dirtcraft.dirtlauncher.backend.objects.Account;
import net.dirtcraft.dirtlauncher.backend.objects.LoginResult;

import javax.annotation.Nullable;

public class LoginButtonHandler {
    private static boolean initialized = false;
    private static TextField usernameField;
    private static PasswordField passwordField;
    private static Button playButton;
    private static Thread uiCallback;
    private static TextFlow messageBox;
    private static Pane launchBox;

    @Nullable
    public static Account onClick() {
        if (!initialized){
            usernameField = Controller.getInstance().getUsernameField();
            passwordField = Controller.getInstance().getPasswordField();
            playButton = Controller.getInstance().getPlayButton();
            messageBox = Controller.getInstance().getMessageBox();
            launchBox = Controller.getInstance().getLaunchBox();
            uiCallback = null;
        }
        Account account = null;

        String email = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        try {
            account = Verification.login(email, password);
            displayLoginError(account, LoginResult.SUCCESS);
            //} catch (Exception e){
        } catch (InvalidCredentialsException e) {
                displayLoginError(account, LoginResult.INVALID_CREDENTIALS);
        } catch (IllegalArgumentException e) {
            displayLoginError(account, LoginResult.ILLEGAL_ARGUMENT);
        } catch (UserMigratedException e) {
            displayLoginError(account, LoginResult.USER_MIGRATED);
        }

        //TODO - do stuff with the userAccount

        return account;
    }

    private static void displayLoginError(Account account, LoginResult result){

        if (uiCallback != null) uiCallback.interrupt();

        Text text = new Text();
        text.getStyleClass().add("errorMessage");
        text.setFill(Color.WHITE);
        text.setTextOrigin(VPos.CENTER);

        if (account != null) text.setText("Successfully logged into " + account.getUsername() + "'s account");
        else {
            ShakeTransition animation = new ShakeTransition(messageBox);
            animation.playFromStart();

            switch (result) {
                case SUCCESS:
                    text.setText("Successfully logged into");
                    break;
                case USER_MIGRATED:
                    text.setText("An account with this username has already been migrated!");
                    break;
                case ILLEGAL_ARGUMENT:
                    text.setText("Your username or password contains invalid arguments!");
                    break;
                default:
                case INVALID_CREDENTIALS:
                    text.setText("Your E-Mail or password is invalid!");
                    break;
            }
        }

        //text.setText(message.replace("where", "were"));

        messageBox.setTextAlignment(TextAlignment.CENTER);
        messageBox.getChildren().clear();
        messageBox.getChildren().add(text);

        uiCallback = new Thread(() -> {
            Platform.runLater(() -> messageBox.setOpacity(1));
            try {
                Thread.sleep(5000);
                Platform.runLater(() -> messageBox.setOpacity(0));
                Platform.runLater(()-> uiCallback = null);
            } catch (InterruptedException ex) {
                //we interupted this boi so who cares
            }

        });
        uiCallback.start();
    }
}
