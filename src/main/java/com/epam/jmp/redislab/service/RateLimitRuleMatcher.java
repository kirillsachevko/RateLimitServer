package com.epam.jmp.redislab.service;

import io.github.bucket4j.Bandwidth;

public interface RateLimitRuleMatcher {

    Bandwidth getLimit();
}
