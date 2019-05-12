package com.cloud.storage.client;


import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;

import java.io.IOException;
import java.net.Socket;

class Network {
    
    private static Network ourInstance = new Network();
    
    static Network getInstance() {
        return ourInstance;
    }
    
    private String host = "127.0.0.1";
    private int port = 8080;
    private int MAX_OBJ_SIZE = 1024 * 1024 * 100;
    private Socket socket;
    private ObjectDecoderInputStream in;
    private ObjectEncoderOutputStream out;
    
    ObjectDecoderInputStream getIn() {
        return in;
    }
    
    boolean isConnected() {
        return socket != null && !socket.isClosed() && socket.isConnected();
    }
    
    void write(Object obj) throws IOException {
        out.writeObject(obj);
        out.flush();
    }
    
    void close(){
        try {
            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    void connect(){
        try {
            socket = new Socket(host, port);
            out = new ObjectEncoderOutputStream(socket.getOutputStream());
            in = new ObjectDecoderInputStream(socket.getInputStream(), MAX_OBJ_SIZE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private Network(){
    }
}
