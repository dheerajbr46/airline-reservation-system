package com.dheeraj.airline.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI airlineReservationOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Airline Reservation and Seat Inventory Platform")
                        .version("v1")
                        .description(
                                "Backend platform for flight inventory, seat holds, bookings, cancellations, and availability"
                        ))
                .tags(List.of(
                        new Tag().name("Flights").description("Flight inventory management"),
                        new Tag().name("Flight Search").description("Flight discovery by route and date"),
                        new Tag().name("Seats").description("Seat inventory and availability"),
                        new Tag().name("Seat Holds").description("Temporary seat holds and hold lifecycle operations"),
                        new Tag().name("Bookings").description("Booking confirmation, retrieval, history, and cancellation"),
                        new Tag().name("Waitlist").description("Waitlist queue management and promotion into temporary holds")
                ));
    }
}
