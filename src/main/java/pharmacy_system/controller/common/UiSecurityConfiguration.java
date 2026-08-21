package pharmacy_system.controller.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/** Security-filter wiring retained in the fixed controller.common boundary. */
@Configuration(proxyBeanMethods = false)
public class UiSecurityConfiguration {
    @Bean
    SecurityFilterChain pharmacySecurity(HttpSecurity http, SessionController sessionController) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .addLogoutHandler((request, response, authentication) -> sessionController.invalidateSession())
                        .logoutSuccessUrl("/login"));
        return http.build();
    }
}
