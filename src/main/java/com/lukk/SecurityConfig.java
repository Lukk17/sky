package com.lukk;

import com.lukk.service.SpringDataUserDetailsServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter{


    /**
     *
     * For password encryption
     *
     * @return encrypted password
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder()
    {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SpringDataUserDetailsServiceImpl springDataUserDetailsService()
    {
        return new SpringDataUserDetailsServiceImpl();
    }

    /**
     *
     * Method set up website's addresses and permission to access them.
     * It is specified by authentication.
     *
     *
     *
     * @param httpSecurity
     * @throws Exception
     */
    @Override
    protected void configure(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.csrf().disable()
                .authorizeRequests()
                .antMatchers("/").permitAll()
                .antMatchers("/userList").hasAnyRole("USER","ADMIN")
                .antMatchers("/user").hasAnyRole("USER","ADMIN")
                .antMatchers("/user/**").hasAnyRole("USER","ADMIN")
                .antMatchers("/admin").hasRole("ADMIN")
                .antMatchers("/admin/**").hasRole("ADMIN")
                .antMatchers("/offer/**").authenticated()
                .and().formLogin().loginPage("/login")
                .failureUrl("/loginError")
                .and().logout().logoutUrl("/logout").permitAll();

        // TODO check if roles in DB should be named 'USER' or 'ROLE_USER' etc.
    }



}