package com.robod.triggertask.config;

import com.robod.triggertask.entity.TriggerTaskBaseEntity;
import com.robod.triggertask.storage.TriggerTaskStorage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @ClassName TriggerTaskAutoConfiguration
 * @Description TriggerTask配置类
 * @Author Robod
 * @Date 2024/12/28 10:00
 */
@Configuration
public class TriggerTaskAutoConfiguration {

    @Bean
    public <T extends TriggerTaskBaseEntity> TriggerTaskStorage<T> triggerTaskStorage() {
        return new TriggerTaskStorage<>();
    }

}
