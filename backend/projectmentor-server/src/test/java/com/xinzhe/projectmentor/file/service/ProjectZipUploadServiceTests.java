package com.xinzhe.projectmentor.file.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xinzhe.projectmentor.auth.interceptor.UserContext;
import com.xinzhe.projectmentor.file.entity.ProjectFile;
import com.xinzhe.projectmentor.file.mapper.ProjectFileMapper;
import com.xinzhe.projectmentor.file.vo.UploadZipResultVO;
import com.xinzhe.projectmentor.project.entity.Project;
import com.xinzhe.projectmentor.project.mapper.ProjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectZipUploadServiceTests {

    @AfterEach
    void clearUserContext() {
        UserContext.clear();
    }

    @Test
    void zipUploadUsesAtomicUpsertAndReturnsPersistedFile() throws Exception {
        ProjectMapper projectMapper = mock(ProjectMapper.class);
        ProjectFileMapper projectFileMapper = mock(ProjectFileMapper.class);
        when(projectMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(new Project());

        ProjectFile persisted = new ProjectFile();
        persisted.setId(301L);
        persisted.setProjectId(31L);
        persisted.setFilePath("README.md");
        persisted.setFileType("README");
        persisted.setContent("new zip content");
        when(projectFileMapper.selectByProjectIdAndFilePath(31L, "README.md")).thenReturn(persisted);

        UserContext.setUserId(7L);
        ProjectZipUploadService service = new ProjectZipUploadService(projectMapper, projectFileMapper);

        UploadZipResultVO result = service.uploadZip(31L, zip("README.md", "new zip content"));

        ArgumentCaptor<ProjectFile> upsertCaptor = ArgumentCaptor.forClass(ProjectFile.class);
        verify(projectFileMapper).upsertProjectFile(upsertCaptor.capture());
        ProjectFile write = upsertCaptor.getValue();
        assertThat(write.getProjectId()).isEqualTo(31L);
        assertThat(write.getFilePath()).isEqualTo("README.md");
        assertThat(write.getFileType()).isEqualTo("README");
        assertThat(write.getContent()).isEqualTo("new zip content");
        assertThat(result.getSavedFileCount()).isEqualTo(1);
        assertThat(result.getFiles()).singleElement().satisfies(file -> {
            assertThat(file.getId()).isEqualTo(301L);
            assertThat(file.getFilePath()).isEqualTo("README.md");
        });
        verify(projectFileMapper, never()).insert(any(ProjectFile.class));
        verify(projectFileMapper, never()).updateById(any(ProjectFile.class));
    }

    private MockMultipartFile zip(String path, String content) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutput = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            zipOutput.putNextEntry(new ZipEntry(path));
            zipOutput.write(content.getBytes(StandardCharsets.UTF_8));
            zipOutput.closeEntry();
        }
        return new MockMultipartFile("file", "project.zip", "application/zip", output.toByteArray());
    }
}
