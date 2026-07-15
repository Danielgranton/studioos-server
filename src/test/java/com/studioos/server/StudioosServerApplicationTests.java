package com.studioos.server;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest(classes = StudioosServerApplication.class, properties = {
		"spring.datasource.url=jdbc:h2:mem:studioos_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.flyway.enabled=false",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"jwt.secret=test-secret-key-with-enough-length-for-jjwt-signing",
		"internal.service.api-key=test-internal-api-key",
		"MAIL_USERNAME=test@example.com",
		"MAIL_PASSWORD=test-password",
		"MPESA_CONSUMER_KEY=test-consumer-key",
		"MPESA_CONSUMER_SECRET=test-consumer-secret",
		"MPESA_SHORTCODE=123456",
		"MPESA_PASSKEY=test-passkey",
		"MPESA_CALLBACK_URL=http://localhost/callback",
		"MPESA_TIMEOUT_URL=http://localhost/timeout",
		"africastalking.username=test-user",
		"africastalking.apikey=test-api-key",
		"storage.s3.bucket=test-bucket",
		"opensearch.host=localhost",
		"opensearch.port=9200",
		"media.callback.grpc.enabled=false"
})
class StudioosServerApplicationTests {

	@MockBean
	private JavaMailSender mailSender;

	@MockBean
	private StringRedisTemplate redisTemplate;

	@Test
	void contextLoads() {
	}

}
