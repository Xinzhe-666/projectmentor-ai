package com.xinzhe.projectmentor.project.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xinzhe.projectmentor.auth.interceptor.UserContext;
import com.xinzhe.projectmentor.common.BusinessException;
import com.xinzhe.projectmentor.common.ErrorCode;
import com.xinzhe.projectmentor.project.dto.CreateProjectRequest;
import com.xinzhe.projectmentor.project.entity.Project;
import com.xinzhe.projectmentor.project.mapper.ProjectMapper;
import com.xinzhe.projectmentor.project.vo.ProjectVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectMapper projectMapper;

    public ProjectVO createProject(CreateProjectRequest request) {
        Long userId = getCurrentUserId();

        Project project = new Project();
        project.setUserId(userId);
        project.setName(request.getName());
        project.setGithubUrl(request.getGithubUrl());
        project.setDescription(request.getDescription());
        project.setProjectType(request.getProjectType());
        project.setTechStack(request.getTechStack());
        project.setStatus("PENDING");

        projectMapper.insert(project);

        return toVO(project);
    }

    public List<ProjectVO> listMyProjects() {
        Long userId = getCurrentUserId();

        List<Project> projects = projectMapper.selectList(
                new LambdaQueryWrapper<Project>()
                        .eq(Project::getUserId, userId)
                        .orderByDesc(Project::getCreateTime)
        );

        return projects.stream()
                .map(this::toVO)
                .toList();
    }

    public ProjectVO getProjectDetail(Long projectId) {
        Long userId = getCurrentUserId();

        Project project = projectMapper.selectOne(
                new LambdaQueryWrapper<Project>()
                        .eq(Project::getId, projectId)
                        .eq(Project::getUserId, userId)
                        .last("LIMIT 1")
        );

        if (project == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "项目不存在或无权限访问");
        }

        return toVO(project);
    }

    public void deleteProject(Long projectId) {
        Long userId = getCurrentUserId();

        Project project = projectMapper.selectOne(
                new LambdaQueryWrapper<Project>()
                        .eq(Project::getId, projectId)
                        .eq(Project::getUserId, userId)
                        .last("LIMIT 1")
        );

        if (project == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "项目不存在或无权限删除");
        }

        projectMapper.deleteById(projectId);
    }

    private Long getCurrentUserId() {
        Long userId = UserContext.getUserId();

        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return userId;
    }

    private ProjectVO toVO(Project project) {
        return ProjectVO.builder()
                .id(project.getId())
                .name(project.getName())
                .githubUrl(project.getGithubUrl())
                .description(project.getDescription())
                .projectType(project.getProjectType())
                .techStack(project.getTechStack())
                .status(project.getStatus())
                .createTime(project.getCreateTime())
                .updateTime(project.getUpdateTime())
                .build();
    }
}