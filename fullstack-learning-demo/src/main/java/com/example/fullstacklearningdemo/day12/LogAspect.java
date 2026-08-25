package com.example.fullstacklearningdemo.day12;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Aspect
@Component
public class LogAspect {

  @Autowired
  private LogService logService;

  @Autowired
  private ObjectMapper objectMapper;

  // 环绕切入点：拦截所有贴有 @Log 注解的方法
  @Pointcut("@annotation(com.example.fullstacklearningdemo.day12.Log)")
  public void logPointcut() {
  }

  @Around("logPointcut()")
  public Object around(ProceedingJoinPoint point) throws Throwable {
    long startTime = System.currentTimeMillis();

    // 执行目标业务方法
    Object result = point.proceed();

    long executionTime = System.currentTimeMillis() - startTime;

    // 异步记录日志
    handleLog(point, executionTime);

    return result;
  }

  private void handleLog(ProceedingJoinPoint point, long executionTime) {
    try {
      ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

      MethodSignature signature = (MethodSignature) point.getSignature();
      Method method = signature.getMethod();
      Log logAnnotation = method.getAnnotation(Log.class);

      SysLog sysLog = new SysLog();
      if (logAnnotation != null) {
        sysLog.setTitle(logAnnotation.value());
      }

      if (request != null) {
        sysLog.setRequestUrl(request.getRequestURI());
        sysLog.setIp(request.getRemoteAddr());
      }

      sysLog.setMethod(point.getTarget().getClass().getName() + "." + method.getName() + "()");
      sysLog.setExecutionTime(executionTime);
      sysLog.setCreateTime(LocalDateTime.now());

      // 获取当前登录用户
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication != null && authentication.getPrincipal() != null) {
        sysLog.setUsername(authentication.getPrincipal().toString());
      } else {
        sysLog.setUsername("匿名用户");
      }

      // 参数转 JSON 字符串
      sysLog.setParams(objectMapper.writeValueAsString(point.getArgs()));

      // 异步提交给日志服务处理
      logService.saveLogAsync(sysLog);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}