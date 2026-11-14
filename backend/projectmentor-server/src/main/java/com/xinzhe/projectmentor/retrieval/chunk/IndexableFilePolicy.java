package com.xinzhe.projectmentor.retrieval.chunk;

import com.xinzhe.projectmentor.file.entity.ProjectFile;
import com.xinzhe.projectmentor.util.ZipFileUtil;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

@Component
public class IndexableFilePolicy {
    private static final int MAX_CONTENT_CHARS = 2 * 1024 * 1024;
    private static final Set<String> SENSITIVE_FILE_MARKERS = Set.of(
            ".env", "id_rsa", "id_ed25519", "credentials", "credential", "secrets", "secret.",
            "password", "passwd", "token."
    );

    public boolean isIndexable(ProjectFile file) {
        if (file == null || file.getFilePath() == null || file.getContent() == null || file.getContent().isBlank()) {
            return false;
        }
        String normalized = ZipFileUtil.normalizeEntryName(file.getFilePath());
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (normalized.isBlank() || ZipFileUtil.isDangerousPath(normalized)
                || ZipFileUtil.findFilteredDirectory(normalized) != null
                || ZipFileUtil.isUnsupportedFileType(normalized)
                || !ZipFileUtil.isWhiteListFile(normalized)
                || file.getContent().length() > MAX_CONTENT_CHARS) {
            return false;
        }
        return SENSITIVE_FILE_MARKERS.stream().noneMatch(lower::contains);
    }
}
