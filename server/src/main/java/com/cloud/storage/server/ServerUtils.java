package com.cloud.storage.server;

import com.cloud.storage.common.AbstractMessage;
import com.cloud.storage.common.MessageType;
import com.cloud.storage.server.handlers.CallbackInterface;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;

public class ServerUtils {
    private static final String STORAGE_DIR = "D:\\server";
    private Path userDir;
    
    public ServerUtils(String username) {
        userDir = Paths.get(STORAGE_DIR + File.separator + username);
    }
    
    public AbstractMessage generateFileList() {
        if (Files.notExists(userDir)) {
            try {
                Files.createDirectory(userDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        ArrayList<String[]> filesInfo = new ArrayList<>();
        try {
            DirectoryStream<Path> directoryStream = Files.newDirectoryStream(userDir);
            for (Path p : directoryStream) {
                filesInfo.add(new String[]{p.getFileName().toString(), Long.toString(Files.size(p)), Boolean.toString(Files.isDirectory(p))});
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new AbstractMessage(MessageType.FILE_LIST, filesInfo);
    }
    
    public void saveFile(CallbackInterface callback, AbstractMessage data) {
        String fileName = (String) data.getObjects()[0];
        byte[] fileBytes = (byte[]) data.getObjects()[1];
        Path fileToCreate = Paths.get(userDir + File.separator + fileName);
        try {
            Files.deleteIfExists(fileToCreate);
            Files.createFile(fileToCreate);
            Files.write(fileToCreate, fileBytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        callback.sendNewFileList();
    }
    
    public AbstractMessage packRequestedFile(AbstractMessage data) {
        String fileName = (String) data.getObjects()[0];
        Path fileToDownload = Paths.get(userDir + File.separator + fileName);
        byte[] fileBytes = null;
        try {
            fileBytes = Files.readAllBytes(fileToDownload);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new AbstractMessage(MessageType.FILE, fileName, fileBytes);
    }
    public void renameRequestedFile(CallbackInterface callback, AbstractMessage data) {
        String oldName = (String) data.getObjects()[0];
        String newName = (String) data.getObjects()[1];
        Path fileToRename = Paths.get(userDir + File.separator + oldName);
        Path fileWithNewName = Paths.get(userDir + File.separator + newName);
        try {
            Files.move(fileToRename, fileWithNewName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        callback.sendNewFileList();
    }
    public void deleteRequestedFile(CallbackInterface callback, AbstractMessage data) {
        String fileName = (String) data.getObjects()[0];
        Path fileToDelete = Paths.get(userDir + File.separator + fileName);
        try {
            Files.delete(fileToDelete);
        } catch (IOException e) {
            e.printStackTrace();
        }
        callback.sendNewFileList();
    }
}

