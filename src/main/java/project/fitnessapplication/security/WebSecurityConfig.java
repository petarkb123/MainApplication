package project.fitnessapplication.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import project.fitnessapplication.user.model.User;
import project.fitnessapplication.user.repository.UserRepository;

@Configuration
@EnableMethodSecurity
public class WebSecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    UserDetailsService userDetailsService(UserRepository users) {
        return username -> {
            User u = users.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            return org.springframework.security.core.userdetails.User
                    .withUsername(u.getUsername())
                    .password(u.getPasswordHash())
                    .roles(u.getRole().name())
                    .disabled(!u.isActive())
                    .build();
        };
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http, Environment env) throws Exception {

        String rememberCookie = env.getProperty(
                "app.security.remember-me.cookie", "FITPOWER_REMEMBER_ME");

        int validity = env.getProperty(
                "app.security.remember-me.validity-seconds", Integer.class, 1800); 

        http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/actuator/**"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/index", "/login", "/register",
                                "/terms", "/privacy",
                                "/css/**", "/js/**", "/images/**", "/webjars/**", "/actuator/**",
                                "/media/**", "/workouts/templates/*/exercises" 
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/", true)
                        .permitAll()
                )
                .rememberMe(rm -> rm
                        .rememberMeCookieName(rememberCookie)
                        .tokenValiditySeconds(validity)
                        .key("change-this-very-long-random-secret-key")
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID", rememberCookie)
                        .permitAll()
                );

        return http.build();
    }
}
