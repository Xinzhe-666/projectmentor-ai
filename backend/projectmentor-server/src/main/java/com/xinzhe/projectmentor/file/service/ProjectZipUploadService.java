package com.xinzhe.projectmentor.file.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xinzhe.projectmentor.auth.interceptor.UserContext;
import com.xinzhe.projectmentor.common.BusinessException;
import com.xinzhe.projectmentor.common.ErrorCode;
import com.xinzhe.projectmentor.file.entity.ProjectFile;
import com.xinzhe.projectmentor.file.mapper.ProjectFileMapper;
import com.xinzhe.projectmentor.file.vo.ParsedProjectFileVO;
import com.xinzhe.projectmentor.file.vo.SkippedProjectFileVO;
import com.xinzhe.projectmentor.file.vo.UploadZipResultVO;
import com.xinzhe.projectmentor.project.entity.Project;
import com.xinzhe.projectmentor.project.mapper.ProjectMapper;
import com.xinzhe.projectmentor.util.ZipFileUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@RequiredArgsConstructor
public class ProjectZipUploadService {

    private static final long MAX_ZIP_SIZE_BYTES = 800L * 1024 * 1024;

    private static final int MAX_VALID_FILE_COUNT = 8000;

    private static final int MAX_SINGLE_TEXT_FILE_BYTES = 2 * 1024 * 1024;

    private static final long MAX_TOTAL_PROCESSED_TEXT_BYTES = 1024L * 1024 * 1024;

    private static final int MAX_TOTAL_ENTRY_COUNT = 50000;

    private static final int MAX_WARNING_COUNT = 30;

    private static final String REASON_IGNORED_DIRECTORY = "ignored_directory";

    private static final String REASON_UNSUPPORTED_TYPE = "unsupported_type";

    private static final String REASON_FILE_TOO_LARGE = "file_too_large";

    private static final String REASON_UNSAFE_PATH = "unsafe_path";

    private static final String REASON_MAX_FILE_COUNT_EXCEEDED = "max_file_count_exceeded";

    private static final String REASON_MAX_TOTAL_SIZE_EXCEEDED = "max_total_size_exceeded";

    private static final String REASON_EMPTY_FILE = "empty_file";

    private static final String REASON_BINARY_FILE = "binary_file";

    private final ProjectMapper projectMapper;

    private final ProjectFileMapper projectFileMapper;

    @Transactional(rollbackFor = Exception.class)
    public UploadZipResultVO uploadZip(Long projectId, MultipartFile file) {
        checkProjectOwner(projectId);
        checkZipFile(file);

        UploadContext context = new UploadContext();

        try (InputStream inputStream = file.getInputStream();
             ZipInputStream zipInputStream = new ZipInputStream(inputStream, StandardCharsets.UTF_8)) {

            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                try {
                    context.entryCount++;
                    if (context.entryCount > MAX_TOTAL_ENTRY_COUNT) {
                        context.skipFileOnceWarning(
                                REASON_MAX_FILE_COUNT_EXCEEDED,
                                entry.getName(),
                                "ZIP 内文件数量超过安全上限，已停止继续解析"
                        );
                        break;
                    }

                    parseEntry(projectId, zipInputStream, entry, context);
                } finally {
                    zipInputStream.closeEntry();
                }
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "ZIP 文件解析失败，请确认文件格式正确");
        }

