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
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@RequiredArgsConstructor
public class ProjectZipUploadService {

    private static final long MAX_ZIP_SIZE = 10L * 1024 * 1024;

    private static final int MAX_VALID_FILE_COUNT = 80;

    private static final int MAX_TEXT_FILE_SIZE = 300 * 1024;

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
                parseEntry(projectId, zipInputStream, entry, context);
                zipInputStream.closeEntry();
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
                .build();
    }

    private void parseEntry(Long projectId,
                            ZipInputStream zipInputStream,
                            ZipEntry entry,
                            UploadContext context) throws IOException {
        String entryName = entry.getName();

        if (ZipFileUtil.isDangerousPath(entryName)) {
            context.skipFile("跳过危险路径：" + entryName);
            return;
        }

        String filePath = ZipFileUtil.normalizeEntryName(entryName);
        if (filePath.isBlank()) {
            context.skipFile("跳过空文件路径");
            return;
        }

        String skippedDirectory = ZipFileUtil.findFilteredDirectory(filePath);
        if (skippedDirectory != null) {
            if (context.warnedDirectories.add(skippedDirectory)) {
                context.addWarning("跳过 " + skippedDirectory + " 目录");
            }
            if (!entry.isDirectory()) {
                context.skippedFileCount++;
            }
            return;
        }

        if (entry.isDirectory()) {
            return;
        }

        if (context.savedFiles.size() >= MAX_VALID_FILE_COUNT) {
            context.skipFile("超过最多解析 80 个有效文件限制：" + filePath);
            return;
        }

        if (ZipFileUtil.isBlockedBinaryFile(filePath)) {
            context.skipFile("跳过不支持的文件类型：" + filePath);
            return;
        }

        if (!ZipFileUtil.isWhiteListFile(filePath)) {
            context.skipFile("跳过非白名单文件：" + filePath);
            return;
        }

        if (entry.getSize() > MAX_TEXT_FILE_SIZE) {
            context.skipFile("跳过过大的文件：" + filePath);
            return;
        }

        ReadFileResult readFileResult = readTextFile(zipInputStream);
        if (readFileResult.tooLarge) {
            context.skipFile("跳过过大的文件：" + filePath);
            return;
        }

        String content = decodeUtf8(readFileResult.bytes);
        if (content == null) {
            context.skipFile("跳过二进制或非 UTF-8 文件：" + filePath);
            return;
        }

        ProjectFile savedFile = upsertProjectFile(projectId, filePath, content);
        context.savedFiles.add(toParsedVO(savedFile));
    }

    private void checkZipFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请上传 ZIP 文件");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".zip")) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "只支持上传 .zip 文件");
        }

        if (file.getSize() > MAX_ZIP_SIZE) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "ZIP 文件不能超过 10MB");
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
            if (totalSize > MAX_TEXT_FILE_SIZE) {
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

        private int skippedFileCount;

        private void skipFile(String warning) {
            skippedFileCount++;
            addWarning(warning);
        }

        private void addWarning(String warning) {
            warnings.add(warning);
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
