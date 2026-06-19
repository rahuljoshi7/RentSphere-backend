package com.rentsphere.config;

import com.rentsphere.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter       jwtAuthFilter;
    private final UserDetailsService  userDetailsService;

    @Value("${app.cors.allowed-origins}")
    private String allowedOriginsRaw;

    private static final String[] PUBLIC_URLS = {
        "/api/v1/auth/**",
        "/api/v1/properties/search",
        "/api/v1/properties/cities",
        "/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/actuator/health"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_URLS).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/properties/**").permitAll()

                // Admin only
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                // Owner + Admin
                .requestMatchers(HttpMethod.POST, "/api/v1/properties/**")
                    .hasAnyRole("ADMIN", "PROPERTY_OWNER")
                .requestMatchers(HttpMethod.PUT, "/api/v1/properties/**")
                    .hasAnyRole("ADMIN", "PROPERTY_OWNER", "PROPERTY_MANAGER")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/properties/**")
                    .hasAnyRole("ADMIN", "PROPERTY_OWNER")

                // Agreements
                .requestMatchers("/api/v1/agreements/**")
                    .hasAnyRole("ADMIN", "PROPERTY_OWNER", "PROPERTY_MANAGER", "TENANT")

                // Payments
                .requestMatchers("/api/v1/payments/**")
                    .hasAnyRole("ADMIN", "PROPERTY_OWNER", "PROPERTY_MANAGER", "TENANT")

                // Maintenance
                .requestMatchers("/api/v1/maintenance/**")
                    .hasAnyRole("ADMIN", "PROPERTY_OWNER", "PROPERTY_MANAGER", "TENANT", "MAINTENANCE_STAFF")

                // Dashboard
                .requestMatchers("/api/v1/dashboard/admin")
                    .hasRole("ADMIN")
                .requestMatchers("/api/v1/dashboard/owner")
                    .hasAnyRole("ADMIN", "PROPERTY_OWNER")

                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = List.of(allowedOriginsRaw.split(","));
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
