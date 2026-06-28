package com.yinfeng.interview;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "loadtest.master-url=http://localhost:9999"
})
@ActiveProfiles("worker")
class InterviewApplicationTests {

	@Test
	void contextLoads() {
	}

}
