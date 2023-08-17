package com.epam.jmp.redislab.service;

import com.epam.jmp.redislab.api.RequestDescriptor;
import com.epam.jmp.redislab.configuration.ratelimit.RateLimitRule;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.redis.jedis.cas.JedisBasedProxyManager;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@Component
@DependsOn({"rateLimitRules"})
@Profile("LOCAL")
public class JedisRateLimitService implements RateLimitService {

    private final Set<RateLimitRule> rateLimitRules;
    private final JedisBasedProxyManager<byte[]> proxyManager;

    public JedisRateLimitService(Set<RateLimitRule> rateLimitRules, JedisBasedProxyManager<byte[]> proxyManager) {
        this.rateLimitRules = rateLimitRules;
        this.proxyManager = proxyManager;
    }

    @Override
    public boolean shouldLimit(Set<RequestDescriptor> requestDescriptors) {
        AtomicBoolean shouldLimit = new AtomicBoolean(true);
        requestDescriptors.forEach(requestDescriptor -> {
            AtomicReference<RateLimitRuleMatcherImpl> key = new AtomicReference<>();
            rateLimitRules.forEach(rateLimitRule -> {
                if (RateLimitRuleMatcherImpl.isDescriptorMatchesToRule(requestDescriptor, rateLimitRule)) {
                    key.set(resolveKey(requestDescriptor, rateLimitRule));
                }
            });
            if (Objects.isNull(key.get())) {
                key.set(RateLimitRuleMatcherImpl.DEFAULT);
            }
            Bucket bucket = resolveBucket(key.get().getLimit(), key.get());
            if (bucket.tryConsume(1)) {
                shouldLimit.getAndSet(false);
            }
        });

        return shouldLimit.get();
    }

    public Bucket resolveBucket(Bandwidth bandwidth, RateLimitRuleMatcherImpl matcher) {
        Supplier<BucketConfiguration> configSupplier = addRateLimiterBucketToCache(bandwidth);
        return proxyManager.builder().build(matcher.name().getBytes(StandardCharsets.UTF_8), configSupplier);
    }

    private Supplier<BucketConfiguration> addRateLimiterBucketToCache(Bandwidth bandwidth) {
        return () -> BucketConfiguration.builder()
                .addLimit(bandwidth)
                .build();
    }

    private RateLimitRuleMatcherImpl resolveKey(RequestDescriptor descriptor, RateLimitRule rule) {
        return RateLimitRuleMatcherImpl
                .compareDescriptorToRule(descriptor, rule);
    }
}
