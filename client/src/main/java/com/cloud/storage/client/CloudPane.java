package com.cloud.storage.client;


import com.cloud.storage.common.AbstractMessage;
import com.cloud.storage.common.MessageType;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.input.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


class CloudPane {
    private TitledPane cloudPane;
    private ListView<String> cloudListView;
    private ObservableList<String> filesNames;
    private ContextMenu contextMenu;
    private TextInputDialog renameDialog;
    private Alert propAlert;
    private Button enterBtn;
    private TextField loginFld;
    private PasswordField passFld;
    private Alert unacceptableInput;
    private ArrayList<String> filesSizes;
    private Network network;
    private LocalPane lPane;
    private DataHandler dataHandler;
    private KeyCombination keyCombinationProps;
    
    CloudPane(Controller controller) {
        
        this.cloudListView = controller.getCloudListView();
        this.cloudPane = controller.getCloudPane();
        this.enterBtn = controller.getEnterBtn();
        this.loginFld = controller.getLoginFld();
        this.passFld = controller.getPassFld();
        this.lPane = controller.getlPane();
        this.dataHandler = controller.getDataHandler();
        network = Network.getInstance();
        initLoginScreen();
    }
    
    private void initLoginScreen(){
        toggleView(true);
        unacceptableInput = new Alert(Alert.AlertType.ERROR);
        unacceptableInput.setGraphic(null);
        unacceptableInput.setTitle(null);
        unacceptableInput.setContentText(null);
        unacceptableInput.setHeaderText("Incorrect login-password combination!");
        enterBtn.setOnAction(v -> {
            String login = loginFld.getText();
            String passwd = passFld.getText();
            if (login.isEmpty() || passwd.isEmpty()) {
                unacceptableInput.showAndWait();
            } else {
                dataHandler.write(new AbstractMessage(MessageType.AUTH, login, passwd));
            }
            v.consume();
        });
    
        passFld.setOnKeyPressed(v -> {
            switch (v.getCode()) {
                case ENTER: enterBtn.fire();
                break;
            }
        });
    }
    
    void showUnacceptableInputAlert() {
        unacceptableInput.showAndWait();
    }
    
