package com.yinfeng.interview.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("request_result")
public class RequestResult {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private String workerId;

    private String method;

    private String url;

    private Integer statusCode;

    private Long latencyMs;

    private Boolean success;

    private String errorMessage;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
