package com.dheeraj.airline;

import org.springframework.boot.SpringApplication;

public class TestAirlineReservationSystemApplication {

	public static void main(String[] args) {
		SpringApplication.from(AirlineReservationSystemApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
