package com.mootann.arxivdaily;

import java.time.Duration;

import javax.net.ssl.SSLException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.reactive.function.client.WebClient;

import com.mootann.arxivdaily.config.ArxivProxyConfig;
import com.mootann.arxivdaily.config.GitHubConfig;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;

@Slf4j
@SpringBootApplication
@EnableScheduling
public class ArXivDailyApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArXivDailyApplication.class, args);
    }

    @Bean
    public WebClient webClient(ArxivProxyConfig arxivProxyConfig, GitHubConfig gitHubConfig) {
        HttpClient httpClient = HttpClient.create()
            .followRedirect(true)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000)
            .responseTimeout(Duration.ofSeconds(60))
            .doOnConnected(conn -> 
                conn.addHandlerLast(new ReadTimeoutHandler(60))
                .addHandlerLast(new WriteTimeoutHandler(60)));
        
        // 配置SSL信任所有证书
        SslContext sslContext;
        try {
            sslContext = SslContextBuilder
                .forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();
        } catch (SSLException e) {
            log.warn("配置SSL上下文失败，使用默认配置", e);
            sslContext = null;
        }
        
        if (sslContext != null) {
            SslContext finalSslContext = sslContext;
            httpClient = httpClient.secure(sslContextSpec ->
                sslContextSpec.sslContext(finalSslContext)
                            .handshakeTimeout(Duration.ofSeconds(30))
                            .closeNotifyFlushTimeout(Duration.ofSeconds(10))
                            .closeNotifyReadTimeout(Duration.ofSeconds(10)));
        }
        
        // 配置代理（优先使用arXiv代理配置）
        if (arxivProxyConfig.getEnabled() != null && arxivProxyConfig.getEnabled()
            && arxivProxyConfig.getHost() != null && arxivProxyConfig.getPort() != null) {
            httpClient = httpClient.proxy(proxy -> 
                proxy.type(ProxyProvider.Proxy.HTTP)
                    .host(arxivProxyConfig.getHost())
                    .port(arxivProxyConfig.getPort()));
            log.info("WebClient配置代理: {}:{}", arxivProxyConfig.getHost(), arxivProxyConfig.getPort());
        } else if (gitHubConfig.getProxyEnabled() != null && gitHubConfig.getProxyEnabled()
            && gitHubConfig.getProxyHost() != null && gitHubConfig.getProxyPort() != null) {
            httpClient = httpClient.proxy(proxy -> 
                proxy.type(ProxyProvider.Proxy.HTTP)
                    .host(gitHubConfig.getProxyHost())
                    .port(gitHubConfig.getProxyPort()));
            log.info("WebClient配置代理: {}:{}", gitHubConfig.getProxyHost(), gitHubConfig.getProxyPort());
        }
        
        return WebClient.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(50 * 1024 * 1024))
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
    }

}
