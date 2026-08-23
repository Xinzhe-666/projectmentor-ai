package com.xinzhe.projectmentor.defense.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("pm_defense_question")
public class DefenseQuestion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;

    private String question;

    private String category;

    private String relatedClaims;

    private String relatedEvidence;

    private Integer sortOrder;
}
