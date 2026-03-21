package io.github.johneliud.order_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestPropertySource(properties = {
		"spring.mongodb.uri=mongodb://localhost:27017/test",
		"jwt.secret=testSecretKeyForTestingPurposeOnly123456",
		"jwt.expiration=86400000",
		"spring.kafka.bootstrap-servers=localhost:9092",
		"spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration"
})
class OrderServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
