package com.lodhi.auth.security;

import java.util.Set;
import java.util.stream.Collectors;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Enforces {@link RequiresPermission} by requiring every listed permission
 * to be present among the authenticated principal's authorities.
 *
 * Permission authorities are placed on the authentication by JwtAuthenticationFilter
 * (extracted from the JWT "permissions" claim), so revoked or changed role mappings
 * take effect with the next issued token.
 */
@Aspect
@Component
public class RequiresPermissionAspect {

    @Before("@annotation(com.lodhi.auth.security.RequiresPermission) || @within(com.lodhi.auth.security.RequiresPermission)")
    public void enforce(JoinPoint joinPoint) {
        RequiresPermission required = resolveAnnotation(joinPoint);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Authentication required");
        }

        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        for (String permission : required.value()) {
            if (!authorities.contains(permission)) {
                throw new AccessDeniedException("Missing required permission: " + permission);
            }
        }
    }

    private RequiresPermission resolveAnnotation(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        RequiresPermission annotation =
                AnnotatedElementUtils.findMergedAnnotation(signature.getMethod(), RequiresPermission.class);
        if (annotation == null) {
            annotation = AnnotatedElementUtils.findMergedAnnotation(joinPoint.getTarget().getClass(), RequiresPermission.class);
        }
        return annotation;
    }
}
