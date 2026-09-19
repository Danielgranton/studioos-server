package com.studioos.server.shared.media;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.grpc.Server;
import io.grpc.ServerServiceDefinition;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class GrpcMediaCallbackServerConfig {

    @Bean(destroyMethod = "shutdownNow")
    @ConditionalOnProperty(
            name = "media.callback.grpc.enabled",
            havingValue = "true",
            matchIfMissing = true)
    public Server mediaCallbackGrpcServer(
            @Value("${media.callback.grpc.host:127.0.0.1}") String host,
            @Value("${media.callback.grpc.port:50052}") int port,
            @Value("${media.callback.grpc.allow-non-loopback:false}") boolean allowNonLoopback,
            GrpcMediaCallbackService mediaCallbackService) throws IOException {

        if (!isLoopback(host) && !allowNonLoopback) {
            throw new IllegalStateException(
                    "Media callback gRPC must bind to loopback. Configure a protected private network before using "
                            + "MEDIA_CALLBACK_GRPC_HOST=" + host + ", or explicitly set "
                            + "MEDIA_CALLBACK_GRPC_ALLOW_NON_LOOPBACK=true");
        }
        if (!isLoopback(host)) {
            log.warn("Media callback gRPC is exposed on {}; protect port {} with a private network or mTLS", host, port);
        }

        ServerServiceDefinition serviceDefinition = mediaCallbackService.bindService();
        Server server = NettyServerBuilder.forAddress(new java.net.InetSocketAddress(host, port))
                .addService(serviceDefinition)
                .build()
                .start();

        return server;
    }

    private boolean isLoopback(String host) {
        return "127.0.0.1".equals(host) || "::1".equals(host) || "localhost".equalsIgnoreCase(host);
    }
}
