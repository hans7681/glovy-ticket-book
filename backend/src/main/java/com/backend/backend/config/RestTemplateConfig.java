package com.backend.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import java.io.IOException;
import java.time.Duration;

/**
 * RestTemplate 配置类
 */
@Configuration
public class RestTemplateConfig { // Renamed back

    @Value("${tmdb.api.accessToken}")
    private String tmdbAccessToken;

    // Proxy settings are now handled by JVM arguments (-Dhttps.proxyHost...)

    /**
     * 创建 RestTemplate Bean
     * 配置适当的连接和读取超时时间
     * 添加拦截器以自动添加 TMDB 的 Bearer Token 和 Accept 头
     * @param builder RestTemplateBuilder
     * @return RestTemplate 实例
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        // RestTemplateBuilder automatically uses JVM proxy settings if configured
        return builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(30))
                .additionalInterceptors(new TmdbAuthInterceptor(tmdbAccessToken))
                .build();
    }

    // 内部类实现拦截器
    private static class TmdbAuthInterceptor implements ClientHttpRequestInterceptor {
        private final String accessToken;

        public TmdbAuthInterceptor(String accessToken) {
            this.accessToken = accessToken;
        }

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
            // 只对访问 TMDB API 的请求添加认证头
            if (request.getURI().getHost().contains("themoviedb.org")) {
                 request.getHeaders().setBearerAuth(this.accessToken);
                 request.getHeaders().set(HttpHeaders.ACCEPT, "application/json");
            }
            return execution.execute(request, body);
        }
    }
} 