package io.worldloom.config;

import io.worldloom.world.DynamoWorldRepository;
import io.worldloom.world.WorldRepository;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Configuration
public class DynamoDbConfiguration {
    @Bean(destroyMethod = "close")
    DynamoDbClient dynamoDbClient(@Value("${worldloom.region}") String region) {
        return DynamoDbClient.builder().region(Region.of(region))
                .overrideConfiguration(c -> c.apiCallTimeout(Duration.ofSeconds(15))
                        .apiCallAttemptTimeout(Duration.ofSeconds(5))).build();
    }

    @Bean
    WorldRepository worldRepository(DynamoDbClient client,
                                     @Value("${worldloom.table}") String table) {
        return new DynamoWorldRepository(client, table);
    }
}
