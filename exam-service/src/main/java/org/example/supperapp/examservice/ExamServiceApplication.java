package org.example.supperapp.examservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

//@SpringBootApplication(exclude = {
//        DataSourceAutoConfiguration.class,
//        HibernateJpaAutoConfiguration.class
//})
//tắt tự config của jpa khi test prj mà chưa khai báo các properties
@SpringBootApplication
public class ExamServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ExamServiceApplication.class, args);
    }
}