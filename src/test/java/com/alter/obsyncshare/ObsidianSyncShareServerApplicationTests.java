package com.alter.obsyncshare;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@SpringBootTest
class ObsidianSyncShareServerApplicationTests {

	@Test
	void contextLoads() throws IOException {

		System.out.println(Paths.get("并发3.md1/../../未命名.md").normalize());
		boolean b =Paths.get("并发3.md1/../../未命名.md").normalize().startsWith("并发3.md1");
		System.out.println(b);


		System.out.println(Files.probeContentType(Paths.get("并发3.md1/../../未命名.mdx")));
	}

}
