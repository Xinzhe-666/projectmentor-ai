package com.xinzhe.projectmentor.file.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UploadZipResultVO {

    private Long projectId;

    private Integer savedFileCount;

    private Integer skippedFileCount;

    private List<ParsedProjectFileVO> files;

    private List<String> warnings;
}
