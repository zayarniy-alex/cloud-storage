package com.cloud.storage.client;

import com.cloud.storage.common.AbstractMessage;
import com.cloud.storage.common.MessageType;
import com.sun.istack.internal.Nullable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.MenuItem;
import javafx.scene.input.*;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

class LocalPane {
    private static final String START_DIR = "D:\\local";
    private TitledPane localPane;
    private String currentPath;
    private ObservableList<String> fileNames;
    private ContextMenu contextMenuForFiles;
    private ContextMenu contextMenuForDir;
    private Path srcPath;
    private Path dstPath;
    private Boolean move;
    private TextInputDialog renameDialog;
    private Alert propAlert;
    private ListView<String> localListView;
    private Network network;
    private CloudPane cPane;
    private DataHandler dataHandler;
    private KeyCombination keyCombinationCopy;
    private KeyCombination keyCombinationCut;
    private KeyCombination keyCombinationPaste;
    private KeyCombination keyCombinationProps;
    private long size;
    private int numberOfFiles;
    private int numberOfDirs = -1;
    
    
    LocalPane(Controller controller) {
        this.localPane = controller.getLocalPane();
        this.localListView = controller.getLocalListView();
        this.dataHandler = controller.getDataHandler();
        this.cPane = controller.getcPane();
        network = Network.getInstance();
        init();
    }
    
    private void init(){
        currentPath = START_DIR;
    
        propAlert = new Alert(Alert.AlertType.INFORMATION);
        propAlert.setGraphic(null);
        propAlert.setHeaderText(null);
        propAlert.setResizable(true);
    
        renameDialog = new TextInputDialog();
        renameDialog.setTitle("Enter new name");
        renameDialog.setHeaderText(null);
        renameDialog.setGraphic(null);
    
        fileNames = FXCollections.observableArrayList();
        updateListView(currentPath);
        
        localListView.setItems(fileNames);
    
        keyCombinationCopy = new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN);
        keyCombinationCut = new KeyCodeCombination(KeyCode.X, KeyCombination.CONTROL_DOWN);
        keyCombinationPaste = new KeyCodeCombination(KeyCode.V, KeyCombination.CONTROL_DOWN);
        keyCombinationProps = new KeyCodeCombination(KeyCode.ENTER, KeyCombination.ALT_DOWN);
        
        MenuItem refresh = new MenuItem(   "Refresh (F5)");
        MenuItem open = new MenuItem(      "Open (Enter)");
        MenuItem upload = new MenuItem(    "Upload (U)");
        MenuItem delete = new MenuItem(    "Delete (Del)");
        MenuItem rename = new MenuItem(    "Rename (F2)");
        MenuItem properties = new MenuItem("Properties (Alt + Enter)");
        
        contextMenuForFiles = new ContextMenu();
        contextMenuForFiles.getItems().addAll(refresh, open, upload, delete, rename, properties);
    
        MenuItem refreshDir = new MenuItem("Refresh (F5)");
        MenuItem openDir = new MenuItem("Open (Enter)");
        MenuItem deleteDir = new MenuItem("Delete (Del)");
        MenuItem renameDir = new MenuItem("Rename (F2)");
        MenuItem propertiesDir = new MenuItem("Properties (Alt + Enter)");
        
        contextMenuForDir = new ContextMenu();
        contextMenuForDir.getItems().addAll(refreshDir, openDir, deleteDir, renameDir, propertiesDir);
        
        refreshDir.setOnAction(event -> {
            refresh();
            event.consume();
        });
        
        refresh.setOnAction(event -> {
            refresh();
            event.consume();
        });
    
        openDir.setOnAction(event -> {
            open();
            event.consume();
        });
        
        open.setOnAction(event -> {
            open();
            event.consume();
        });
    
        upload.setOnAction(event -> {
            upload();
            event.consume();
        });
    
        deleteDir.setOnAction(event -> {
            delete();
            event.consume();
        });
        
        delete.setOnAction(event -> {
            delete();
            event.consume();
        });
    
        renameDir.setOnAction(event -> {
            rename();
            event.consume();
        });
        
        rename.setOnAction(event -> {
            rename();
            event.consume();
        });
    
