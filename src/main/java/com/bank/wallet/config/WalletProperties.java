package com.bank.wallet.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "wallet")
public class WalletProperties {

    private App app = new App();

    @Data
    public static class App {
        private String name;
        private String version;
        private String description;
        private Server server = new Server();
        private ApiDocs apiDocs = new ApiDocs();
    }

    @Data
    public static class Server {
        private String url;
        private String description;
    }

    @Data
    public static class ApiDocs {
        private boolean enabled = true;
        private String title;
        private String description;
        private String version;
        private Contact contact = new Contact();
    }

    @Data
    public static class Contact {
        private String name;
        private String email;
        private String url;
    }
}
