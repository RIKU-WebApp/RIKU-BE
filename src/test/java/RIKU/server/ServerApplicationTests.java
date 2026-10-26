package RIKU.server;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.amazonaws.services.s3.AmazonS3;

@SpringBootTest
@ActiveProfiles("test")
class ServerApplicationTests {

	@MockBean
	AmazonS3 amazonS3;

	@Test
	void contextLoads() {
	}

}
