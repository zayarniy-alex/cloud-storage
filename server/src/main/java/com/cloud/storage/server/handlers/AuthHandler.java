package com.cloud.storage.server.handlers;

import com.cloud.storage.common.AbstractMessage;
import com.cloud.storage.common.MessageType;
import com.cloud.storage.server.ServerMainClass;
import com.cloud.storage.server.ServerUtils;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.sqlite.JDBC;

import java.sql.*;


public class AuthHandler extends ChannelInboundHandlerAdapter {
    
    private Connection cn;
    private PreparedStatement prepStmnt;
    
    public AuthHandler() {
       this.cn = ServerMainClass.getConnection();
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("unauthorized client connected!");
        
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //do not accept messages, other than auth
        if(msg == null || ((AbstractMessage) msg).getType() != MessageType.AUTH) {
            return;
        }
        AbstractMessage data = (AbstractMessage) msg;
        String username = (String) data.getObjects()[0];
        String passwd = (String) data.getObjects()[1];
       
        prepStmnt = cn.prepareStatement("SELECT * FROM users WHERE username=?;");
        prepStmnt.setString(1, username);
        ResultSet rs = prepStmnt.executeQuery();
        if (rs.next()) {
        //if we found the username, continue to checking the password
            prepStmnt = cn.prepareStatement("SELECT password FROM users WHERE username=?;");
            prepStmnt.setString(1, username);
            rs = prepStmnt.executeQuery();
            if(rs.next()) {
                if (!rs.getString(1).equals(passwd)) {
                    ctx.writeAndFlush(new AbstractMessage(MessageType.AUTH_FAILED));
                    return;
                }
            }
            //if there is no such username in the database, sign up this user
        } else {
            prepStmnt = cn.prepareStatement("INSERT INTO users (username, password) VALUES (?, ?);");
            prepStmnt.setString(1, username);
            prepStmnt.setString(2, passwd);
            prepStmnt.execute();
        }
        
        ctx.writeAndFlush(new AbstractMessage(MessageType.AUTH_OK));
        prepStmnt.close();
        //add sorter to the pipeline, and remove auth handler
        ctx.pipeline().addAfter("AuthHandler", "SortInHandler", new SortInHandler(username));
        ctx.pipeline().remove(this);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
