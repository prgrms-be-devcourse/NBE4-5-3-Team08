package com.team8.project2;

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@EnableJpaAuditing
@SpringBootApplication
class Project2Application

fun main(args: Array<String>) {
	runApplication<Project2Application>(*args)
}
