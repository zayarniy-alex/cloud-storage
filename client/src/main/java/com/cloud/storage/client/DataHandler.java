package com.cloud.storage.client;

import com.cloud.storage.common.AbstractMessage;

import java.io.IOException;
import java.net.SocketException;

class DataHandler {
    private DataUtilizer dataUtilizer;
    
    DataHandler(DataUtilizer dataUtilizer) {
        this.dataUtilizer = dataUtilizer;
    }
    
    private void read() {
        Thread t = new Thread(() -> {
            AbstractMessage data;
            while (Network.getInstance().isConnected()) {
                try {
                    data = (AbstractMessage) Network.getInstance().getIn().readObject();
            
                    if (data != null) {
                        switch (data.getType()) {
                            case FILE_LIST:
                                dataUtilizer.utilizeFileList(data);
                                break;
                            case AUTH_FAILED:
                                dataUtilizer.authFailed();
                                break;
                            case AUTH_OK:
                                dataUtilizer.authOk();
                                break;
                            case FILE:
                                dataUtilizer.utilizeFile(data);
                                break;
                            case UPLOAD_FINISHED:
                                dataUtilizer.uploadFinished();
                                break;
                        }
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (SocketException e) {
                    e.printStackTrace();
                    //if the connection with the server breaks, close the socket and show a user login screen
                    Network.getInstance().close();
                    dataUtilizer.deauth();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }
    
    void write(AbstractMessage data) {
        Thread t = new Thread(() -> {
            if (!Network.getInstance().isConnected()) {
                //if a connection with the server is not established, try to connect and launch a thread for reading
                Network.getInstance().connect();
                read();
            }
            try {
                Network.getInstance().write(data);
            } catch (IOException e) {
                e.printStackTrace();
                //if we could not send data, cast out a user to login screen
                dataUtilizer.deauth();
            }
        });
        t.setDaemon(true);
        t.start();
    }
}
