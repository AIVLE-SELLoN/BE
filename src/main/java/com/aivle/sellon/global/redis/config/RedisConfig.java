package com.aivle.sellon.global.redis.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

// Cluster Mode Disabled ElastiCache 기준 standalone 연결.
// 로컬 컨테이너는 AUTH/TLS가 없어 password가 비어있으면 인증을 건너뛴다.
// ElastiCache(전송 중 암호화 활성화)는 AUTH 토큰과 TLS가 강제되므로 prod에서는 둘 다 켠다.
@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Value("${spring.data.redis.password:}")
    private String password;

    @Value("${spring.data.redis.ssl.enabled:false}")
    private boolean sslEnabled;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration standaloneConfig = new RedisStandaloneConfiguration(host, port);
        if (!password.isBlank()) {
            standaloneConfig.setPassword(RedisPassword.of(password));
        }

        LettuceClientConfiguration clientConfig = sslEnabled
                ? LettuceClientConfiguration.builder().useSsl().build()
                : LettuceClientConfiguration.builder().build();

        return new LettuceConnectionFactory(standaloneConfig, clientConfig);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());

        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new StringRedisSerializer());

        return redisTemplate;
    }
}
