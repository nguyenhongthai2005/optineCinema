package com.opticine.util;

import java.net.URI;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class YouTubeUtils {
    private static final Pattern VIDEO_ID = Pattern.compile("^[A-Za-z0-9_-]{11}$");

    private YouTubeUtils() {
    }

    public static Optional<String> extractVideoId(String url) {
        if (url == null || url.isBlank()) return Optional.empty();
        String trimmed = url.trim();
        if (VIDEO_ID.matcher(trimmed).matches()) return Optional.of(trimmed);

        try {
            URI uri = URI.create(trimmed);
            String host = uri.getHost();
            if (host == null) return Optional.empty();
            host = host.toLowerCase();
            if (host.startsWith("www.")) host = host.substring(4);
            if (!host.equals("youtube.com") && !host.equals("youtu.be") && !host.equals("m.youtube.com")) {
                return Optional.empty();
            }

            if (host.equals("youtu.be")) {
                return firstValidPathSegment(uri.getPath());
            }

            String path = uri.getPath() == null ? "" : uri.getPath();
            if (path.startsWith("/embed/") || path.startsWith("/shorts/")) {
                return firstValidPathSegment(path.substring(path.indexOf('/', 1) + 1));
            }

            String query = uri.getQuery();
            if (query != null) {
                for (String part : query.split("&")) {
                    String[] pair = part.split("=", 2);
                    if (pair.length == 2 && pair[0].equals("v") && VIDEO_ID.matcher(pair[1]).matches()) {
                        return Optional.of(pair[1]);
                    }
                }
            }
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    public static String embedUrl(String videoId) {
        return videoId == null || videoId.isBlank() ? null : "https://www.youtube.com/embed/" + videoId;
    }

    private static Optional<String> firstValidPathSegment(String path) {
        if (path == null) return Optional.empty();
        String cleaned = path.startsWith("/") ? path.substring(1) : path;
        String first = cleaned.split("/", 2)[0];
        Matcher matcher = VIDEO_ID.matcher(first);
        return matcher.matches() ? Optional.of(first) : Optional.empty();
    }
}
