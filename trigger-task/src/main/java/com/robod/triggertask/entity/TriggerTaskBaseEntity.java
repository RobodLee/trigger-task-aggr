package com.robod.triggertask.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @ClassName TriggerTaskBaseEntity
 * @Description 触发任务数据库表实体类
 * @Author Robod
 * @Date 2024/12/28 10:00
 */
@Data
public class TriggerTaskBaseEntity {

    private Long id;

    private String taskType;

    /**
     * {@link TriggerTaskStatusEnum}
     */
    private Integer taskStatus;

    /**
     * 触发时间
     */
    private LocalDateTime triggerTime;

    /**
     * 备注
     */
    private String remark;

    /**
     * MDC
     */
    private String mdc;

    /**
     * 执行失败时的错误信息
     */
    private String failMsg;

    /**
     * 上次失败时间
     */
    private LocalDateTime lastFailTime;

    /**
     * 失败次数
     */
    private Integer failCount;

    /**
     * 分布式锁key
     */
    private String lockKey;

    private String param1;

    private String param2;

    private String param3;

    private String param4;

    private String param5;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

}
