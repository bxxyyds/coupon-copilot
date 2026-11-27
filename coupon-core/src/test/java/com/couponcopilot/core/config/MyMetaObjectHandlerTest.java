package com.couponcopilot.core.config;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import lombok.Data;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MyMetaObjectHandlerTest {

    private final MyMetaObjectHandler handler = new MyMetaObjectHandler();

    @Data
    static class DummyEntity {
        @TableField(fill = FieldFill.INSERT)
        private LocalDateTime createTime;

        @TableField(fill = FieldFill.INSERT_UPDATE)
        private LocalDateTime updateTime;

        @TableField(fill = FieldFill.INSERT)
        private Integer delFlag;
    }

    @BeforeAll
    static void initTableInfo() {
        // strictInsertFill 会从 TableInfoHelper 读取 @TableField(fill=...) 元数据
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), DummyEntity.class);
    }

    @Test
    void insertFillShouldPopulateDefaultFields() {
        DummyEntity entity = new DummyEntity();
        MetaObject metaObject = SystemMetaObject.forObject(entity);

        handler.insertFill(metaObject);

        assertNotNull(entity.getCreateTime());
        assertNotNull(entity.getUpdateTime());
        assertEquals(0, entity.getDelFlag());
    }

    @Test
    void insertFillShouldNotOverwriteExistingValues() {
        LocalDateTime fixedTime = LocalDateTime.of(2026, 1, 1, 12, 0);
        DummyEntity entity = new DummyEntity();
        entity.setCreateTime(fixedTime);
        entity.setDelFlag(1);
        MetaObject metaObject = SystemMetaObject.forObject(entity);

        handler.insertFill(metaObject);

        assertEquals(fixedTime, entity.getCreateTime());
        assertEquals(1, entity.getDelFlag());
        assertNotNull(entity.getUpdateTime());
    }

    @Test
    void updateFillShouldRefreshExistingUpdateTime() {
        LocalDateTime oldTime = LocalDateTime.of(2020, 1, 1, 0, 0);
        DummyEntity entity = new DummyEntity();
        entity.setUpdateTime(oldTime);
        MetaObject metaObject = SystemMetaObject.forObject(entity);

        handler.updateFill(metaObject);

        assertNotNull(entity.getUpdateTime());
        assertTrue(entity.getUpdateTime().isAfter(oldTime));
    }
}
