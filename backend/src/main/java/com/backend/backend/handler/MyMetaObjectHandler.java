package com.backend.backend.handler; // 包名根据实际情况修改，这里假设为 com.backend.backend.handler。

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Mybatis-Plus MetaObjectHandler 实现类
 * 用于自动填充实体类中的 createTime 和 updateTime 字段
 */
@Slf4j
@Component // 将该处理器注册为 Spring Bean
public class MyMetaObjectHandler implements MetaObjectHandler {

    private static final String CREATE_TIME_FIELD = "createTime"; // 创建时间字段名
    private static final String UPDATE_TIME_FIELD = "updateTime"; // 更新时间字段名

    /**
     * 插入操作时填充字段
     * @param metaObject 元数据对象
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        log.debug("Start insert fill for createTime and updateTime...");
        try {
            // 填充创建时间 (如果字段存在且为 null)
            if (metaObject.hasSetter(CREATE_TIME_FIELD)) {
                 // 使用 strictInsertFill 避免重复填充，LocalDateTime.class 明确类型
                 this.strictInsertFill(metaObject, CREATE_TIME_FIELD, LocalDateTime::now, LocalDateTime.class);
                 log.debug("Filled createTime field.");
            } else {
                 log.warn("Entity does not have a setter for field: {}", CREATE_TIME_FIELD);
            }

            // 填充更新时间 (如果字段存在且为 null)
            if (metaObject.hasSetter(UPDATE_TIME_FIELD)) {
                 this.strictInsertFill(metaObject, UPDATE_TIME_FIELD, LocalDateTime::now, LocalDateTime.class);
                 log.debug("Filled updateTime field.");
            } else {
                log.warn("Entity does not have a setter for field: {}", UPDATE_TIME_FIELD);
            }
        } catch (Exception e) {
             log.error("Error during insert fill: {}", e.getMessage(), e);
        }
    }

    /**
     * 更新操作时填充字段
     * @param metaObject 元数据对象
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        log.debug("Start update fill for updateTime...");
         try {
            // 填充更新时间 (如果字段存在)
             if (metaObject.hasSetter(UPDATE_TIME_FIELD)) {
                 // 使用 strictUpdateFill 避免在没有设置更新值时也填充
                 this.strictUpdateFill(metaObject, UPDATE_TIME_FIELD, LocalDateTime::now, LocalDateTime.class);
                 log.debug("Filled updateTime field.");
            } else {
                 log.warn("Entity does not have a setter for field: {}", UPDATE_TIME_FIELD);
             }
        } catch (Exception e) {
            log.error("Error during update fill: {}", e.getMessage(), e);
         }
    }
} 