        propertiesDir.setOnAction(event -> {
            getProperties();
            event.consume();
        });
        
        properties.setOnAction(event -> {
            getProperties();
            event.consume();
        });
    
        localListView.setCellFactory(param -> {
            ListCell<String> cell = new ListCell<>();
            cell.textProperty().bind(cell.itemProperty());
            cell.textProperty().addListener((observable, oldValue, newValue) -> {
                
                if(newValue != null) {
                    cell.setDisable(false);
                    //enable context menu only for files
                    if(!new File(currentPath + cell.textProperty().getValue()).isDirectory()) {
                        cell.setContextMenu(contextMenuForFiles);
                        cell.setOnDragDetected(v->{
                            Dragboard db = cell.startDragAndDrop(TransferMode.COPY);
                            ClipboardContent cc = new ClipboardContent();
                            cc.putString("lPane" + getSelectedItemPath().toString());
                            db.setContent(cc);
                            v.setDragDetect(true);
                        });
                        
                    } else {
                        cell.setContextMenu(contextMenuForDir);
                    }
                    cell.setOnMouseClicked(event-> {
                        if(event.getClickCount() == 2) {
                            open();
                            event.consume();
                        }
                    });
                } else {
                    cell.setDisable(true);
                }
            });
            return cell;
        });
    
        localListView.setOnDragOver(v->{
            if (v.getDragboard().hasString() &&
                    //accept drag and drop only if it does not come from lPane
                    !v.getDragboard().getString().startsWith("lPane") ||
                    v.getDragboard().hasFiles())
            {
                v.acceptTransferModes(TransferMode.COPY);
            }
            v.consume();
        });
    
        localListView.setOnDragDropped(v-> {
            Dragboard db = v.getDragboard();
            if (db.hasFiles()) {
                acceptFiles(db);
            } else {
                //cut off the mark
                String fileName = db.getString().substring(5);
                dataHandler.write(new AbstractMessage(MessageType.DOWNLOAD, fileName));
            }
            
            v.setDropCompleted(true);
            v.consume();
        });
        
        localListView.setOnKeyPressed(event -> {
            if (keyCombinationCopy.match(event)) {
                copy(false);
            } else if (keyCombinationCut.match(event)) {
                copy(true);
            } else if (keyCombinationPaste.match(event)) {
                paste();
            } else if (keyCombinationProps.match(event)) {
                getProperties();
            } else {
                switch (event.getCode()) {
                    case BACK_SPACE:
                    case ESCAPE:
                        //we can go up a directory tree until root directory
                        Path path = Paths.get(currentPath);
                        if(path.getParent() != null) {
                            currentPath = path.getParent().toString();
                            updateListView(currentPath);
                        }
                        break;
                    case ENTER:
                        open();
                        break;
                    case UP:
                        if(localListView.getSelectionModel().isEmpty()) {
                            localListView.getSelectionModel().selectLast();
                        }
                        break;
                    case F5:
                        refresh();
                        break;
                    case DELETE:
                        delete();
                        break;
                    case F2:
                        rename();
                        break;
                    case U:
                        upload();
                        break;
                }
            }
        });
    
