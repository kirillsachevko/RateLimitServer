package com.epam.jmp.redislab.configuration;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.redis.jedis.cas.JedisBasedProxyManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;

@Configuration
@Profile("LOCAL")
public class JedisLocalConfiguration {

    private static final HostAndPort HOST_AND_PORT = new HostAndPort("localhost", 6379);

    @Bean
    public JedisPool jedisPool() {
        return new JedisPool(buildPoolConfig(), HOST_AND_PORT.getHost(), HOST_AND_PORT.getPort());
    }

    @Bean
    JedisBasedProxyManager<byte[]> proxyManager(JedisPool jedisPool) {
        return JedisBasedProxyManager.builderFor(jedisPool)
                .withExpirationStrategy(ExpirationAfterWriteStrategy
                        .basedOnTimeForRefillingBucketUpToMax(Duration.ofSeconds(10)))
                .build();
    }

    private JedisPoolConfig buildPoolConfig() {
        final JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(128);
        poolConfig.setMaxIdle(128);
        poolConfig.setMinIdle(16);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setNumTestsPerEvictionRun(3);
        poolConfig.setBlockWhenExhausted(true);
        return poolConfig;
    }
}
