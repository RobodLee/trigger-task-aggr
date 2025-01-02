package com.robod.triggertask.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @ClassName TriggerTaskStatusEnum
 * @Description 触发任务状态枚举
 * @Author Robod
 * @Date 2024/12/28 10:00
 */
@Getter
@AllArgsConstructor
public enum TriggerTaskStatusEnum {

    /**
     * 未结束: 新增或执行异常
     */
    NOT_END(1),

    /**
     * 正常结束
     */
    END(2);

    private final Integer status;

}
