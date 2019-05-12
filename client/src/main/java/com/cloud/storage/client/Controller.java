package com.cloud.storage.client;

import com.cloud.storage.common.AbstractMessage;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable, DataUtilizer{
    private LocalPane lPane;
    private CloudPane cPane;
    private DataHandler dataHandler;
    
    @FXML
    SplitPane splitPane;
   
    @FXML
    TitledPane localPane;
    
    @FXML
    TitledPane cloudPane;
    
    @FXML
    ListView<String> localListView;
    
    @FXML
    ListView<String> cloudListView;
    
    @FXML
    Button enterBtn;
    
    @FXML
    TextField loginFld;
    
    @FXML
    PasswordField passFld;
    
    LocalPane getlPane() {
        return lPane;
    }
    
    CloudPane getcPane() {
        return cPane;
    }
    
    DataHandler getDataHandler() {
        return dataHandler;
    }
    
    public SplitPane getSplitPane() {
        return splitPane;
    }
    
    TitledPane getLocalPane() {
        return localPane;
    }
    
    TitledPane getCloudPane() {
        return cloudPane;
    }
    
    ListView<String> getLocalListView() {
        return localListView;
    }
    
    ListView<String> getCloudListView() {
        return cloudListView;
    }
    
    Button getEnterBtn() {
        return enterBtn;
    }
    
    TextField getLoginFld() {
        return loginFld;
    }
    
    PasswordField getPassFld() {
        return passFld;
    }
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        dataHandler = new DataHandler(this);
        lPane = new LocalPane(this);
        cPane = new CloudPane(this);
       
    }
    
    @Override
    public void utilizeFileList(AbstractMessage data) {
        Platform.runLater(()-> cPane.updateListView(data));
        
    }
    
    @Override
    public void authFailed() {
        Platform.runLater(()-> cPane.showUnacceptableInputAlert());
    }
    
    @Override
    public void utilizeFile(AbstractMessage data) {
        Platform.runLater(()-> lPane.createNewFile(data));
    }
    
    @Override
    public void authOk() {
        Platform.runLater(()-> cPane.init());
    }
    
    @Override
    public void uploadFinished() {
        Platform.runLater(()-> cPane.refresh());
    }
    
    @Override
    public void deauth() {
        Platform.runLater(() -> cPane.toggleView(true));
    }
}
