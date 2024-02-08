package happy.birthday.bot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;
@EnableScheduling
@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BotConfig {

    @Value("${bot.name}")
    String name;
    @Value("${bot.token}")
    String token;
    @Value("${bot.owner}")
    String botOwner;

    @Bean
    public String name() {
        return name;
    }
    @Bean
    public String token() {
        return token;
    }
    @Bean
    public Long owner() {
        return Long.parseLong(botOwner);
    }

}
