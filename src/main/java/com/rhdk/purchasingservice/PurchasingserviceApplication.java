package com.rhdk.purchasingservice;

import com.codingapi.txlcn.tc.config.EnableDistributedTransaction;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication
@ComponentScan(basePackages = {"com.rhdk", "com.igen"})
@MapperScan("com.rhdk.purchasingservice.mapper")
@EnableEurekaClient
@EnableAspectJAutoProxy
@ServletComponentScan
@EnableSwagger2
@EnableFeignClients
@EnableDistributedTransaction
public class PurchasingserviceApplication {

  public static void main(String[] args) {
    SpringApplication.run(PurchasingserviceApplication.class, args);
  }
}
