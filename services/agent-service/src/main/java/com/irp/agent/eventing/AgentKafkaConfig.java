package com.irp.agent.eventing;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class AgentKafkaConfig {

    @Bean
    NewTopic incidentDetected() {
        return TopicBuilder.name("incident.detected").partitions(3).replicas(1).build();
    }
}
