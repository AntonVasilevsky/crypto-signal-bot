package happy.birthday.bot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import java.util.concurrent.locks.ReentrantLock;

@Configuration
public class AppConfig {
    @Bean
    ReentrantLock getLock() {
        return new ReentrantLock();
    }
}
