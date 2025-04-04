package com.team8.project2.domain.image.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3Config {

	@Bean
	public S3Client amazonS3() {
		return S3Client.builder()
			.region(Region.AP_NORTHEAST_2)
			.credentialsProvider(DefaultCredentialsProvider.create())
			.build();
	}

	@Bean
	public S3Presigner s3Presigner() {
		return S3Presigner.builder()
			.region(Region.of("ap-northeast-2"))
			.credentialsProvider(DefaultCredentialsProvider.create()) // IAM 역할 또는 자격 증명 사용
			.build();
	}
}
