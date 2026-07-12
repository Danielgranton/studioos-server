package com.studioos.server;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

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
		"media.callback.grpc.enabled=false"
})
class StudioosServerApplicationTests {

	@Test
	void contextLoads() {
	}

}