        return UploadZipResultVO.builder()
                .projectId(projectId)
                .savedFileCount(context.savedFiles.size())
                .skippedFileCount(context.skippedFileCount)
                .files(context.savedFiles)
                .skippedFiles(context.skippedFiles)
                .warnings(context.warnings)
                .skippedByReason(context.skippedByReason)
                .build();
    }

    private void parseEntry(Long projectId,
                            ZipInputStream zipInputStream,
                            ZipEntry entry,
                            UploadContext context) throws IOException {
        String entryName = entry.getName();

        if (ZipFileUtil.isDangerousPath(entryName)) {
            context.skipFile(REASON_UNSAFE_PATH, entryName, "跳过不安全路径：" + entryName);
            return;
        }

        String filePath = ZipFileUtil.normalizeEntryName(entryName);
        if (filePath.isBlank()) {
            context.skipFile(REASON_UNSAFE_PATH, entryName, "跳过空文件路径");
            return;
        }

        String skippedDirectory = ZipFileUtil.findFilteredDirectory(filePath);
        if (skippedDirectory != null) {
            if (context.warnedDirectories.add(skippedDirectory)) {
                context.addWarning("跳过 " + skippedDirectory + " 目录");
            }
            if (!entry.isDirectory()) {
                context.skipFileSilently(REASON_IGNORED_DIRECTORY, filePath);
            }
            return;
        }

        if (entry.isDirectory()) {
            return;
        }

        if (ZipFileUtil.isUnsupportedFileType(filePath)) {
            context.skipFileSilently(REASON_UNSUPPORTED_TYPE, filePath);
            return;
        }

        if (!ZipFileUtil.isWhiteListFile(filePath)) {
            context.skipFileSilently(REASON_UNSUPPORTED_TYPE, filePath);
            return;
        }

        if (context.savedFiles.size() >= MAX_VALID_FILE_COUNT) {
            context.skipFileOnceWarning(
                    REASON_MAX_FILE_COUNT_EXCEEDED,
                    filePath,
                    "有效文件数量已达到 8000 个，后续文件不再保存"
            );
            return;
        }

        if (context.maxTotalSizeReached) {
            context.skipFileOnceWarning(
                    REASON_MAX_TOTAL_SIZE_EXCEEDED,
                    filePath,
                    "累计有效处理大小已达到 1GB，后续文件不再保存"
            );
            return;
        }

        long entrySize = entry.getSize();
        if (entrySize == 0) {
            context.skipFileSilently(REASON_EMPTY_FILE, filePath);
            return;
        }

        if (entry.getSize() > MAX_SINGLE_TEXT_FILE_BYTES) {
            context.skipFile(REASON_FILE_TOO_LARGE, filePath, "文件超过 2MB，已跳过内容解析：" + filePath);
            return;
        }

        if (entrySize > 0 && context.totalProcessedTextBytes + entrySize > MAX_TOTAL_PROCESSED_TEXT_BYTES) {
            context.maxTotalSizeReached = true;
            context.skipFileOnceWarning(
                    REASON_MAX_TOTAL_SIZE_EXCEEDED,
                    filePath,
                    "累计有效处理大小已达到 1GB，后续文件不再保存"
            );
            return;
        }

        long remainingTotalBytes = MAX_TOTAL_PROCESSED_TEXT_BYTES - context.totalProcessedTextBytes;
        ReadFileResult readFileResult = readTextFile(zipInputStream, remainingTotalBytes);
        if (readFileResult.totalSizeExceeded) {
            context.maxTotalSizeReached = true;
            context.skipFileOnceWarning(
                    REASON_MAX_TOTAL_SIZE_EXCEEDED,
                    filePath,
                    "累计有效处理大小已达到 1GB，后续文件不再保存"
            );
            return;
        }

        if (readFileResult.tooLarge) {
            context.skipFile(REASON_FILE_TOO_LARGE, filePath, "文件超过 2MB，已跳过内容解析：" + filePath);
            return;
        }

        if (readFileResult.bytes.length == 0) {
            context.skipFileSilently(REASON_EMPTY_FILE, filePath);
            return;
        }

        String content = decodeUtf8(readFileResult.bytes);
        if (content == null) {
            context.skipFile(REASON_BINARY_FILE, filePath, "跳过二进制或非 UTF-8 文件：" + filePath);
            return;
        }

        ProjectFile savedFile = upsertProjectFile(projectId, filePath, content);
        context.savedFiles.add(toParsedVO(savedFile));
        context.totalProcessedTextBytes += readFileResult.bytes.length;
    }

    private void checkZipFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请上传 ZIP 文件");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".zip")) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "只支持上传 .zip 文件");
        }

        if (file.getSize() > MAX_ZIP_SIZE_BYTES) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "ZIP 文件超过 800MB，请先删除 node_modules / target / dist / .git 等无关目录后重试");
        }
    }

    private void checkProjectOwner(Long projectId) {
        Long userId = UserContext.getUserId();

        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Project project = projectMapper.selectOne(
                new LambdaQueryWrapper<Project>()
                        .eq(Project::getId, projectId)
                        .eq(Project::getUserId, userId)
                        .last("LIMIT 1")
        );

        if (project == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "项目不存在或无权访问");
        }
    }

    private ProjectFile upsertProjectFile(Long projectId, String filePath, String content) {
        ProjectFile existingFile = projectFileMapper.selectOne(
                new LambdaQueryWrapper<ProjectFile>()
                        .eq(ProjectFile::getProjectId, projectId)
                        .eq(ProjectFile::getFilePath, filePath)
                        .last("LIMIT 1")
        );

        String fileType = ZipFileUtil.detectFileType(filePath);

        if (existingFile == null) {
            ProjectFile projectFile = new ProjectFile();
            projectFile.setProjectId(projectId);
            projectFile.setFilePath(filePath);
            projectFile.setFileType(fileType);
            projectFile.setContent(content);

            projectFileMapper.insert(projectFile);
            return projectFile;
        }

        existingFile.setFileType(fileType);
        existingFile.setContent(content);
        projectFileMapper.updateById(existingFile);

        return projectFileMapper.selectById(existingFile.getId());
    }

    private ReadFileResult readTextFile(ZipInputStream zipInputStream, long remainingTotalBytes) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        long totalSize = 0;
        int length;

        while ((length = zipInputStream.read(buffer)) != -1) {
            totalSize += length;
            if (totalSize > remainingTotalBytes) {
                return ReadFileResult.totalSizeExceeded();
            }
            if (totalSize > MAX_SINGLE_TEXT_FILE_BYTES) {
                return ReadFileResult.tooLarge();
            }
            outputStream.write(buffer, 0, length);
        }

        return ReadFileResult.success(outputStream.toByteArray());
    }

    private String decodeUtf8(byte[] bytes) {
        if (containsZeroByte(bytes)) {
            return null;
        }

        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException e) {
            return null;
        }
    }

    private boolean containsZeroByte(byte[] bytes) {
        for (byte oneByte : bytes) {
            if (oneByte == 0) {
                return true;
            }
        }
        return false;
    }

    private ParsedProjectFileVO toParsedVO(ProjectFile file) {
        String content = file.getContent();

        return ParsedProjectFileVO.builder()
                .id(file.getId())
                .filePath(file.getFilePath())
                .fileType(file.getFileType())
                .contentLength(content == null ? 0 : content.length())
                .build();
    }

    private static class UploadContext {

        private final List<ParsedProjectFileVO> savedFiles = new ArrayList<>();

        private final List<SkippedProjectFileVO> skippedFiles = new ArrayList<>();

        private final List<String> warnings = new ArrayList<>();

        private final Set<String> warnedDirectories = new HashSet<>();

        private final Set<String> warnedReasons = new HashSet<>();

        private final Map<String, Integer> skippedByReason = initSkippedByReason();

        private int skippedFileCount;

        private int entryCount;

        private long totalProcessedTextBytes;

        private boolean maxTotalSizeReached;

        private void skipFile(String reason, String filePath, String warning) {
            skippedFileCount++;
            skippedByReason.merge(reason, 1, Integer::sum);
            addSkippedFile(filePath, reason);
            addWarning(warning);
        }

        private void skipFileSilently(String reason, String filePath) {
            skippedFileCount++;
            skippedByReason.merge(reason, 1, Integer::sum);
            addSkippedFile(filePath, reason);
        }

        private void skipFileOnceWarning(String reason, String filePath, String warning) {
            skippedFileCount++;
            skippedByReason.merge(reason, 1, Integer::sum);
            addSkippedFile(filePath, reason);
            if (warnedReasons.add(reason)) {
                addWarning(warning);
            }
        }

        private void addSkippedFile(String filePath, String reason) {
            skippedFiles.add(SkippedProjectFileVO.builder()
                    .filePath(filePath == null || filePath.isBlank() ? "-" : filePath)
                    .reason(reason)
                    .build());
        }

        private void addWarning(String warning) {
            if (warnings.size() >= MAX_WARNING_COUNT) {
                return;
            }
            warnings.add(warning);
        }

        private static Map<String, Integer> initSkippedByReason() {
            Map<String, Integer> reasonMap = new LinkedHashMap<>();
            reasonMap.put(REASON_IGNORED_DIRECTORY, 0);
            reasonMap.put(REASON_UNSUPPORTED_TYPE, 0);
            reasonMap.put(REASON_FILE_TOO_LARGE, 0);
            reasonMap.put(REASON_UNSAFE_PATH, 0);
            reasonMap.put(REASON_MAX_FILE_COUNT_EXCEEDED, 0);
            reasonMap.put(REASON_MAX_TOTAL_SIZE_EXCEEDED, 0);
            reasonMap.put(REASON_EMPTY_FILE, 0);
            reasonMap.put(REASON_BINARY_FILE, 0);
            return reasonMap;
        }
    }

    private static class ReadFileResult {

        private final byte[] bytes;

        private final boolean tooLarge;

        private final boolean totalSizeExceeded;

        private ReadFileResult(byte[] bytes, boolean tooLarge, boolean totalSizeExceeded) {
            this.bytes = bytes;
            this.tooLarge = tooLarge;
            this.totalSizeExceeded = totalSizeExceeded;
        }

        private static ReadFileResult success(byte[] bytes) {
            return new ReadFileResult(bytes, false, false);
        }

        private static ReadFileResult tooLarge() {
            return new ReadFileResult(new byte[0], true, false);
        }

        private static ReadFileResult totalSizeExceeded() {
            return new ReadFileResult(new byte[0], false, true);
        }
    }
}
