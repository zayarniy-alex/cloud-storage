package com.cloud.storage.server;

import com.cloud.storage.server.handlers.AuthHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import org.sqlite.JDBC;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ServerMainClass {
    
    private int port = 8080;
    private int MAX_OBJ_SIZE = 1024 * 1024 * 100;
    private static Connection cn;
    
    public static Connection getConnection() {
        return cn;
    }
    
    private void run() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() { // (4)
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new ObjectEncoder());
                            ch.pipeline().addLast(new ObjectDecoder(MAX_OBJ_SIZE, ClassResolvers.cacheDisabled(null)));
                            ch.pipeline().addLast("AuthHandler", new AuthHandler());
                        }
                    })
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            
            ChannelFuture f = b.bind(port).sync();
            
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
            cn.close();
        }
    }
    
    public static void main(String[] args) throws Exception {
        connectToDatabase();
        new ServerMainClass().run();
    }
    
    private static void connectToDatabase() {
        try {
            DriverManager.registerDriver(new JDBC());
            cn = DriverManager.getConnection("jdbc:sqlite:users.db");
            cn.setAutoCommit(true);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
