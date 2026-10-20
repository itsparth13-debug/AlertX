package com.yourname.safereport.utils;

import java.security.SecureRandom;

/**
 * Utility for generating tracking codes in the format INC-XXXXXXXX.
 */
public class TrackingCodeGenerator {

    private static final String ALPHA_NUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom random = new SecureRandom();

    public static String generateTrackingCode() {
        StringBuilder builder = new StringBuilder("INC-");
        for (int i = 0; i < 8; i++) {
            int index = random.nextInt(ALPHA_NUMERIC.length());
            builder.append(ALPHA_NUMERIC.charAt(index));
        }
        return builder.toString();
    }
}
