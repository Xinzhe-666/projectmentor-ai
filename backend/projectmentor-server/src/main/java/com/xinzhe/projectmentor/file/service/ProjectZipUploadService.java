package com.xinzhe.projectmentor.file.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xinzhe.projectmentor.auth.interceptor.UserContext;
import com.xinzhe.projectmentor.common.BusinessException;
import com.xinzhe.projectmentor.common.ErrorCode;
import com.xinzhe.projectmentor.file.entity.ProjectFile;
import com.xinzhe.projectmentor.file.mapper.ProjectFileMapper;
import com.xinzhe.projectmentor.file.vo.ParsedProjectFileVO;
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

    private static final long MAX_ZIP_SIZE_BYTES = 200L * 1024 * 1024;

    private static final int MAX_VALID_FILE_COUNT = 150;

    private static final int MAX_SINGLE_TEXT_FILE_BYTES = 500 * 1024;

    private static final int MAX_TOTAL_SAVED_TEXT_BYTES = 8 * 1024 * 1024;

    private static final int MAX_TOTAL_ENTRY_COUNT = 3000;

    private static final int MAX_WARNING_COUNT = 30;

    private static final String REASON_FILTERED_DIRECTORY = "FILTERED_DIRECTORY";

    private static final String REASON_NOT_WHITELIST = "NOT_WHITELIST";

    private static final String REASON_BINARY_FILE = "BINARY_FILE";

    private static final String REASON_FILE_TOO_LARGE = "FILE_TOO_LARGE";

    private static final String REASON_VALID_FILE_LIMIT = "VALID_FILE_LIMIT";

    private static final String REASON_TOTAL_TEXT_LIMIT = "TOTAL_TEXT_LIMIT";

    private static final String REASON_DANGEROUS_PATH = "DANGEROUS_PATH";

    private static final String REASON_ENTRY_LIMIT = "ENTRY_LIMIT";

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
                                REASON_ENTRY_LIMIT,
                                "ZIP 内文件数量过多，已停止继续解析"
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
            context.skipFile(REASON_DANGEROUS_PATH, "跳过危险路径：" + entryName);
            return;
        }

        String filePath = ZipFileUtil.normalizeEntryName(entryName);
        if (filePath.isBlank()) {
            context.skipFile(REASON_DANGEROUS_PATH, "跳过空文件路径");
            return;
        }

        String skippedDirectory = ZipFileUtil.findFilteredDirectory(filePath);
        if (skippedDirectory != null) {
            if (context.warnedDirectories.add(skippedDirectory)) {
                context.addWarning("跳过 " + skippedDirectory + " 目录");
            }
            if (!entry.isDirectory()) {
                context.skipFileSilently(REASON_FILTERED_DIRECTORY);
            }
            return;
        }

        if (entry.isDirectory()) {
            return;
        }

        if (ZipFileUtil.isBlockedBinaryFile(filePath)) {
            context.skipFileSilently(REASON_BINARY_FILE);
            return;
        }

        if (!ZipFileUtil.isWhiteListFile(filePath)) {
            context.skipFileSilently(REASON_NOT_WHITELIST);
            return;
        }

        if (context.savedFiles.size() >= MAX_VALID_FILE_COUNT) {
            context.skipFileOnceWarning(
                    REASON_VALID_FILE_LIMIT,
                    "有效文件数量已达到 150 个，后续文件不再保存"
            );
            return;
        }

        if (context.totalTextLimitReached) {
            context.skipFileOnceWarning(
                    REASON_TOTAL_TEXT_LIMIT,
                    "累计保存文本已达到 8MB，后续文件不再保存"
            );
            return;
        }

        if (entry.getSize() > MAX_SINGLE_TEXT_FILE_BYTES) {
            context.skipFile(REASON_FILE_TOO_LARGE, "跳过超过 500KB 的文本文件：" + filePath);
            return;
        }

        ReadFileResult readFileResult = readTextFile(zipInputStream);
        if (readFileResult.tooLarge) {
            context.skipFile(REASON_FILE_TOO_LARGE, "跳过超过 500KB 的文本文件：" + filePath);
            return;
        }

        String content = decodeUtf8(readFileResult.bytes);
        if (content == null) {
            context.skipFile(REASON_BINARY_FILE, "跳过二进制或非 UTF-8 文件：" + filePath);
            return;
        }

        if ((long) context.totalSavedTextBytes + readFileResult.bytes.length > MAX_TOTAL_SAVED_TEXT_BYTES) {
            context.totalTextLimitReached = true;
            context.skipFileOnceWarning(
                    REASON_TOTAL_TEXT_LIMIT,
                    "累计保存文本已达到 8MB，后续文件不再保存"
            );
            return;
        }

        ProjectFile savedFile = upsertProjectFile(projectId, filePath, content);
        context.savedFiles.add(toParsedVO(savedFile));
        context.totalSavedTextBytes += readFileResult.bytes.length;
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
            throw new BusinessException(ErrorCode.PARAM_ERROR, "ZIP 文件超过 200MB，请删除无关依赖目录后重试");
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

    private ReadFileResult readTextFile(ZipInputStream zipInputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int totalSize = 0;
        int length;

        while ((length = zipInputStream.read(buffer)) != -1) {
            totalSize += length;
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

        private final List<String> warnings = new ArrayList<>();

        private final Set<String> warnedDirectories = new HashSet<>();

        private final Set<String> warnedReasons = new HashSet<>();

        private final Map<String, Integer> skippedByReason = initSkippedByReason();

        private int skippedFileCount;

        private int entryCount;

        private int totalSavedTextBytes;

        private boolean totalTextLimitReached;

        private void skipFile(String reason, String warning) {
            skippedFileCount++;
            skippedByReason.merge(reason, 1, Integer::sum);
            addWarning(warning);
        }

        private void skipFileSilently(String reason) {
            skippedFileCount++;
            skippedByReason.merge(reason, 1, Integer::sum);
        }

        private void skipFileOnceWarning(String reason, String warning) {
            skippedFileCount++;
            skippedByReason.merge(reason, 1, Integer::sum);
            if (warnedReasons.add(reason)) {
                addWarning(warning);
            }
        }

        private void addWarning(String warning) {
            if (warnings.size() >= MAX_WARNING_COUNT) {
                return;
            }
            warnings.add(warning);
        }

        private static Map<String, Integer> initSkippedByReason() {
            Map<String, Integer> reasonMap = new LinkedHashMap<>();
            reasonMap.put(REASON_FILTERED_DIRECTORY, 0);
            reasonMap.put(REASON_NOT_WHITELIST, 0);
            reasonMap.put(REASON_BINARY_FILE, 0);
            reasonMap.put(REASON_FILE_TOO_LARGE, 0);
            reasonMap.put(REASON_VALID_FILE_LIMIT, 0);
            reasonMap.put(REASON_TOTAL_TEXT_LIMIT, 0);
            reasonMap.put(REASON_DANGEROUS_PATH, 0);
            reasonMap.put(REASON_ENTRY_LIMIT, 0);
            return reasonMap;
        }
    }

    private static class ReadFileResult {

        private final byte[] bytes;

        private final boolean tooLarge;

        private ReadFileResult(byte[] bytes, boolean tooLarge) {
            this.bytes = bytes;
            this.tooLarge = tooLarge;
        }

        private static ReadFileResult success(byte[] bytes) {
            return new ReadFileResult(bytes, false);
        }

        private static ReadFileResult tooLarge() {
            return new ReadFileResult(new byte[0], true);
        }
    }
}
