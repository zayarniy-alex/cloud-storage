package com.cloud.storage.common;

public enum MessageType {
    // auth (String[] login, passwd)
    //file_list (ArrayList<String[]> { {name, size, isDirectory} } )
    //file (String name, byte[] fileBytes)
    //download (String fileName)
    //rename (String oldName, String newName)
    //delete (String fileName)
    AUTH, DEAUTH, AUTH_OK, AUTH_FAILED, UPLOAD_FINISHED, FILE_LIST, FILE, DOWNLOAD, RENAME, DELETE
}
