package com.xinzhe.projectmentor.file.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class UploadZipResultVO {

    private Long projectId;

    private Integer savedFileCount;

    private Integer skippedFileCount;

    private List<ParsedProjectFileVO> files;

    private List<SkippedProjectFileVO> skippedFiles;

    private List<String> warnings;

    private Map<String, Integer> skippedByReason;
}
