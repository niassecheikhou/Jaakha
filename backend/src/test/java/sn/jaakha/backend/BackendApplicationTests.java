package sn.jaakha.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:jaakha;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.flyway.enabled=false",
		"app.cors.allowed-origins=http://localhost:5173",
		"spring.mail.host=",
		"app.mail.from=noreply@jaakha.local",
		"app.admin.bootstrap-email="
})
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
