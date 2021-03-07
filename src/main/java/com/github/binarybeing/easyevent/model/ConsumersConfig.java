package com.github.binarybeing.easyevent.model;

import com.github.binarybeing.easyevent.Consumers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Description
 * @Author junhua5
 * @Date 2021-03-06
 **/
@Configuration
public class ConsumersConfig {
    @Bean
    public Consumers consumers(){
        return new Consumers();
    }
}
