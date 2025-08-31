package com.example.yelp;


import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.data.geo.Point;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@Order(1)
@Profile("!prod")
@RequiredArgsConstructor
public class SampleDataLoader implements CommandLineRunner {
    private final BusinessRepository repo;

    @Override
    public void run(String... args) {
        if (repo.count() > 0) return;

        // A few sample records (coords roughly around Bangalore)
        repo.saveAll(List.of(
                Business.builder().id(UUID.randomUUID().toString())
                        .name("Sunrise Cafe").description("Cozy coffee & brunch spot")
                        .categories(List.of("cafe","breakfast","coffee"))
                        .address("MG Road, Bengaluru")
                        .location(new Point(77.612, 12.975))
                        .phone("+91-9876543210").website("https://sunrisecafe.example.com")
                        .rating(4.3).build(),

                Business.builder().id(UUID.randomUUID().toString())
                        .name("TechFix Solutions").description("Laptop & phone repairs")
                        .categories(List.of("electronics","repair"))
                        .address("Koramangala, Bengaluru")
                        .location(new Point(77.628, 12.935))
                        .phone("+91-9900011111").website("https://techfix.example.com")
                        .rating(4.6).build(),

                Business.builder().id(UUID.randomUUID().toString())
                        .name("Spice Route Restaurant").description("Authentic Indian cuisine")
                        .categories(List.of("restaurant","indian"))
                        .address("Indiranagar, Bengaluru")
                        .location(new Point(77.640, 12.971))
                        .phone("+91-9988776655").website("https://spiceroute.example.com")
                        .rating(4.5).build(),

                Business.builder().id(UUID.randomUUID().toString())
                        .name("GreenLeaf Grocers").description("Organic produce & daily needs")
                        .categories(List.of("grocery","organic"))
                        .address("HSR Layout, Bengaluru")
                        .location(new Point(77.651, 12.912))
                        .phone("+91-9123456780").website("https://greenleaf.example.com")
                        .rating(4.1).build()
        ));
    }
}
