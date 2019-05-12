package com.cloud.storage.common;

import java.io.Serializable;

public class AbstractMessage implements Serializable {
    
    private MessageType type;
    private Object[] objects;
    
    public AbstractMessage(MessageType type, Object... objects) {
        this.type = type;
        if(objects != null) {
            this.objects = objects;
        }
    }
    
    public MessageType getType() {
        return type;
    }
    
    public Object[] getObjects() {
        return objects;
    }
}
