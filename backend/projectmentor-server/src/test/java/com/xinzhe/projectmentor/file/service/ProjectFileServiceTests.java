package com.xinzhe.projectmentor.file.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xinzhe.projectmentor.auth.interceptor.UserContext;
import com.xinzhe.projectmentor.file.dto.SaveReadmeRequest;
import com.xinzhe.projectmentor.file.entity.ProjectFile;
import com.xinzhe.projectmentor.file.mapper.ProjectFileMapper;
import com.xinzhe.projectmentor.file.vo.ProjectFileDetailVO;
import com.xinzhe.projectmentor.project.entity.Project;
import com.xinzhe.projectmentor.project.mapper.ProjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectFileServiceTests {

    @AfterEach
    void clearUserContext() {
        UserContext.clear();
    }

    @Test
    void saveReadmeUsesAtomicUpsertForNewAndExistingContent() {
        ProjectFileMapper projectFileMapper = mock(ProjectFileMapper.class);
        ProjectMapper projectMapper = mock(ProjectMapper.class);
        Project project = new Project();
        project.setId(21L);
        project.setUserId(7L);

        when(projectMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(project);
        when(projectFileMapper.selectByProjectIdAndFilePath(21L, "README.md"))
                .thenReturn(file(101L, 21L, "first"), file(101L, 21L, "second"));

        UserContext.setUserId(7L);
        ProjectFileService service = new ProjectFileService(projectFileMapper, projectMapper);

        ProjectFileDetailVO firstResult = service.saveReadme(21L, request("first"));
        ProjectFileDetailVO secondResult = service.saveReadme(21L, request("second"));

        ArgumentCaptor<ProjectFile> upsertCaptor = ArgumentCaptor.forClass(ProjectFile.class);
        verify(projectFileMapper, org.mockito.Mockito.times(2)).upsertProjectFile(upsertCaptor.capture());
        List<ProjectFile> writes = upsertCaptor.getAllValues();

        assertThat(writes).extracting(ProjectFile::getProjectId).containsExactly(21L, 21L);
        assertThat(writes).extracting(ProjectFile::getFilePath).containsOnly("README.md");
        assertThat(writes).extracting(ProjectFile::getFileType).containsOnly("README");
        assertThat(writes).extracting(ProjectFile::getContent).containsExactly("first", "second");
        assertThat(firstResult.getContent()).isEqualTo("first");
        assertThat(secondResult.getContent()).isEqualTo("second");
        verify(projectFileMapper, never()).insert(any(ProjectFile.class));
        verify(projectFileMapper, never()).updateById(any(ProjectFile.class));
    }

    @Test
    void sameReadmePathInDifferentProjectsKeepsProjectIdInTheUniqueKey() {
        ProjectFileMapper projectFileMapper = mock(ProjectFileMapper.class);
        ProjectMapper projectMapper = mock(ProjectMapper.class);
        when(projectMapper.selectOne(any(LambdaQueryWrapper.class))).thenAnswer(invocation -> new Project());
        when(projectFileMapper.selectByProjectIdAndFilePath(21L, "README.md"))
                .thenReturn(file(101L, 21L, "project-one"));
        when(projectFileMapper.selectByProjectIdAndFilePath(22L, "README.md"))
                .thenReturn(file(102L, 22L, "project-two"));

        UserContext.setUserId(7L);
        ProjectFileService service = new ProjectFileService(projectFileMapper, projectMapper);

        service.saveReadme(21L, request("project-one"));
        service.saveReadme(22L, request("project-two"));

        ArgumentCaptor<ProjectFile> upsertCaptor = ArgumentCaptor.forClass(ProjectFile.class);
        verify(projectFileMapper, org.mockito.Mockito.times(2)).upsertProjectFile(upsertCaptor.capture());
        assertThat(upsertCaptor.getAllValues())
                .extracting(ProjectFile::getProjectId, ProjectFile::getFilePath)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(21L, "README.md"),
                        org.assertj.core.groups.Tuple.tuple(22L, "README.md")
                );
    }

    private SaveReadmeRequest request(String content) {
        SaveReadmeRequest request = new SaveReadmeRequest();
        request.setContent(content);
        return request;
    }

    private ProjectFile file(Long id, Long projectId, String content) {
        ProjectFile file = new ProjectFile();
        file.setId(id);
        file.setProjectId(projectId);
        file.setFilePath("README.md");
        file.setFileType("README");
        file.setContent(content);
        return file;
    }
}
