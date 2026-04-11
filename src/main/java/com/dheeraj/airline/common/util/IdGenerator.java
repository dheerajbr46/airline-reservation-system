package com.dheeraj.airline.common.util;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class IdGenerator {

    private static final int CODE_LENGTH = 6;

    public String generateCode(String prefix) {
        String randomPart = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .toUpperCase()
                .substring(0, CODE_LENGTH);

        return prefix.toUpperCase() + "-" + randomPart;
    }
}
