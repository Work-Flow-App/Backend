package com.workflow.config.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.common.constant.Role;
import com.workflow.config.properties.PaddleConfigProperties;
import com.workflow.repository.company.CompanyMemberRepository;
import com.workflow.repository.company.CompanySubscriptionRepository;
import com.workflow.service.auth.JwtFilter;
import com.workflow.service.company.ICompanyService;
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

    @Value("${rate-limiting.enabled:true}")
    private boolean rateLimitingEnabled;

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
            "/api/v1/auth/signup/company-member",
            "/api/v1/auth/refresh",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/google",
            "/api/v1/auth/verify-email",
            "/api/v1/auth/resend-verification",
            "/api/v1/workers/invites/check/**",
            "/api/v1/companies/members/invitations/check",
            "/api/v1/webhooks/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/api-docs/**",
            "/aggregate/**",
            "/actuator/**",
            "/checkout-test.html"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ICompanyService companyService,
            CompanyMemberRepository companyMemberRepository,
            CompanySubscriptionRepository subscriptionRepository,
            PaddleConfigProperties paddleConfigProperties,
            ObjectMapper objectMapper) throws Exception {
        http
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
                        .requestMatchers("/api/v1/assets/**").hasRole(COMPANY)
                        .requestMatchers("/api/v1/asset-assignments/**").hasRole(COMPANY)
                        .requestMatchers("/api/v1/workflows/**").hasRole(COMPANY)
                        .requestMatchers("/api/v1/job-workflows/**").hasRole(COMPANY)
                        .requestMatchers("/api/v1/job-workflow-steps/**").hasRole(COMPANY)
                        .requestMatchers("/api/v1/customers/**").hasRole(COMPANY)
                        .requestMatchers("/api/v1/worker/**").hasRole(WORKER)
                        .anyRequest().authenticated())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(new CompanyMembershipFilter(companyMemberRepository), JwtFilter.class)
                .addFilterAfter(
                        new SubscriptionCheckFilter(
                                companyService,
                                subscriptionRepository,
                                paddleConfigProperties,
                                objectMapper),
                        CompanyMembershipFilter.class);

        if (rateLimitingEnabled) {
            http.addFilterBefore(new RateLimitingFilter(), UsernamePasswordAuthenticationFilter.class);
        }

        return http.build();
    }
}
