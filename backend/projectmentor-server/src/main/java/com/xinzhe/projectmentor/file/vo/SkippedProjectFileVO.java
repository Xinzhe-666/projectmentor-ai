package com.xinzhe.projectmentor.file.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SkippedProjectFileVO {

    private String filePath;

    private String reason;
}
