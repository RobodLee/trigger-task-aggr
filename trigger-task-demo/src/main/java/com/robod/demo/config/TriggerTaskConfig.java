package com.robod.demo.config;

import com.robod.triggertask.entity.TriggerTaskBaseEntity;
import com.robod.triggertask.executor.TriggerTaskExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TriggerTaskConfig {

    @Bean
    @SuppressWarnings("unchecked")
    public <T extends TriggerTaskBaseEntity> TriggerTaskExecutor<T> triggerTaskExecutor() {
        return (TriggerTaskExecutor<T>) new TriggerTaskExecutor<>(TriggerTaskBaseEntity.class);
    }

}
