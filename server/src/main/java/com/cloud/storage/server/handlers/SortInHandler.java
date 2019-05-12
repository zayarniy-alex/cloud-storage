package com.cloud.storage.server.handlers;

import com.cloud.storage.common.AbstractMessage;
import com.cloud.storage.server.ServerUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class SortInHandler extends ChannelInboundHandlerAdapter implements CallbackInterface{
    
    private String username;
    private ChannelHandlerContext ctx;
    private ServerUtils serverUtils;
    
    SortInHandler(String username) {
        this.username = username;
        serverUtils = new ServerUtils(username);
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        this.ctx = ctx;
        if(msg == null) return;
        AbstractMessage data = (AbstractMessage) msg;
        switch (data.getType()) {
            //client requested file list
            case FILE_LIST:
                sendNewFileList();
                break;
                //client uploaded a file
            case FILE:
                serverUtils.saveFile(this, data);
                break;
                //client requested a file download
            case DOWNLOAD:
                ctx.writeAndFlush(serverUtils.packRequestedFile(data));
                break;
                //client requested file renaming
            case RENAME:
                serverUtils.renameRequestedFile(this, data);
                break;
                //client requested file deletion
            case DELETE:
                serverUtils.deleteRequestedFile(this, data);
                break;
                //client logged out
            case DEAUTH:
                ctx.pipeline().addAfter("SortInHandler", "AuthHandler", new AuthHandler());
                ctx.pipeline().remove(this);
                break;
        }
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
    
    @Override
    public void sendNewFileList() {
        ctx.writeAndFlush(serverUtils.generateFileList());
    }
    
    
}
