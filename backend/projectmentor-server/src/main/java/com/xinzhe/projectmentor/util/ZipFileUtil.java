package com.xinzhe.projectmentor.util;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ZipFileUtil {

    private static final Set<String> FILTERED_DIRECTORIES = Set.of(
            ".git",
            "node_modules",
            "target",
            "dist",
            "build",
            "out",
            ".idea",
            ".vscode",
            "logs",
            "coverage",
            ".next",
            ".nuxt",
            "vendor",
            "__pycache__",
            ".cache",
            ".gradle",
            "uploads",
            "tmp",
            "temp"
    );

    private static final Set<String> FILTERED_PATH_PREFIXES = Set.of(
            ".mvn/wrapper"
    );

    private static final List<String> UNSUPPORTED_EXTENSIONS = List.of(
            ".png",
            ".jpg",
            ".jpeg",
            ".gif",
            ".webp",
            ".ico",
            ".svg",
            ".mp4",
            ".mov",
            ".avi",
            ".mp3",
            ".wav",
            ".zip",
            ".rar",
            ".7z",
            ".tar",
            ".gz",
            ".tgz",
            ".bz2",
            ".xz",
            ".exe",
            ".dll",
            ".so",
            ".dylib",
            ".class",
            ".jar",
            ".war",
            ".onnx",
            ".pt",
            ".pth",
            ".bin",
            ".dat",
            ".sqlite",
            ".db",
            ".pdf",
            ".doc",
            ".docx",
            ".xls",
            ".xlsx",
            ".ppt",
            ".pptx"
    );

    private static final List<String> WHITE_LIST_EXTENSIONS = List.of(
            ".md",
            ".txt",
            ".xml",
            ".yml",
            ".yaml",
            ".properties",
            ".ini",
            ".conf",
            ".toml",
            ".java",
            ".kt",
            ".kts",
            ".gradle",
            ".js",
            ".jsx",
            ".ts",
            ".tsx",
            ".vue",
            ".css",
            ".scss",
            ".less",
            ".html",
            ".htm",
            ".py",
            ".go",
            ".rs",
            ".c",
            ".cpp",
            ".h",
            ".hpp",
            ".cs",
            ".sh",
            ".bat",
            ".cmd",
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
        for (String filteredPrefix : FILTERED_PATH_PREFIXES) {
            if (lowerPath.equals(filteredPrefix)
                    || lowerPath.startsWith(filteredPrefix + "/")
                    || lowerPath.endsWith("/" + filteredPrefix)
                    || lowerPath.contains("/" + filteredPrefix + "/")) {
                return filteredPrefix;
            }
        }

        String[] pathParts = lowerPath.split("/");

        for (String pathPart : pathParts) {
            if (FILTERED_DIRECTORIES.contains(pathPart)) {
                return pathPart;
            }
        }

        return null;
    }

    public static boolean isUnsupportedFileType(String normalizedPath) {
        String lowerPath = normalizedPath.toLowerCase(Locale.ROOT);

        for (String extension : UNSUPPORTED_EXTENSIONS) {
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

        if ("dockerfile".equals(lowerFileName)
                || "makefile".equals(lowerFileName)
                || "mvnw".equals(lowerFileName)
                || "mvnw.cmd".equals(lowerFileName)
                || "gradlew".equals(lowerFileName)
                || "gradlew.bat".equals(lowerFileName)
                || "yarn.lock".equals(lowerFileName)
                || ".gitignore".equals(lowerFileName)
                || ".dockerignore".equals(lowerFileName)
                || ".env.example".equals(lowerFileName)
                || ".env.sample".equals(lowerFileName)) {
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

        if ("package.json".equals(lowerFileName)
                || "package-lock.json".equals(lowerFileName)
                || "yarn.lock".equals(lowerFileName)
                || "pnpm-lock.yaml".equals(lowerFileName)) {
            return "PACKAGE";
        }

        if (".gitignore".equals(lowerFileName) || ".dockerignore".equals(lowerFileName)) {
            return "GITIGNORE";
        }

        if (lowerFileName.endsWith(".sql")) {
            return "SQL";
        }

        if (lowerFileName.endsWith(".java")) {
            return detectJavaFileType(normalizedPath, lowerPath, lowerFileName);
        }

        if (isCodeFile(lowerFileName)) {
            return "CODE";
        }

        return "OTHER";
    }

    private static boolean isCodeFile(String lowerFileName) {
        return lowerFileName.endsWith(".js")
                || lowerFileName.endsWith(".jsx")
                || lowerFileName.endsWith(".ts")
                || lowerFileName.endsWith(".tsx")
                || lowerFileName.endsWith(".vue")
                || lowerFileName.endsWith(".css")
                || lowerFileName.endsWith(".scss")
                || lowerFileName.endsWith(".less")
                || lowerFileName.endsWith(".html")
                || lowerFileName.endsWith(".py")
                || lowerFileName.endsWith(".go")
                || lowerFileName.endsWith(".rs")
                || lowerFileName.endsWith(".c")
                || lowerFileName.endsWith(".cpp")
                || lowerFileName.endsWith(".h")
                || lowerFileName.endsWith(".hpp")
                || lowerFileName.endsWith(".cs")
                || lowerFileName.endsWith(".sh")
                || lowerFileName.endsWith(".bat")
                || lowerFileName.endsWith(".cmd");
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
