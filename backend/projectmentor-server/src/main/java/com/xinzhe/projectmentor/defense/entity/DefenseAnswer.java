package com.xinzhe.projectmentor.defense.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pm_defense_answer")
public class DefenseAnswer {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long questionId;

    private String answerText;

    private String evaluationStatus;

    private String reviewResult;

    private LocalDateTime createdAt;
}
