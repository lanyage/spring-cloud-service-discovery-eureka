package com.lanyage.eureka.consumerhello;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableDiscoveryClient
public class ConsumerHelloApplication {

    @Bean
    @LoadBalanced   //负载均衡,也就是软负载均衡,该注解能让restTemplate启用客户端负载均衡
    RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Primary
    @Bean(name="lbcRestTemplate")
    RestTemplate lbcRestTemplate() {
        return new RestTemplate();
    }
    public static void main(String[] args) {
        SpringApplication.run(ConsumerHelloApplication.class, args);
    }
}
