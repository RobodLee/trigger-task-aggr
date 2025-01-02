package com.robod.triggertask.storage;

import com.alibaba.fastjson2.JSON;
import com.robod.triggertask.entity.TriggerTaskBaseEntity;
import com.robod.triggertask.entity.TriggerTaskStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.LocalDateTime;
import java.util.*;

/**
 * @ClassName TriggerTaskStorage
 * @Description 触发任务存储器
 * @Author Robod
 * @Date 2024/12/28 10:00
 */
@Slf4j
public class TriggerTaskStorage<T extends TriggerTaskBaseEntity> {

    @Resource
    private JdbcTemplate jdbcTemplate;

    public void save(T entity) {
        init(entity);
        String sql = generateInsertSQL(Collections.singletonList(entity));
        log.info(sql);
        jdbcTemplate.update(sql);
    }

    public void saveBatch(List<T> entityList) {
        if (CollectionUtils.isEmpty(entityList)) {
            return;
        }

        entityList.forEach(this::init);
        split(entityList).forEach(list -> {
            String sql = generateInsertSQL(entityList);
            log.info(sql);
            jdbcTemplate.update(sql);
        });
    }

    private List<List<T>> split(Collection<T> collection) {
        // 该方法摘抄自hutool
        final List<List<T>> result = new ArrayList<>();
        if (CollectionUtils.isEmpty(collection)) {
            return result;
        }

        final int defaultSize = 500;
        final int initSize = Math.min(collection.size(), defaultSize);
        List<T> subList = new ArrayList<>(initSize);
        for (T t : collection) {
            if (subList.size() >= defaultSize) {
                result.add(subList);
                subList = new ArrayList<>(initSize);
            }
            subList.add(t);
        }
        result.add(subList);
        return result;
    }

    private void init(TriggerTaskBaseEntity entity) {
        entity.setTaskStatus(TriggerTaskStatusEnum.NOT_END.getStatus());
        entity.setTriggerTime(entity.getTriggerTime() != null ? entity.getTriggerTime() : LocalDateTime.now());
        entity.setRemark(entity.getRemark() != null ? entity.getRemark() : "");
        entity.setFailMsg("");
        entity.setFailCount(0);
        entity.setLockKey(entity.getLockKey() != null ? entity.getLockKey() : "");
        entity.setCreatedAt(entity.getCreatedAt() != null ? entity.getCreatedAt() : LocalDateTime.now());
        entity.setUpdatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt() : LocalDateTime.now());
        Map<String, String> copyOfContextMap = MDC.getCopyOfContextMap();
        if (copyOfContextMap != null) {
            copyOfContextMap.remove("requestParam");
            copyOfContextMap.remove("token");
            copyOfContextMap.remove("MachineName");
            copyOfContextMap.remove("ApplicationName");
            entity.setMdc(JSON.toJSONString(copyOfContextMap));
        } else {
            entity.setMdc("");
        }
    }

    /**
     * 实体类转成插入语句
     */
    private String generateInsertSQL(List<T> taskList) {
        StringBuilder sb = new StringBuilder();
        try {
            Class<?> clazz = taskList.get(0).getClass();

            sb.append("INSERT INTO ").append("trigger_task").append(" (");

            Class<?> clazzTemp = clazz;
            while (clazzTemp != null) {
                Field[] fields = clazzTemp.getDeclaredFields();
                for (Field field : fields) {
                    if (!Modifier.isStatic(field.getModifiers())) {
                        sb.append(camelToSnake(field.getName())).append(", ");
                    }
                }
                clazzTemp = clazzTemp.getSuperclass();
            }

            sb.deleteCharAt(sb.length() - 2);
            sb.append(") VALUES ");

            for (T task : taskList) {
                sb.append("(");
                clazzTemp = clazz;
                while (clazzTemp != null) {
                    Field[] fields = clazzTemp.getDeclaredFields();
                    for (Field field : fields) {
                        field.setAccessible(true);
                        if (!Modifier.isStatic(field.getModifiers())) {
                            Object val = field.get(task);
                            if (val == null || Objects.equals(field.getName(), "id")) {
                                sb.append(" null, ");
                            } else {
                                sb.append("'").append(field.get(task)).append("', ");
                            }
                        }
                    }
                    clazzTemp = clazzTemp.getSuperclass();
                }
                sb.deleteCharAt(sb.length() - 2);
                sb.append("),");
            }

            sb.deleteCharAt(sb.length() - 1);
            sb.append(";");
        } catch (Exception e) {
            throw new RuntimeException("trigger task sql generate fail!");
        }
        return sb.toString();
    }

    /**
     * 字符串驼峰转成下划线
     */
    private String camelToSnake(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (Character.isUpperCase(ch)) {
                if (i != 0) {
                    result.append('_');
                }
                result.append(Character.toLowerCase(ch));
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

}
