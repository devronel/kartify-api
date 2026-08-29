package com.kartify.api.shared.helper;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.util.StringUtils;

public final class SlugUtil {
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9\\s-]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern REPEATING_DASHES = Pattern.compile("-+");

    private SlugUtil() {
        // Prevent instantiation of utility class
    }

    public static String toSlug(String input) {
        if (!StringUtils.hasText(input)) {
            return "";
        }

        // 1. Separate accented characters (e.g., 'é' becomes 'e' + accent mark)
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        
        // 2. Strip off the isolated accent marks and convert to lowercase
        String cleaned = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                                   .toLowerCase(Locale.ENGLISH);

        // 3. Remove all characters that are not letters, numbers, spaces, or hyphens
        cleaned = NON_ALPHANUMERIC.matcher(cleaned).replaceAll("");

        // 4. Convert spaces to hyphens and trim external padding
        cleaned = WHITESPACE.matcher(cleaned.trim()).replaceAll("-");

        // 5. Collapse multiple consecutive hyphens (e.g., "hello---world" -> "hello-world")
        return REPEATING_DASHES.matcher(cleaned).replaceAll("-");
    }
}
