package com.robod.demo.service.trigger;

import com.robod.demo.common.TriggerTaskTypeDefine;
import com.robod.triggertask.entity.TriggerTaskBaseEntity;
import com.robod.triggertask.handler.TriggerTaskHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service(TriggerTaskTypeDefine.UPDATE_STUDENT)
public class UpdateStudentTaskHandler implements TriggerTaskHandler<TriggerTaskBaseEntity> {

    @Override
    public void handleTask(TriggerTaskBaseEntity task) {
        String studentId = task.getParam1();
        String studentName = task.getParam2();

        log.info("更新学生信息,{},{}", studentId, studentName);
        // throw new RuntimeException("更新学生信息异常"); // 模拟执行异常
    }

}