    void init(){
        toggleView(false);
        renameDialog = new TextInputDialog();
        renameDialog.setTitle("Enter new name");
        renameDialog.setHeaderText(null);
        renameDialog.setGraphic(null);
    
        propAlert = new Alert(Alert.AlertType.INFORMATION);
        propAlert.setGraphic(null);
        propAlert.setHeaderText(null);
        propAlert.setResizable(true);
        
        filesNames = FXCollections.observableArrayList();
        filesSizes = new ArrayList<>();
        
        MenuItem refresh = new MenuItem("Refresh (F5)");
        MenuItem download = new MenuItem("Download (D)");
        MenuItem rename = new MenuItem("Rename (F2)");
        MenuItem delete = new MenuItem("Delete (Del)");
        MenuItem properties = new MenuItem("Properties (Alt + Enter)");
    
        keyCombinationProps = new KeyCodeCombination(KeyCode.ENTER, KeyCombination.ALT_DOWN);
        
        contextMenu = new ContextMenu();
        contextMenu.getItems().addAll(refresh, download, rename, delete, properties);
        
        cloudListView.setOnDragOver(v->{
            if (v.getDragboard().getFiles().size() == 1 &&
                    !v.getDragboard().getFiles().get(0).isDirectory() ||
                    v.getDragboard().hasString() &&
                    //accept drag and drop only if it does not come from cPane
                    !v.getDragboard().getString().startsWith("cPane"))
            {
                v.acceptTransferModes(TransferMode.COPY);
            }
            v.consume();
        });
        cloudListView.setOnDragDropped(v-> {
            File file;
            if(v.getDragboard().hasString()) {
                //cut off the mark
                file = new File(v.getDragboard().getString().substring(5));
            } else {
                file = v.getDragboard().getFiles().get(0);
            }

            String fileName = file.getName();
            byte[] fileBytes = null;
            try {
                fileBytes = Files.readAllBytes(file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
            dataHandler.write(new AbstractMessage(MessageType.FILE, fileName, fileBytes));
            
            v.setDropCompleted(true);
            v.consume();
        });
        cloudListView.setCellFactory(param -> {
            ListCell<String> cell = new ListCell<>();
            cell.textProperty().bind(cell.itemProperty());
            cell.textProperty().addListener((observable, oldValue, newValue) -> {
                if(newValue != null) {
                    cell.setDisable(false);
                    cell.setContextMenu(contextMenu);
                    
                    cell.setOnDragDetected(v->{
                        Dragboard db = cell.startDragAndDrop(TransferMode.COPY);
                        ClipboardContent cc = new ClipboardContent();
                        //mark that this drag comes from cPane
                        cc.putString("cPane" + getSelectedItem());
                        db.setContent(cc);
                        v.setDragDetect(true);
                        v.consume();
                    });
                    
                } else {
                    cell.setDisable(true);
                }
            });
            return cell;
        });
    
        refresh();
        
        cloudListView.setOnKeyPressed(event -> {
            if (keyCombinationProps.match(event)) {
                generateProperties();
            } else {
                switch (event.getCode()) {
                    case ESCAPE:
                        dataHandler.write(new AbstractMessage(MessageType.DEAUTH));
                        toggleView(true);
                        break;
                    case DELETE:
                        delete();
                        break;
                    case F2:
                        rename();
                        break;
                    case F5:
                        refresh();
                        break;
                    case UP:
                        if(cloudListView.getSelectionModel().isEmpty()) {
                            cloudListView.getSelectionModel().selectLast();
                        }
                        break;
                    case D:
                        download();
                        break;
                }
            }
        });
    
        renameDialog.getEditor().setOnKeyPressed(event -> {
            if(event.getCode().equals(KeyCode.ENTER)) {
                renameDialog.getEditor().commitValue();
            }
        });
        
        refresh.setOnAction(event -> {
            refresh();
            event.consume();
        });
        
        download.setOnAction(event -> {
            download();
            event.consume();
        });
        
        rename.setOnAction(event -> {
            rename();
            event.consume();
        });
        
        delete.setOnAction(event -> {
            delete();
            event.consume();
        });
        
        properties.setOnAction(event -> {
            generateProperties();
            event.consume();
        });
    }
    
    private void generateProperties() {
        if (!cloudListView.getSelectionModel().isEmpty()) {
            int selectedFileIndex = cloudListView.getSelectionModel().getSelectedIndex();
            String name = filesNames.get(selectedFileIndex);
            long size = Long.parseLong(filesSizes.get(selectedFileIndex));
            StringBuilder byteSize = new StringBuilder(filesSizes.get(selectedFileIndex));
            for (int i = byteSize.length() - 3; i > 0 ; i -= 3) {
                byteSize.insert(i, ' ');
            }
            String sizeStr = filesSizes.get(selectedFileIndex) + " bytes";
            if(size >= 1073741824L) sizeStr = String.format("%.2f", (float)size/1073741824L) + " Gb (" + byteSize + " bytes)";
            else if(size >= 1048576L) sizeStr = String.format("%.2f", (float)size/1048576L) + " Mb (" + byteSize + " bytes)";
            else if(size >= 1024L) sizeStr = String.format("%.2f", (float)size/1024L) + " Kb (" + byteSize + " bytes)";
            propAlert.setContentText("Name: " + name + "\n" + "Size: " + sizeStr);
            propAlert.setTitle(name + " properties");
        
            propAlert.showAndWait();
        }
    }
    
    private void download() {
        if (!cloudListView.getSelectionModel().isEmpty()) {
            String fileName = getSelectedItem();
            dataHandler.write(new AbstractMessage(MessageType.DOWNLOAD, fileName));
        }
    }
    
    void refresh() {
        dataHandler.write(new AbstractMessage(MessageType.FILE_LIST));
    }
    
    void updateListView(AbstractMessage data) {
        filesSizes.clear();
        filesNames.clear();
        ArrayList<String[]> fileList = (ArrayList<String[]>) data.getObjects()[0];
        fileList.forEach(s -> {
            filesNames.add(s[0]);
            filesSizes.add(s[1]);
        });
        cloudListView.setItems(filesNames);
    }
    
    private String getSelectedItem() {
        return cloudListView.getSelectionModel().getSelectedItem();
    }
    
    private void rename() {
        if (!cloudListView.getSelectionModel().isEmpty()) {
            String oldName = getSelectedItem();
            renameDialog.getEditor().setText(oldName);
            renameDialog.showAndWait();
            if(renameDialog.getResult() != null) {
                String newName = renameDialog.getResult();
                dataHandler.write(new AbstractMessage(MessageType.RENAME, oldName, newName));
            }
        }
    }
    
    private void delete() {
        if (!cloudListView.getSelectionModel().isEmpty()) {
            String fileName = getSelectedItem();
            dataHandler.write(new AbstractMessage(MessageType.DELETE, fileName));
        }
    }
    
    void toggleView(boolean loginViewIsVisible) {
        loginFld.setVisible(loginViewIsVisible);
        passFld.setVisible(loginViewIsVisible);
        enterBtn.setVisible(loginViewIsVisible);
        loginFld.setManaged(loginViewIsVisible);
        passFld.setManaged(loginViewIsVisible);
        enterBtn.setManaged(loginViewIsVisible);
        cloudListView.setManaged(!loginViewIsVisible);
        cloudListView.setVisible(!loginViewIsVisible);
        if (loginViewIsVisible) {
            loginFld.requestFocus();
        } else {
            cloudListView.requestFocus();
        }
    }
}
