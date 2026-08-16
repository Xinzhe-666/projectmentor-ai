package com.xinzhe.projectmentor.file.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xinzhe.projectmentor.auth.interceptor.UserContext;
import com.xinzhe.projectmentor.common.BusinessException;
import com.xinzhe.projectmentor.common.ErrorCode;
import com.xinzhe.projectmentor.file.dto.SaveReadmeRequest;
import com.xinzhe.projectmentor.file.entity.ProjectFile;
import com.xinzhe.projectmentor.file.mapper.ProjectFileMapper;
import com.xinzhe.projectmentor.file.vo.ProjectFileDetailVO;
import com.xinzhe.projectmentor.file.vo.ProjectFileVO;
import com.xinzhe.projectmentor.project.entity.Project;
import com.xinzhe.projectmentor.project.mapper.ProjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectFileService {

    private final ProjectFileMapper projectFileMapper;

    private final ProjectMapper projectMapper;

    @Transactional(rollbackFor = Exception.class)
    public ProjectFileDetailVO saveReadme(Long projectId, SaveReadmeRequest request) {
        checkProjectOwner(projectId);

        ProjectFile readme = new ProjectFile();
        readme.setProjectId(projectId);
        readme.setFilePath("README.md");
        readme.setFileType("README");
        readme.setContent(request.getContent());

        projectFileMapper.upsertProjectFile(readme);
        ProjectFile updatedFile = projectFileMapper.selectByProjectIdAndFilePath(projectId, "README.md");

        return toDetailVO(updatedFile);
    }

    public List<ProjectFileVO> listProjectFiles(Long projectId) {
        checkProjectOwner(projectId);

        List<ProjectFile> files = projectFileMapper.selectList(
                new LambdaQueryWrapper<ProjectFile>()
                        .eq(ProjectFile::getProjectId, projectId)
                        .orderByAsc(ProjectFile::getFilePath)
        );

        return files.stream()
                .map(this::toVO)
                .toList();
    }

    public ProjectFileDetailVO getFileDetail(Long projectId, Long fileId) {
        checkProjectOwner(projectId);

        ProjectFile file = projectFileMapper.selectOne(
                new LambdaQueryWrapper<ProjectFile>()
                        .eq(ProjectFile::getId, fileId)
                        .eq(ProjectFile::getProjectId, projectId)
                        .last("LIMIT 1")
        );

        if (file == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文件不存在或无权限访问");
        }

        return toDetailVO(file);
    }

    public void deleteFile(Long projectId, Long fileId) {
        checkProjectOwner(projectId);

        ProjectFile file = projectFileMapper.selectOne(
                new LambdaQueryWrapper<ProjectFile>()
                        .eq(ProjectFile::getId, fileId)
                        .eq(ProjectFile::getProjectId, projectId)
                        .last("LIMIT 1")
        );

        if (file == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文件不存在或无权限删除");
        }

        projectFileMapper.deleteById(fileId);
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
            throw new BusinessException(ErrorCode.NOT_FOUND, "项目不存在或无权限访问");
        }
    }

    private ProjectFileVO toVO(ProjectFile file) {
        String content = file.getContent();

        return ProjectFileVO.builder()
                .id(file.getId())
                .projectId(file.getProjectId())
                .filePath(file.getFilePath())
                .fileType(file.getFileType())
                .contentLength(content == null ? 0 : content.length())
                .createTime(file.getCreateTime())
                .updateTime(file.getUpdateTime())
                .build();
    }

    private ProjectFileDetailVO toDetailVO(ProjectFile file) {
        return ProjectFileDetailVO.builder()
                .id(file.getId())
                .projectId(file.getProjectId())
                .filePath(file.getFilePath())
                .fileType(file.getFileType())
                .content(file.getContent())
                .createTime(file.getCreateTime())
                .updateTime(file.getUpdateTime())
                .build();
    }
}
