package RIKU.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class ServerApplication {

	public static void main(String[] args) {
		System.out.println("------------------------------------------------------------------------------------------");
		System.out.printf("DisplayName: %s, ID: %s, Offset: %s%n",
				TimeZone.getDefault().getDisplayName(),
				TimeZone.getDefault().getID(),
				TimeZone.getDefault().getRawOffset());
		System.out.println("------------------------------------------------------------------------------------------");
		SpringApplication.run(ServerApplication.class, args);
	}

}
