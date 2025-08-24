package com.bank.wallet.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.List;

@Configuration
@RequiredArgsConstructor
// Only enables this bean if the properties file says so.
@ConditionalOnProperty(name = "wallet.app.api-docs.enabled", havingValue = "true", matchIfMissing = true)
public class OpenApiConfig {

	private final WalletProperties walletProperties;
	private final Environment environment;

	@Bean
	public OpenAPI customOpenAPI() {
		var isProduction = List.of(environment.getActiveProfiles()).contains("prod");
		if (isProduction && !walletProperties.getApp().getApiDocs().isEnabled()) {
			return null;
		}

		var app = walletProperties.getApp();
		var apiDocs = app.getApiDocs();
		var contact = apiDocs.getContact();
		var server = app.getServer();

		return new OpenAPI()
			.servers(List.of(
				new Server().url(server.getUrl()).description(server.getDescription())
			))
			.info(new Info()
				.title(apiDocs.getTitle())
				.description(apiDocs.getDescription())
				.version(apiDocs.getVersion())
				.contact(new Contact()
					.name(contact.getName())
					.email(contact.getEmail())
					.url(contact.getUrl())));
	}
}
