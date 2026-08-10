import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

//告知spring 扫描 @FeignClient接口
//同时指定 需要scan的包
@SpringBootApplication(scanBasePackages = "com.movieticket")
@EnableFeignClients(basePackages = "com.movieticket")
@MapperScan("com.movieticket.order.mapper")
public class OrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
        System.out.println("--------------order-service started---------------");
    }
}
