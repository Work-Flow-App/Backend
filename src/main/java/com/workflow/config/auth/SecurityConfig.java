package com.workflow.config.auth;

import com.workflow.common.constant.Role;
import com.workflow.service.auth.JwtFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String COMPANY = Role.COMPANY.name();
    private static final String WORKER  = Role.WORKER.name();

    @Autowired
    @Qualifier("handlerExceptionResolver")
    private HandlerExceptionResolver exceptionResolver;

    @Value("${cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    @Bean
    public JwtFilter jwtFilter(){
        return new JwtFilter(exceptionResolver);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Parse comma-separated allowed origins from environment variable
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .toList();
        configuration.setAllowedOrigins(origins);

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }


    private static final String[] PUBLIC_URLS = {
            "/api/v1/auth/login",
            "/api/v1/auth/signup",
            "/api/v1/auth/signup/worker",
            "/api/v1/auth/refresh",
            "/api/v1/auth/logout",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/google",
            "/api/v1/auth/verify-email",
            "/api/v1/auth/resend-verification",
            "/api/v1/workers/invites/check/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/api-docs/**",
            "/aggregate/**",
            "/actuator/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain (HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(PUBLIC_URLS)
                        .permitAll()
                        .requestMatchers("/api/v1/companies/**").hasRole(COMPANY)
                        .requestMatchers("/api/v1/workers/**").hasRole(COMPANY)
                        .requestMatchers("/api/v1/clients/**").hasRole(COMPANY)
                        .requestMatchers("/api/v1/jobs/**").hasRole(COMPANY)
                        .requestMatchers("/api/v1/estimates/**").hasRole(COMPANY)
                        .requestMatchers("/api/v1/line-items/**").hasRole(COMPANY)
                        .requestMatchers("/api/v1/job-templates/**").hasRole(COMPANY)
                        .requestMatchers("/api/v1/worker/**").hasRole(WORKER)
                        .anyRequest().authenticated())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtFilter(), UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
