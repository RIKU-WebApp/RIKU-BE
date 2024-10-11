package RIKU.server.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // csrf 설정을 disable
                .csrf((csrfConfig) ->
                        csrfConfig.disable()
                )
                // HTTP 인증 요구 비활성화
//                .authorizeHttpRequests(auth -> auth
//                        .anyRequest().authenticated()
//                )
                // Session 사용 설정 해제
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // h2 console 화면을 사용하기 위해 해당 옵션들 disable
                .headers((headerConfig) ->
                        headerConfig.frameOptions(frameOptionsConfig ->
                                frameOptionsConfig.disable()
                        )
                );
        // 요청별 권한 설정
//                .authorizeHttpRequests((authorizeRequests) ->
//                        authorizeRequests
//                                .requestMatchers(PathRequest.toH2Console()).permitAll()
//                                .requestMatchers("/", "/user/signup", "/user/login").permitAll()
////                                .anyRequest().authenticated()
//                );

        return http.build();
    }
}
