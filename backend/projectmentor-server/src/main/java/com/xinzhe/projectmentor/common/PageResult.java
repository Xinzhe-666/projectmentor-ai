package com.xinzhe.projectmentor.common;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PageResult<T> {

    private List<T> records;

    private Long total;

    private Integer page;

    private Integer size;
}