        renameDialog.getEditor().setOnKeyPressed(event -> {
            if(event.getCode().equals(KeyCode.ENTER)) {
                renameDialog.getEditor().commitValue();
            }
        });
        
    }
    
    private void acceptFiles(Dragboard db) {
        for (int i = 0; i < db.getFiles().size(); i++) {
            File f = db.getFiles().get(i);
            try {
                doFileOperation(f.toPath(), Paths.get(currentPath), OperationType.COPY);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void refresh(){
        updateListView(currentPath);
    }
    
    private void upload() {
        if (!localListView.getSelectionModel().isEmpty() && Network.getInstance().isConnected()) {
            if (!Files.isDirectory(getSelectedItemPath())) {
                String fileName = getSelectedItemPath().getFileName().toString();
                byte[] fileBytes = null;
                try {
                    fileBytes = Files.readAllBytes(getSelectedItemPath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                dataHandler.write(new AbstractMessage(MessageType.FILE, fileName, fileBytes));
            }
        }
    }
    
    private void open() {
        if (!localListView.getSelectionModel().isEmpty()) {
            //if we tried to open a directory, open it in our list view
            if(Files.isDirectory(getSelectedItemPath())) {
                updateListView(getSelectedItemPath().toString());
                //if we tried to open a file, open it with a default application
            } else {
                try {
                    Desktop.getDesktop().open(getSelectedItemPath().toFile());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    private void copy(boolean move) {
        if (!localListView.getSelectionModel().isEmpty()) {
            this.move = move;
            srcPath = getSelectedItemPath();
        }
    }
    
    private void paste() {
        if(srcPath != null) {
            //if nothing is selected or selected item is a file destination path is our current directory
            if (localListView.getSelectionModel().isEmpty() || !Files.isDirectory(getSelectedItemPath()))
                dstPath = Paths.get(currentPath);
                //if selected item is a directory, destination path is that directory
            else dstPath = getSelectedItemPath();
    
            try {
                if (move) {
                    doFileOperation(srcPath, dstPath, OperationType.MOVE);
                    move = false;
                } else {
                    doFileOperation(srcPath, dstPath, OperationType.COPY);
                }
                srcPath = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    enum OperationType {
        COPY, MOVE, DELETE, RENAME, GET_SIZE
    }
    
    private void doFileOperation(Path src, Path dst, OperationType type) throws IOException {
        if (Files.isDirectory(src)) {
            Files.walkFileTree(src, new FileVisitor<Path>() {
                Path dstDir = dst;
                Path dstFile;
                
                
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    dstDir = Paths.get(dstDir + File.separator + dir.getFileName());
                    
                    switch (type) {
                        case RENAME:
                            int replaceIndex = src.getNameCount() - 1;
                            String newName = dst.getFileName().toString();
                            Path temp = Paths.get(dir.toString());
                            Path firstPart = dir.subpath(0, replaceIndex);
                            dir = temp;
                            dstDir = Paths.get(dir.getRoot() + File.separator + firstPart + File.separator + newName);
                            if (dir.getNameCount() - 1 > replaceIndex) {
                                Path secondPart = dir.subpath(replaceIndex + 1, dir.getNameCount());
                                dstDir = Paths.get(dstDir + File.separator + secondPart);
                            }
                        case COPY:
                        case MOVE:
                            System.out.println("previsiting " + dir.toString() + " trying to create " + dstDir.toString());
                            if (Files.notExists(dstDir)) Files.createDirectory(dstDir);
                            break;
                        case GET_SIZE:
                            numberOfDirs++;
                            break;
                    }
                    return FileVisitResult.CONTINUE;
                }
    
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    dstFile = Paths.get(dstDir + File.separator + file.getFileName());
        
                    switch (type) {
                        case COPY:
                            Files.copy(file, dstFile, StandardCopyOption.REPLACE_EXISTING);
                            System.out.println("copying " + file.toString() + " to " + dstFile.toString());
                            break;
                        case RENAME:
                        case MOVE:
                            Files.move(file, dstFile, StandardCopyOption.REPLACE_EXISTING);
                            System.out.println("moving " + file.toString() + " to " + dstFile.toString());
                            break;
                        case DELETE:
                            Files.delete(file);
                            System.out.println("deleting " + file.toString());
                            break;
                        case GET_SIZE:
                            size += Files.size(file);
                            numberOfFiles++;
                            break;
                    }
                    return FileVisitResult.CONTINUE;
                }
    
                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
    
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    if (dstDir != null) {
                        dstDir = dstDir.getParent();
                    }
                    switch (type) {
                        case RENAME:
                        case MOVE:
                        case DELETE:
                            Files.delete(dir);
                            System.out.println("deleting dir " + dir.toString());
                            break;
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            switch (type) {
                case COPY:
                    Files.copy(src, Paths.get(dst + File.separator + src.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                    break;
                case RENAME:
                    if (Files.notExists(dst)) {
                        Files.move(src, dst);
                    } else {
                        Alert fileExistsAlert = new Alert(Alert.AlertType.ERROR);
                        fileExistsAlert.setGraphic(null);
                        fileExistsAlert.setHeaderText(null);
                        fileExistsAlert.setTitle(null);
                        fileExistsAlert.setContentText("A file with this name already exists!");
                        fileExistsAlert.showAndWait();
                    }
                    break;
                case MOVE:
                    Files.move(src, Paths.get(dst + File.separator + src.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                    break;
                case DELETE:
                    Files.delete(src);
                    break;
                case GET_SIZE:
                    size = Files.size(src);
                    break;
            }
        }
        updateListView(currentPath);
    }
    
    private void delete() {
        if (!localListView.getSelectionModel().isEmpty()) {
            try {
                doFileOperation(getSelectedItemPath(), null, OperationType.DELETE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void rename() {
        if (!localListView.getSelectionModel().isEmpty()) {
            Path p = getSelectedItemPath();
            renameDialog.getEditor().setText(p.getFileName().toString());
            renameDialog.showAndWait();
            if(renameDialog.getResult() != null) {
                String newName = renameDialog.getResult();
                dstPath = Paths.get(currentPath + newName);
                try {
                    doFileOperation(p, dstPath, OperationType.RENAME);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    private void getProperties() {
        if (!localListView.getSelectionModel().isEmpty()) {
            Path p = getSelectedItemPath();
            try {
                String name = p.getFileName().toString();
                String location = p.toString();
                doFileOperation(p, null, OperationType.GET_SIZE);
                StringBuilder byteSize = new StringBuilder(Long.toString(size));
                for (int i = byteSize.length() - 3; i > 0 ; i -= 3) {
                    byteSize.insert(i, ' ');
                }
                String sizeStr = Long.toString(size) + " bytes";
                if(size >= 1073741824L) sizeStr = String.format("%.2f", (float)size/1073741824L) + " Gb (" + byteSize + " bytes)";
                else if(size >= 1048576L) sizeStr = String.format("%.2f", (float)size/1048576L) + " Mb (" + byteSize + " bytes)";
                else if(size >= 1024L) sizeStr = String.format("%.2f", (float)size/1024L) + " Kb (" + byteSize + " bytes)";
                StringBuilder contentText = new StringBuilder();
                contentText.append("Name: ").append(name).append("\n").append("Location: ").append(location).append("\n").append("Size: ").append(sizeStr);
                if (Files.isDirectory(p)) {
                    if (numberOfDirs < 0) numberOfDirs = 0;
                    contentText.append("\n" + "Contains: ").append(numberOfFiles).append(" files, ").append(numberOfDirs).append(" folders");
                }
                propAlert.setContentText(contentText.toString());
                propAlert.setTitle(name + " properties");
            } catch (IOException e) {
                e.printStackTrace();
            }
            propAlert.showAndWait();
            size = 0;
            numberOfDirs = 0;
            numberOfFiles = 0;
        }
    }
    
    private void updateListView(String path) {
        //if current path is directory, and does not end with \, append it
        if(new File(path).isDirectory() && !path.endsWith("\\")) {
            currentPath = path + File.separator;
        } else {
            currentPath = path;
        }
        updateFileNames(currentPath);
        //localListView.setItems(fileNames);
        localPane.setText(currentPath);
    }
    
    private Path getSelectedItemPath() {
        return Paths.get(currentPath + localListView.getSelectionModel().getSelectedItem());
    }
    
    private void updateFileNames(String path) {
        fileNames.clear();
        Path p = Paths.get(path);
        try {
            DirectoryStream<Path> directoryStream = Files.newDirectoryStream(p);
            for (Path o: directoryStream) {
                //add directories to the top of our list
                if(Files.isDirectory(o)) {
                    fileNames.add(0, o.getFileName().toString());
                } else {
                    fileNames.add(o.getFileName().toString());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    void createNewFile(AbstractMessage data) {
        String fileName = (String) data.getObjects()[0];
        byte[] fileBytes = (byte[]) data.getObjects()[1];
        Path fileToCreate = Paths.get(currentPath + fileName);
        try {
            Files.deleteIfExists(fileToCreate);
            Files.createFile(fileToCreate);
            Files.write(fileToCreate, fileBytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        updateFileNames(currentPath);
    }
}
