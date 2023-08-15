package com.epam.jmp.redislab.service;

import com.epam.jmp.redislab.api.RequestDescriptor;
import com.epam.jmp.redislab.configuration.ratelimit.RateLimitRule;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Refill;

import java.time.Duration;
import java.util.Set;

public enum RateLimitRuleMatcherImpl implements RateLimitRuleMatcher {
    DEFAULT {
        public Bandwidth getLimit() {
            return Bandwidth.classic(2,
                    Refill.intervally(2, Duration.ofMinutes(1)));
        }
    },
    SLOW {
        public Bandwidth getLimit() {
            return Bandwidth.classic(1,
                    Refill.intervally(1, Duration.ofMinutes(1)));
        }
    },
    CLIENT_IP {
        public Bandwidth getLimit() {
            return Bandwidth.classic(1,
                    Refill.intervally(1, Duration.ofHours(1)));
        }
    },
    IMPORTANT {
        public Bandwidth getLimit() {
            return Bandwidth.classic(10,
                    Refill.intervally(10, Duration.ofMinutes(1)));
        }
    };

    public static boolean isDescriptorMatchesToRule(RequestDescriptor descriptor, RateLimitRule rule) {
        if (descriptor.getAccountId().isPresent() && rule.getAccountId().equals(descriptor.getAccountId())) {
            return true;
        } else if (descriptor.getRequestType().isPresent() && rule.getRequestType().equals(descriptor.getRequestType())) {
            return true;
        } else return descriptor.getClientIp().isPresent() && rule.getClientIp().equals(descriptor.getClientIp());
    }

    public static RateLimitRuleMatcherImpl compareDescriptorToRule(RequestDescriptor descriptor, RateLimitRule rule) {
        if (descriptor.getAccountId().isPresent() && rule.getAccountId().equals(descriptor.getAccountId())) {
            return IMPORTANT;
        } else if (descriptor.getRequestType().isPresent() && rule.getRequestType().equals(descriptor.getRequestType())) {
            return SLOW;
        } else if (descriptor.getClientIp().isPresent() && rule.getClientIp().equals(descriptor.getClientIp())) {
            return CLIENT_IP;
        } else return DEFAULT;
    }
}
