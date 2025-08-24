package com.bank.wallet.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final Environment environment;
	private final WalletProperties walletProperties;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.csrf(AbstractHttpConfigurer::disable)
			.authorizeHttpRequests(authz -> {
				var isDocumentationEnabled = isDocumentationEnabled();
				var authorizedUrl = authz.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html");

				if (isDocumentationEnabled) {
					authorizedUrl.permitAll();
				} else {
					authorizedUrl.denyAll();
				}

				authz
					.requestMatchers("/actuator/**").permitAll()
					.requestMatchers("/api/**").permitAll()
					.anyRequest().permitAll(); // Explicitly allow other requests since we're not handling auth in this phase.
			});

		return http.build();
	}

	private boolean isDocumentationEnabled() {
		boolean isProduction = List.of(environment.getActiveProfiles()).contains("prod");
		boolean isApiDocsEnabled = walletProperties.getApp().getApiDocs().isEnabled();
		return !isProduction && isApiDocsEnabled;
	}
}
