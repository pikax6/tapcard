package com.zxy.tapcard.config;

import com.zxy.tapcard.auth.LoginWall;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebCfg implements WebMvcConfigurer {

    private final LoginWall loginWall;      // 构造注入

    public WebCfg(LoginWall loginWall) { this.loginWall = loginWall; }

    @Override
    public void addInterceptors(InterceptorRegistry reg) {
        reg.addInterceptor(loginWall)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/login", "/api/ping/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry reg) {
        reg.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
