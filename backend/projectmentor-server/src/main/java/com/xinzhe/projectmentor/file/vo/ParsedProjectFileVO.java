package com.xinzhe.projectmentor.file.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ParsedProjectFileVO {

    private Long id;

    private String filePath;

    private String fileType;

    private Integer contentLength;
}
