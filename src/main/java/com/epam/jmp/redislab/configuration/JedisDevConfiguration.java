package com.epam.jmp.redislab.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;

import java.util.HashSet;
import java.util.Set;

//@Configuration
//@Profile("DEV")
public class JedisDevConfiguration {
    private static final HostAndPort HOST_AND_PORT = new HostAndPort("localhost", 30000);

    @Bean
    // https://www.baeldung.com/jedis-java-redis-client-library
    // https://stackoverflow.com/questions/30078034/redis-cluster-in-multiple-threads
    public JedisCluster jedisCluster() {
        Set<HostAndPort> jedisClusterNodes = new HashSet<HostAndPort>();
        jedisClusterNodes.add(HOST_AND_PORT);
        return new JedisCluster(jedisClusterNodes);
    }
}
