package com.xinzhe.projectmentor.dashboard.vo;

import com.xinzhe.projectmentor.analysis.vo.AnalysisReportListItemVO;
import com.xinzhe.projectmentor.interview.vo.InterviewSessionListItemVO;
import com.xinzhe.projectmentor.project.vo.ProjectVO;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DashboardSummaryVO {

    private Long projectCount;

    private Integer creditBalance;

    private Long reportCount;

    private Long interviewSessionCount;

    private List<ProjectVO> recentProjects;

    private List<AnalysisReportListItemVO> recentReports;

    private List<InterviewSessionListItemVO> recentInterviews;
}
