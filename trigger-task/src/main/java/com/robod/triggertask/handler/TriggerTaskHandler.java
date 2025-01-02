package com.robod.triggertask.handler;

import com.robod.triggertask.entity.TriggerTaskBaseEntity;

/**
 * @ClassName TriggerTaskHandler
 * @Description 触发任务处理器接口
 * @Author Robod
 * @Date 2024/12/28 10:00
 */
public interface TriggerTaskHandler<T extends TriggerTaskBaseEntity> {

    void handleTask(T task);

}
