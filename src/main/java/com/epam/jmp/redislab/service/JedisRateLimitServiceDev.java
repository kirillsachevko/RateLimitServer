package com.epam.jmp.redislab.service;

import com.epam.jmp.redislab.api.RequestDescriptor;
import com.epam.jmp.redislab.configuration.ratelimit.RateLimitRule;
import io.github.bucket4j.Bandwidth;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisCluster;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Component
@DependsOn({"rateLimitRules"})
@Profile("DEV")
public class JedisRateLimitServiceDev implements RateLimitService {

    private final Set<RateLimitRule> rateLimitRules;
    private final JedisCluster jedisCluster;

    public JedisRateLimitServiceDev(Set<RateLimitRule> rateLimitRules, JedisCluster jedisCluster) {
        this.rateLimitRules = rateLimitRules;
        this.jedisCluster = jedisCluster;
    }

    @Override
    public boolean shouldLimit(Set<RequestDescriptor> requestDescriptors) {
        AtomicBoolean shouldLimit = new AtomicBoolean(true);

        requestDescriptors.forEach(descriptor -> {
            AtomicReference<RateLimitRuleMatcherImpl> key = new AtomicReference<>();
            rateLimitRules.forEach(rateLimitRule -> {
                if (RateLimitRuleMatcherImpl.isDescriptorMatchesToRule(descriptor, rateLimitRule)) {
                    key.set(resolveKey(descriptor, rateLimitRule));
                }
            });

            if (Objects.isNull(key.get())) {
                key.set(RateLimitRuleMatcherImpl.DEFAULT);
            }

            boolean isTokenAvailable = resolveLimiter(key.get());
            shouldLimit.getAndSet(isTokenAvailable);
        });

        return shouldLimit.get();
    }

    private boolean resolveLimiter(RateLimitRuleMatcherImpl rateLimit) {
        Bandwidth ruleBandwidth = rateLimit.getLimit();
        String key = rateLimit.name();
        String tokensNumber = String.valueOf(ruleBandwidth.getRefillTokens());
        long expirationLimit = TimeUnit.SECONDS.convert(ruleBandwidth.getRefillPeriodNanos(), TimeUnit.NANOSECONDS);
        long limitKeyExist = jedisCluster.setnx(key, tokensNumber);

        if (limitKeyExist == 1) {
            jedisCluster.expire(key, expirationLimit);
        }

        int tokenAvailable = Integer.parseInt(jedisCluster.get(key));

        if (tokenAvailable > 0) {
            jedisCluster.decrBy(key, 1L);
            return false;
        }

        return true;
    }

    private RateLimitRuleMatcherImpl resolveKey(RequestDescriptor descriptor, RateLimitRule rule) {
        return RateLimitRuleMatcherImpl
                .compareDescriptorToRule(descriptor, rule);
    }
}
