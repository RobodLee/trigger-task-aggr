package com.robod.triggertask.executor;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.robod.triggertask.entity.TriggerTaskBaseEntity;
import com.robod.triggertask.entity.TriggerTaskStatusEnum;
import com.robod.triggertask.handler.TriggerTaskHandler;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.MDC;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.concurrent.Executors.newFixedThreadPool;

/**
 * @ClassName TriggerTaskExecutor
 * @Description 触发任务执行器
 * @Author Robod
 * @Date 2024/12/28 10:00
 */
@Slf4j
public class TriggerTaskExecutor<T extends TriggerTaskBaseEntity> implements EnvironmentAware {

    private Environment environment;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private Map<String, TriggerTaskHandler<T>> triggerTaskHandlerMap;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private TransactionTemplate transactionTemplate;

    private static final ExecutorService executor = newFixedThreadPool(10);
    private static final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

    private final Class<T> clazz;

    public TriggerTaskExecutor(Class<T> clazz) {
        this.clazz = clazz;
    }

    /**
     * n秒后立即扫描一次任务表执行
     *
     * @param second 秒
     */
    public void exec(int second) {
        scheduledExecutorService.schedule(this::timeExec, second, TimeUnit.SECONDS);
    }

    @Scheduled(cron = "0 0/10 * * * ?")
    public void timeExec() {
        String lockKey = environment.getProperty("spring.application.name") + ":trigger-task:timeExec";

        RLock lock = redissonClient.getLock(lockKey);
        try {
            // 尝试获取锁，获取成功才执行。保证一次只有一台实例能执行
            boolean b = lock.tryLock();
            if (b) {
                exec();
            }
        } catch (Exception e) {
            log.error("timeExec异常", e);
        } finally {
            lock.unlock();
        }
    }

    private void exec() {
        int failCountLimit = getFailCountLimit();

        Long idLimit = 0L;
        while (true) {
            String sql = String.format(" select * from trigger_task where id > %d and task_status = %d and trigger_time <= '%s' %s order by id asc limit 200 ",
                    idLimit, TriggerTaskStatusEnum.NOT_END.getStatus(), LocalDateTime.now(), failCountLimit < 3 ? "and fail_count <= " + failCountLimit : "");
            log.info(sql);
            List<T> taskList = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(clazz));

            log.info("task.size: {}", taskList.size());

            if (CollectionUtils.isEmpty(taskList)) {
                break;
            }

            idLimit = taskList.get(taskList.size() - 1).getId();
            for (T task : taskList) {
                executor.execute(() -> {
                    // 设置MDC信息
                    if (task.getMdc() != null && !task.getMdc().isEmpty()) {
                        MDC.setContextMap(JSON.parseObject(task.getMdc(), new TypeReference<Map<String, String>>() {
                        }.getType()));
                    }

                    TriggerTaskHandler<T> triggerTaskHandler = triggerTaskHandlerMap.get(task.getTaskType());
                    if (triggerTaskHandler == null) {
                        throw new RuntimeException("未找到对应的TriggerTaskHandler," + task.getTaskType());
                    }

                    log.info("开始执行TriggerTask: {}", JSON.toJSONString(task));
                    handleTask(triggerTaskHandler, task);
                    log.info("TriggerTask执行结束: {}", JSON.toJSONString(task));
                });
            }
        }

        cacheFailCountLimit(failCountLimit);
    }

    /**
     * 失败次数限制。failCount等于0的，每次exec都会执行。failCount等于1的，隔一次执行一次。failCount等于2的，隔2次执行一次。failCount大于2的，隔3次执行一次。
     */
    private Integer getFailCountLimit() {
        Integer failCountLimit = redissonClient.<Integer>getBucket(getFailCountLimitKey()).get();
        return failCountLimit != null ? failCountLimit : 0;
    }

    private String getFailCountLimitKey() {
        String applicationName = environment.getProperty("spring.application.name");
        return applicationName + ":trigger-task:failCountLimit";
    }

    private void cacheFailCountLimit(int failCountLimit) {
        failCountLimit = ++failCountLimit % 4;  // 限制failCountLimit最大是3
        redissonClient.<Integer>getBucket(getFailCountLimitKey()).set(failCountLimit, Duration.ofHours(1));
    }

    @SuppressWarnings("all")
    private void handleTask(TriggerTaskHandler triggerTaskHandler, T task) {
        String lockKey = environment.getProperty("spring.application.name") + ":" + task.getLockKey();
        RLock lock = null;

        AtomicBoolean isExecuteFail = new AtomicBoolean(false);
        try {
            // 加分布式锁
            if (task.getLockKey() != null && !task.getLockKey().isEmpty()) {
                lock = redissonClient.getLock(lockKey);
                lock.lock();
            }
            transactionTemplate.execute(status -> {
                try {
                    // 处理任务
                    triggerTaskHandler.handleTask(task);

                    // 更新状态
                    task.setTaskStatus(TriggerTaskStatusEnum.END.getStatus());
                    task.setUpdatedAt(LocalDateTime.now());

                    String sql = String.format("update trigger_task set task_status = %d, updated_at = '%s'  where id = %d",
                            TriggerTaskStatusEnum.END.getStatus(), LocalDateTime.now(), task.getId());
                    log.info(sql);
                    jdbcTemplate.update(sql);
                    log.info("触发任务执行成功{},{}", task.getId(), task.getTaskType());
                } catch (Exception e) {
                    // 记录失败信息
                    isExecuteFail.set(true);
                    task.setTaskStatus(TriggerTaskStatusEnum.NOT_END.getStatus());
                    task.setFailMsg(e.getMessage());
                    task.setLastFailTime(LocalDateTime.now());
                    task.setFailCount(task.getFailCount() + 1);
                    task.setUpdatedAt(LocalDateTime.now());

                    log.error("触发任务执行异常{}", task.getId(), e);
                    status.setRollbackOnly();
                }
                return true;
            });
        } catch (Exception e) {
            log.error("分布式锁异常", e);
        } finally {
            if (isExecuteFail.get()) {
                String sql = String.format("update trigger_task set task_status = %d, fail_msg = '%s', last_fail_time = '%s', fail_count =  %d, updated_at = '%s' where id = %d",
                        TriggerTaskStatusEnum.NOT_END.getStatus(), task.getFailMsg(), task.getLastFailTime(), task.getFailCount(), task.getUpdatedAt(), task.getId());
                log.info(sql);
                jdbcTemplate.update(sql);
            }

            // 释放锁
            if (task.getLockKey() != null && !task.getLockKey().isEmpty()) {
                lock.unlock();
            }
        }
    }

    @Scheduled(cron = "0 0 1 * * ? ")
    public void timeClear() {
        String lockKey = environment.getProperty("spring.application.name") + ":" + "trigger-task:timeClear";
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 尝试获取锁，获取成功才执行。保证一次只有一台实例能执行
            boolean b = lock.tryLock();
            if (b) {
                String sql = String.format("delete from trigger_task where task_status = %d and updated_at <= '%s'",
                        TriggerTaskStatusEnum.END.getStatus(), LocalDateTime.now().minusMonths(1));
                log.info(sql);
                jdbcTemplate.update(sql);
            }
        } catch (Exception e) {
            log.error("timeExec异常", e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    @SuppressWarnings("all")
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

}
