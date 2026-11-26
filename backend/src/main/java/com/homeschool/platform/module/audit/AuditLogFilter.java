package com.homeschool.platform.module.audit;

import com.homeschool.platform.module.audit.entity.OperationLog;
import com.homeschool.platform.module.audit.mapper.OperationLogMapper;
import com.homeschool.platform.security.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class AuditLogFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AuditLogFilter.class);
    private static final Set<String> AUDITED_METHODS =
            Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final Pattern SENSITIVE_PARAMS = Pattern.compile(
            "(?i)(token|password|secret|authorization)=([^&]*)");

    private final OperationLogMapper mapper;

    public AuditLogFilter(OperationLogMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !AUDITED_METHODS.contains(request.getMethod().toUpperCase());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        long startedAt = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            Authentication authentication =
                    SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null
                    && authentication.getPrincipal() instanceof UserPrincipal principal) {
                writeLog(request, response, principal, System.currentTimeMillis() - startedAt);
            }
        }
    }

    private void writeLog(
            HttpServletRequest request,
            HttpServletResponse response,
            UserPrincipal principal,
            long durationMs) {
        try {
            OperationLog operationLog = new OperationLog();
            operationLog.setUserId(principal.getUserId());
            operationLog.setUsernameSnapshot(principal.getUsername());
            operationLog.setRoleSnapshot(String.join(",", principal.getRoles()));
            operationLog.setModule(resolveModule(request.getRequestURI()));
            operationLog.setOperation(resolveOperation(request));
            operationLog.setHttpMethod(request.getMethod());
            operationLog.setRequestUri(truncate(request.getRequestURI(), 255));
            operationLog.setRequestParams(maskParams(request.getQueryString()));
            operationLog.setResultCode(response.getStatus());
            operationLog.setClientIp(resolveClientIp(request));
            operationLog.setUserAgent(truncate(request.getHeader("User-Agent"), 500));
            operationLog.setDurationMs(durationMs);
            mapper.insert(operationLog);
        } catch (Exception exception) {
            log.error("Failed to write operation log for {}", request.getRequestURI(), exception);
        }
    }

    private String resolveModule(String uri) {
        if (uri.startsWith("/api/admin/users")) return "用户管理";
        if (uri.startsWith("/api/admin/grades")) return "年级管理";
        if (uri.startsWith("/api/admin/classes")) return "班级管理";
        if (uri.startsWith("/api/admin/exams")) return "考试管理";
        if (uri.startsWith("/api/admin/subjects")) return "学科管理";
        if (uri.startsWith("/api/notices")) return "通知管理";
        if (uri.startsWith("/api/assignments") || uri.startsWith("/api/submissions")) {
            return "作业管理";
        }
        if (uri.startsWith("/api/scores")) return "成绩管理";
        if (uri.startsWith("/api/leaves")) return "请假审批";
        if (uri.startsWith("/api/growth-records")) return "成长档案";
        if (uri.startsWith("/api/messages")) return "家校互动";
        if (uri.startsWith("/api/files")) return "附件管理";
        if (uri.startsWith("/api/auth")) return "身份认证";
        return "系统操作";
    }

    private String resolveOperation(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri.endsWith("/publish")) return "发布";
        if (uri.endsWith("/withdraw")) return "撤回";
        if (uri.endsWith("/close")) return "关闭";
        if (uri.endsWith("/cancel")) return "撤销";
        if (uri.endsWith("/decision")) return "审批";
        if (uri.endsWith("/grade")) return "批改";
        if (uri.endsWith("/return")) return "退回";
        if (uri.endsWith("/submission")) return "提交作业";
        if (uri.endsWith("/replies")) return "回复留言";
        if (uri.endsWith("/read")) return "标记已读";
        return switch (request.getMethod()) {
            case "POST" -> "新增或执行";
            case "PUT", "PATCH" -> "修改";
            case "DELETE" -> "删除";
            default -> request.getMethod();
        };
    }

    private String maskParams(String queryString) {
        if (queryString == null || queryString.isBlank()) {
            return null;
        }
        String masked = SENSITIVE_PARAMS.matcher(queryString)
                .replaceAll("$1=***");
        return truncate(masked, 2000);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return truncate(forwarded.split(",")[0].trim(), 64);
        }
        return truncate(request.getRemoteAddr(), 64);
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}

