package com.robod.demo.controller;

import com.robod.demo.manager.TriggerTaskManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Slf4j
@RestController
@RequestMapping("/api/v1/demo/")
public class DemoController {

    @Resource
    private TriggerTaskManager triggerTaskManager;

    @GetMapping("/updateStudent")
    @Transactional(rollbackFor = Exception.class)
    public void updateStudent(@RequestParam String studentId, @RequestParam String studentName) {
        triggerTaskManager.updateStudent(studentId, studentName);
    }

}
