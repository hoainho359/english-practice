package vn.edu.iuh.english_practice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class EnglishPracticeApplication {

	public static void main(String[] args) {
		SpringApplication.run(EnglishPracticeApplication.class, args);
	}

}
