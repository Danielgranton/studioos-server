package com.studioos.server.shared.media;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;

@Configuration
public class GrpcMediaCallbackServerConfig {

    @Bean(destroyMethod = "shutdownNow")
    @ConditionalOnProperty(
            name = "media.callback.grpc.enabled",
            havingValue = "true",
            matchIfMissing = true)
    public Server mediaCallbackGrpcServer(
            @Value("${media.callback.grpc.host:0.0.0.0}") String host,
            @Value("${media.callback.grpc.port:50052}") int port,
            GrpcMediaCallbackService mediaCallbackService) throws IOException {

        Server server = NettyServerBuilder.forAddress(new java.net.InetSocketAddress(host, port))
                .addService(mediaCallbackService)
                .build()
                .start();

        return server;
    }
}
