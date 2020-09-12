package net.dirtcraft.dirtlauncher.gui.home.sidebar;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.TextAlignment;
import net.dirtcraft.dirtlauncher.Main;
import net.dirtcraft.dirtlauncher.game.modpacks.Modpack;
import net.dirtcraft.dirtlauncher.gui.components.DiscordPresence;
import net.dirtcraft.dirtlauncher.gui.home.login.LoginBar;
import net.dirtcraft.dirtlauncher.utils.Constants;
import net.dirtcraft.dirtlauncher.utils.FileUtils;
import net.dirtcraft.dirtlauncher.utils.MiscUtils;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public final class PackSelector extends Button implements Comparable<PackSelector> {
    private double lastDragY;
    private final ContextMenu contextMenu;
    private final Modpack modpack;

    PackSelector(Modpack modpack) {
        this.modpack = modpack;
        contextMenu = new ContextMenu();
        initContextMenu();
        setCursor(Cursor.HAND);
        setFocusTraversable(false);
        setText(modpack.getName());

        final Tooltip tooltip = new Tooltip();
        tooltip.setTextAlignment(TextAlignment.LEFT);

        tooltip.setText(String.join("\n", Arrays.asList(
                "ModPack Name: " + modpack.getName(),
                "ModPack Version: " + modpack.getVersion(),
                "Minecraft Version: " + modpack.getGameVersion(),
                "Forge Version: " + modpack.getForgeVersion(),
                "Minimum Ram: " + modpack.getRequiredRam() + " GB",
                "Recommended Ram: " + modpack.getRecommendedRam() + " GB",
                "Direct Connect IP: " + (!modpack.isPixelmon() ? (modpack.getCode() + ".DIRTCRAFT").toUpperCase() : "PIXELMON") + ".GG")
        ));

        Image image;
        try {
            image = new Image(MiscUtils.getResourceStream(
                    Constants.JAR_PACK_IMAGES, modpack.getFormattedName().toLowerCase() + ".png"),
                    128, 128, false, true);
        } catch (Exception exception) {
            System.out.println("Could not find image @ \"" + modpack.getForgeVersion().toLowerCase() + ".png\", requesting from the web...");
            image = new Image(modpack.getLogo(), 128, 128, false, true);
        }

        if (image != null) {
            final ImageView imageView = new ImageView(image);
            imageView.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 10, 0, 0, 0);");

            tooltip.setGraphic(imageView);
            tooltip.setGraphicTextGap(50);
        }

        setTooltip(tooltip);
        setOnMouseDragEntered(e-> lastDragY =  e.getY());
        setOnMouseDragged(this::onDrag);
        setOnMouseDragExited(e-> lastDragY = 0);
        setOnContextMenuRequested(e->contextMenu.show(this, e.getScreenX(), e.getScreenY()));
    }

    private void deactivate(){
        pseudoClassStateChanged(PseudoClass.getPseudoClass("selected"), false);
    }

    private void onDrag(MouseEvent event) {
        if (event.isPrimaryButtonDown()) {
            ScrollPane window = (ScrollPane) this.getParent().getParent().getParent().getParent();
            double change = (lastDragY - event.getY()) / window.getHeight();
            window.setVvalue(window.getVvalue() + change);
            lastDragY = change;
        }
    }

    public void update(){
        initContextMenu();
    }

    public void fire() {
        final LoginBar home = Main.getHome().getLoginBar();
        final Button playButton = home.getActionButton();
        home.getActivePackCell().ifPresent(PackSelector::deactivate);
        home.setActivePackCell(this);
        pseudoClassStateChanged(PseudoClass.getPseudoClass("selected"), true);
        DiscordPresence.setDetails("Playing " + modpack.getName());

        if (!MiscUtils.isEmptyOrNull(home.getUsernameField().getText().trim(), home.getPassField().getText().trim()) || Main.getAccounts().hasSelectedAccount()) playButton.setDisable(false);
    }

    private void initContextMenu(){
        contextMenu.getItems().clear();
        if (modpack.isInstalled()) {
            MenuItem reinstall = new MenuItem("Reinstall");
            MenuItem uninstall = new MenuItem("Uninstall");
            MenuItem openFolder = new MenuItem("Open Folder");
            MenuItem favourite = new MenuItem(modpack.isFavourite()? "Unpin" : "Pin");
            contextMenu.getItems().add(favourite);
            contextMenu.getItems().add(reinstall);
            contextMenu.getItems().add(uninstall);
            contextMenu.getItems().add(openFolder);
            contextMenu.pseudoClassStateChanged(PseudoClass.getPseudoClass("installed"), true);

            reinstall.setOnAction(e->{
                uninstall.fire();
                LoginBar loginBar = Main.getHome().getLoginBar();
                Optional<PackSelector> oldPack = loginBar.getActivePackCell();
                loginBar.setActivePackCell(this);
                getModpack().install().thenRun(this::update);
                oldPack.ifPresent(PackSelector::fire);
                Main.getHome().update();
                initContextMenu();
            });

            uninstall.setOnAction(e->{
                JsonObject instanceManifest = FileUtils.readJsonFromFile(Main.getConfig().getDirectoryManifest(Main.getConfig().getInstancesDirectory()));
                if (instanceManifest == null || !instanceManifest.has("packs")) return;
                JsonArray packs = instanceManifest.getAsJsonArray("packs");
                for (int i = 0; i < packs.size(); i++){
                    if (Objects.equals(packs.get(i).getAsJsonObject().get("name").getAsString(), modpack.getName())) packs.remove(i);
                }
                FileUtils.writeJsonToFile(new File(Main.getConfig().getDirectoryManifest(Main.getConfig().getInstancesDirectory()).getPath()), instanceManifest);
                try {
                    FileUtils.deleteDirectory(modpack.getInstanceDirectory());
                } catch (IOException exception){
                    exception.printStackTrace();
                }
                if (isFavourite()) modpack.toggleFavourite();
                Main.getHome().update();
                initContextMenu();
            });

            openFolder.setOnAction(e->{
                try {
                    Desktop.getDesktop().open(modpack.getInstanceDirectory());
                } catch (IOException exception){
                    exception.printStackTrace();
                }
            });

            favourite.setOnAction(e->{
                modpack.toggleFavourite();
                Main.getHome().updateModpacks();
            });
        } else {
            MenuItem install = new MenuItem("Install");
            contextMenu.getItems().add(install);

            install.setOnAction(e->{
                LoginBar loginBar = Main.getHome().getLoginBar();
                Optional<PackSelector> oldPack = loginBar.getActivePackCell();
                loginBar.setActivePackCell(this);
                getModpack().install().thenRun(this::update);
                oldPack.ifPresent(PackSelector::fire);
            });
        }
    }

    public Modpack getModpack(){
        return modpack;
    }

    public boolean isFavourite(){
        return modpack.isFavourite();
    }

    public String getName(){
        return modpack.getName();
    }

    @Override
    public int compareTo(@NotNull PackSelector o) {
        if (o.isFavourite() != isFavourite()) return isFavourite()? -1 : 1;
        else if (o.getModpack().isInstalled() != getModpack().isInstalled()) return modpack.isInstalled()? -1 : 1;
        else return getName().compareTo(o.getName());
    }
}
