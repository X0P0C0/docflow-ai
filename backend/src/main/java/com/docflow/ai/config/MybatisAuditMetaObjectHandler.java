package com.docflow.ai.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.docflow.ai.auth.security.AuthContext;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MybatisAuditMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        Long currentUserId = resolveCurrentUserId();

        strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);

        if (currentUserId != null) {
            strictInsertFill(metaObject, "createBy", Long.class, currentUserId);
            strictInsertFill(metaObject, "updateBy", Long.class, currentUserId);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());

        Long currentUserId = resolveCurrentUserId();
        if (currentUserId != null) {
            strictUpdateFill(metaObject, "updateBy", Long.class, currentUserId);
        }
    }

    private Long resolveCurrentUserId() {
        Long currentUserId = AuthContext.getCurrentUserId();
        if (currentUserId != null) {
            return currentUserId;
        }
        return 0L;
    }
}
