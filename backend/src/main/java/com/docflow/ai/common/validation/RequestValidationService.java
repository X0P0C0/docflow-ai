package com.docflow.ai.common.validation;

import com.docflow.ai.common.enums.ResultCode;
import com.docflow.ai.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 请求验证服务 —— 统一输入验证
 * <p>
 * 验证策略：
 * <ul>
 *   <li>SQL 注入检测</li>
 *   <li>路径遍历检测</li>
 *   <li>富文本清理</li>
 *   <li>手机号、邮箱格式验证</li>
 * </ul>
 * <p>
 * 面试考点：
 * <ul>
 *   <li>白名单 vs 黑名单验证</li>
 *   <li>参数化查询防 SQL 注入</li>
 *   <li>OWASP Top 10 安全风险</li>
 * </ul>
 */
@Component
public class RequestValidationService {

    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
            "(?i)(union|select|insert|update|delete|drop|truncate|exec|execute|xp_|sp_|0x|\\\\)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(
            "(\\.\\./|\\.\\\\|/etc/|/proc/|/sys/)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^1[3-9]\\d{9}$"
    );

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile(
            "<[^>]+>"
    );

    /**
     * 检测 SQL 注入
     */
    public void checkSqlInjection(String input) {
        if (input != null && SQL_INJECTION_PATTERN.matcher(input).find()) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "输入包含非法字符");
        }
    }

    /**
     * 检测路径遍历
     */
    public void checkPathTraversal(String path) {
        if (path != null && PATH_TRAVERSAL_PATTERN.matcher(path).find()) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "路径包含非法字符");
        }
    }

    /**
     * 验证邮箱格式
     */
    public boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * 验证手机号格式
     */
    public boolean isValidPhone(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone).matches();
    }

    /**
     * 清理 HTML 标签
     */
    public String stripHtml(String input) {
        if (input == null) return null;
        return HTML_TAG_PATTERN.matcher(input).replaceAll("");
    }

    /**
     * 验证字符串长度
     */
    public void checkLength(String input, int maxLength, String fieldName) {
        if (input != null && input.length() > maxLength) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR,
                    fieldName + "长度不能超过 " + maxLength + " 个字符");
        }
    }

    /**
     * 验证非空
     */
    public void checkNotEmpty(String input, String fieldName) {
        if (input == null || input.trim().isEmpty()) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR,
                    fieldName + "不能为空");
        }
    }

    /**
     * 综合验证文本字段
     */
    public String sanitizeTextInput(String input, int maxLength, String fieldName) {
        if (input == null) return null;
        checkSqlInjection(input);
        checkLength(input, maxLength, fieldName);
        return input.trim();
    }
}
