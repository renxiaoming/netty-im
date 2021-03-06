package com.objcoding.netty.im.application;

import com.objcoding.netty.im.client.handler.LoginResponseHandler;
import com.objcoding.netty.im.client.handler.MessageResponseHandler;
import com.objcoding.netty.im.codec.PacketDecoder;
import com.objcoding.netty.im.codec.PacketEncoder;
import com.objcoding.netty.im.server.handler.AuthHandler;
import com.objcoding.netty.im.server.handler.LoginRequestHandler;
import com.objcoding.netty.im.server.handler.MessageRequestHandler;
import com.objcoding.netty.im.util.Spliter;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;

import javax.annotation.PostConstruct;

@Slf4j
@SpringBootApplication
public class NettyImApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(NettyImApplication.class).web(true).run(args);
    }

    @Bean
    public Bootstrap bootstrap() {
        EventLoopGroup group = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        // 1. 定义线程模型
        bootstrap.group(group)
                // 2. 定义IO模型
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                // 3. IO 处理逻辑
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    public void initChannel(NioSocketChannel ch) {
                        ch.pipeline().addLast(new Spliter());
                        ch.pipeline().addLast(new PacketDecoder());
                        ch.pipeline().addLast(new LoginResponseHandler());
                        ch.pipeline().addLast(new MessageResponseHandler());
                        ch.pipeline().addLast(new PacketEncoder());
                    }
                });
        return bootstrap;
    }

    @PostConstruct
    private void start() {
        // 1.创建线程模型
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        new ServerBootstrap()
                .group(bossGroup, workerGroup)
                // 2. 定义IO模型
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                // 3. 定义读写处理逻辑
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    public void initChannel(NioSocketChannel ch) {
                        // inBound，处理读数据的逻辑链
                        ch.pipeline().addLast(new Spliter());
                        ch.pipeline().addLast(new PacketDecoder());
                        ch.pipeline().addLast(new LoginRequestHandler());
                        ch.pipeline().addLast(new AuthHandler());
                        ch.pipeline().addLast(new MessageRequestHandler());
                        ch.pipeline().addLast(new PacketEncoder());

                        // outBound，处理写数据的逻辑链


                    }
                })
                .bind(8081)
                .addListener((future) -> {
                    if (future.isSuccess()) {
                        log.info("端口绑定成功!");
                    } else {
                        log.error("端口绑定失败!");
                    }
                });
    }
}
