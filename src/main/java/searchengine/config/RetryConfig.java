package searchengine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "retry")
public class RetryConfig {

    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        // Настройка политики повторных попыток
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();

        // Настройка временной задержки между попытками
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();

        // Настройка условий для повторных попыток
        FixedBackOffPolicy fixedBackOff = new FixedBackOffPolicy();

        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        return retryTemplate;
    }
}
