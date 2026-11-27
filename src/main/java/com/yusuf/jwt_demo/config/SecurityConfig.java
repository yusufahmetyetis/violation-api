package com.yusuf.jwt_demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity      // @PreAuthorize'ı devreye alır
public class SecurityConfig {

    // Token GEREKTİRMEYEN adresler. Tek liste: hem permitAll kuralı hem de
    // "Bearer header'ı yok say" kuralı buradan beslenir, ikisi asla ayrışmaz.
    private static final String[] PUBLIC_PATHS = {
            "/error",
            "/api/public/**",
            "/auth/login",             // /auth/** DEĞİL: /auth/logout token ister ve token'ı okuyabilmeli
                                       // (bu listedeki yollarda Bearer header'ı yok sayılıyor, aşağıya bakın)
            "/v3/api-docs*",           // /v3/api-docs ve /v3/api-docs.yaml (sözleşme dosyası)
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            JwtAuthenticationConverter jwtConverter,
                                            AuthenticationEntryPoint jsonAuthEntryPoint,
                                            AccessDeniedHandler jsonAccessDeniedHandler) throws Exception {
        http
                // Token header'da geliyor, cookie yok → CSRF'in dayanağı yok
                .csrf(csrf -> csrf.disable())
                // Sunucu oturum tutmaz, her istek kendi token'ıyla gelir
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_PATHS).permitAll()

                        // Rol kuralları: yukarıdan aşağı, ilk eşleşen kazanır
                        .requestMatchers(HttpMethod.GET, "/api/violations").hasAnyRole("OPERATOR", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/violations").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/violations/*").hasRole("ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // /mine ve /{id}: sahiplik kontrolü @PreAuthorize ile
                        .anyRequest().authenticated()
                )
                // Token HİÇ yoksa / yetki yoksa (URL kuralı ve @PreAuthorize)
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(jsonAuthEntryPoint)
                        .accessDeniedHandler(jsonAccessDeniedHandler))

                // Token var ama bozuk/süresi dolmuş: Bearer filtresi kendi giriş noktasını kullanır
                .oauth2ResourceServer(o -> o
                        .bearerTokenResolver(publicPathsIgnoringResolver())
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter))
                        .authenticationEntryPoint(jsonAuthEntryPoint)
                        .accessDeniedHandler(jsonAccessDeniedHandler));

        return http.build();
    }

    // Spring, Authorization header'ı gördüğü her istekte token'ı doğrular; adres permitAll olsa bile.
    // Sonuç: süresi dolmuş token'la login'e gelen kullanıcı, yeni token almak isterken 401 alır.
    // Çözüm: public adreslerde token'ı hiç okumuyoruz (zaten gerekmiyor).
    // Korumalı adreslerde davranış AYNI: token yoksa/bozuksa yine 401.
    private static BearerTokenResolver publicPathsIgnoringResolver() {
        DefaultBearerTokenResolver standard = new DefaultBearerTokenResolver();
        RequestMatcher publicPaths = new OrRequestMatcher(Arrays.stream(PUBLIC_PATHS)
                .map(p -> (RequestMatcher) PathPatternRequestMatcher.withDefaults().matcher(p))
                .toList());
        return request -> publicPaths.matches(request) ? null : standard.resolve(request);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        // Hash'in başındaki {bcrypt} önekine bakıp algoritmayı seçer
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(UserDetailsService uds, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(uds);   // DbUserDetailsService
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }
}