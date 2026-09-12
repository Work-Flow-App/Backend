package com.workflow.util;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MentionUtils {

    // Matches @ followed by alphanumeric characters or underscores,
    // ensuring it's at the start of the string or follows a space.
    private static final Pattern MENTION_PATTERN = Pattern.compile("(?<=^|\\s)@([a-zA-Z0-9_]+)");

    public static Set<String> extractUsernames(String text) {
        Set<String> usernames = new HashSet<>();
        if (text == null || text.isBlank()) {
            return usernames;
        }

        Matcher matcher = MENTION_PATTERN.matcher(text);
        while (matcher.find()) {
            usernames.add(matcher.group(1)); // group(1) is the username without the '@'
        }

        return usernames;
    }
}