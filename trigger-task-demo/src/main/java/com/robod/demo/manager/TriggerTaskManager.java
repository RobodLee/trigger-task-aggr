package com.robod.demo.manager;

import com.robod.demo.common.TriggerTaskTypeDefine;
import com.robod.triggertask.entity.TriggerTaskBaseEntity;
import com.robod.triggertask.executor.TriggerTaskExecutor;
import com.robod.triggertask.storage.TriggerTaskStorage;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
public class TriggerTaskManager {

    @Resource
    private TriggerTaskStorage<TriggerTaskBaseEntity> triggerTaskStorage;
    @Resource
    private TriggerTaskExecutor<TriggerTaskBaseEntity> triggerTaskExecutor;

    @SneakyThrows
    public void updateStudent(String studentId, String studentName) {
        TriggerTaskBaseEntity entity = new TriggerTaskBaseEntity();
        entity.setTaskType(TriggerTaskTypeDefine.UPDATE_STUDENT);
        entity.setRemark("学生[" + studentId + "]信息更新");
        entity.setLockKey(studentId);
        entity.setParam1(studentId);
        entity.setParam2(studentName);
        triggerTaskStorage.save(entity);

        triggerTaskExecutor.exec(5);
        Thread.sleep(5 * 1000);
    }

}
