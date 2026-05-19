package com.xinzhe.projectmentor.util;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ZipFileUtil {

    private static final Set<String> FILTERED_DIRECTORIES = Set.of(
            ".git",
            "target",
            "node_modules",
            "dist",
            "build",
            ".idea",
            ".vscode",
            "logs"
    );

    private static final List<String> BLOCKED_EXTENSIONS = List.of(
            ".jpg",
            ".png",
            ".gif",
            ".jpeg",
            ".webp",
            ".pdf",
            ".doc",
            ".docx",
            ".xls",
            ".xlsx",
            ".ppt",
            ".pptx",
            ".class",
            ".jar",
            ".exe",
            ".dll",
            ".zip",
            ".rar",
            ".7z",
            ".tar",
            ".gz",
            ".mp4",
            ".mp3"
    );

    private static final List<String> WHITE_LIST_EXTENSIONS = List.of(
            ".md",
            ".xml",
            ".yml",
            ".yaml",
            ".properties",
            ".java",
            ".sql",
            ".json"
    );

    private ZipFileUtil() {
    }

    public static boolean isDangerousPath(String entryName) {
        if (entryName == null || entryName.isBlank()) {
            return true;
        }

        String trimmedName = entryName.trim();
        if (trimmedName.indexOf('\0') >= 0) {
            return true;
        }

        String normalizedSlash = trimmedName.replace('\\', '/');
        if (normalizedSlash.startsWith("/") || normalizedSlash.startsWith("//")) {
            return true;
        }

        if (trimmedName.matches("^[a-zA-Z]:.*") || normalizedSlash.matches("^[a-zA-Z]:/.*")) {
            return true;
        }

        String[] pathParts = normalizedSlash.split("/");
        for (String pathPart : pathParts) {
            if ("..".equals(pathPart)) {
                return true;
            }
        }

        return false;
    }

    public static String normalizeEntryName(String entryName) {
        if (entryName == null) {
            return "";
        }

        String normalizedSlash = entryName.trim().replace('\\', '/');
        String[] pathParts = normalizedSlash.split("/");
        StringBuilder pathBuilder = new StringBuilder();

        for (String pathPart : pathParts) {
            if (pathPart.isBlank() || ".".equals(pathPart)) {
                continue;
            }

            if (pathBuilder.length() > 0) {
                pathBuilder.append('/');
            }
            pathBuilder.append(pathPart);
        }

        return pathBuilder.toString();
    }

    public static String findFilteredDirectory(String normalizedPath) {
        String lowerPath = normalizedPath.toLowerCase(Locale.ROOT);
        String[] pathParts = lowerPath.split("/");

        for (String pathPart : pathParts) {
            if (FILTERED_DIRECTORIES.contains(pathPart)) {
                return pathPart;
            }
        }

        return null;
    }

    public static boolean isBlockedBinaryFile(String normalizedPath) {
        String lowerPath = normalizedPath.toLowerCase(Locale.ROOT);

        for (String extension : BLOCKED_EXTENSIONS) {
            if (lowerPath.endsWith(extension)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isWhiteListFile(String normalizedPath) {
        String fileName = getFileName(normalizedPath);
        String lowerFileName = fileName.toLowerCase(Locale.ROOT);
        String lowerPath = normalizedPath.toLowerCase(Locale.ROOT);

        if ("dockerfile".equals(lowerFileName) || ".gitignore".equals(lowerFileName)) {
            return true;
        }

        for (String extension : WHITE_LIST_EXTENSIONS) {
            if (lowerPath.endsWith(extension)) {
                return true;
            }
        }

        return false;
    }

    public static String detectFileType(String normalizedPath) {
        String fileName = getFileName(normalizedPath);
        String lowerFileName = fileName.toLowerCase(Locale.ROOT);
        String lowerPath = normalizedPath.toLowerCase(Locale.ROOT);

        if ("readme.md".equals(lowerFileName)) {
            return "README";
        }

        if ("pom.xml".equals(lowerFileName)) {
            return "POM";
        }

        if ("application.yml".equals(lowerFileName)
                || "application.yaml".equals(lowerFileName)
                || "application.properties".equals(lowerFileName)) {
            return "CONFIG";
        }

        if ("dockerfile".equals(lowerFileName)) {
            return "DOCKER";
        }

        if ("docker-compose.yml".equals(lowerFileName) || "docker-compose.yaml".equals(lowerFileName)) {
            return "DOCKER_COMPOSE";
        }

        if ("package.json".equals(lowerFileName)) {
            return "PACKAGE";
        }

        if (".gitignore".equals(lowerFileName)) {
            return "GITIGNORE";
        }

        if (lowerFileName.endsWith(".sql")) {
            return "SQL";
        }

        if (lowerFileName.endsWith(".java")) {
            return detectJavaFileType(normalizedPath, lowerPath, lowerFileName);
        }

        return "OTHER";
    }

    private static String detectJavaFileType(String normalizedPath, String lowerPath, String lowerFileName) {
        if (lowerPath.contains("controller")) {
            return "CONTROLLER";
        }

        if (lowerPath.contains("service")) {
            return "SERVICE";
        }

        if (lowerPath.contains("mapper")) {
            return "MAPPER";
        }

        if (lowerPath.contains("entity")
                || lowerPath.contains("model")
                || normalizedPath.contains("PO")
                || lowerFileName.endsWith("po.java")
                || lowerPath.contains("/po/")) {
            return "ENTITY";
        }

        if (lowerPath.contains("config")) {
            return "CONFIG";
        }

        if (lowerPath.contains("util")) {
            return "UTIL";
        }

        return "OTHER";
    }

    private static String getFileName(String normalizedPath) {
        int lastSlashIndex = normalizedPath.lastIndexOf('/');
        if (lastSlashIndex < 0) {
            return normalizedPath;
        }
        return normalizedPath.substring(lastSlashIndex + 1);
    }
}
