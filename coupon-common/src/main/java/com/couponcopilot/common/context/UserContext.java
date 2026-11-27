package com.couponcopilot.common.context;

import com.couponcopilot.common.exception.ClientException;

/**
 * 基于 ThreadLocal 保存当前请求的用户上下文，请求结束务必调用 clear() 防止线程复用串号。
 */
public final class UserContext {

    private static final ThreadLocal<UserInfo> CONTEXT = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(UserInfo userInfo) {
        if (userInfo == null) {
            CONTEXT.remove();
            return;
        }
        CONTEXT.set(userInfo);
    }

    public static UserInfo get() {
        return CONTEXT.get();
    }

    public static Long getUserId() {
        UserInfo info = CONTEXT.get();
        return info != null ? info.getUserId() : null;
    }

    public static Long getShopId() {
        UserInfo info = CONTEXT.get();
        return info != null ? info.getShopId() : null;
    }

    public static UserRole getRole() {
        UserInfo info = CONTEXT.get();
        return info != null ? info.getRole() : null;
    }

    public static Long requireUserId() {
        Long userId = getUserId();
        if (userId == null) {
            throw new ClientException("缺少请求头 X-User-Id");
        }
        return userId;
    }

    public static Long requireShopId() {
        Long shopId = getShopId();
        if (shopId == null) {
            throw new ClientException("缺少请求头 X-Shop-Id");
        }
        return shopId;
    }

    public static void requireRole(UserRole expected) {
        if (getRole() != expected) {
            throw new ClientException("当前身份无权执行该操作");
        }
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
