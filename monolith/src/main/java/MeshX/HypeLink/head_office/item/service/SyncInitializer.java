package MeshX.HypeLink.head_office.item.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class SyncInitializer {
    private final KafkaItemSyncService kafkaService;

    @Bean
    public ApplicationRunner runSyncOnceAfterStartup() {
        return args -> kafkaService.sync();
    }
}