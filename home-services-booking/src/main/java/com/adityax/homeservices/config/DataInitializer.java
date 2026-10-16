package com.adityax.homeservices.config;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.adityax.homeservices.entity.ServiceEntity;
import com.adityax.homeservices.repository.ServiceRepository;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner loadServices(ServiceRepository serviceRepository) {

        return args -> {

            if (serviceRepository.count() == 0) {

                List<ServiceEntity> services = List.of(
                    new ServiceEntity(null, "Home Cleaning",
                        "Professional home cleaning service", 999.0, 4.5),

                    new ServiceEntity(null, "Deep Cleaning",
                        "Thorough cleaning of the entire home", 1999.0, 4.8),

                    new ServiceEntity(null, "Plumbing",
                        "Repair leaking taps and plumbing issues", 499.0, 4.3),

                    new ServiceEntity(null, "Electrical Repairs",
                        "Electrical fault diagnosis and repair", 599.0, 4.4),

                    new ServiceEntity(null, "AC Servicing",
                        "Air conditioner cleaning and servicing", 799.0, 4.6),

                    new ServiceEntity(null, "Painting",
                        "Professional interior home painting", 2999.0, 4.2),

                    new ServiceEntity(null, "Carpentry",
                        "Furniture repairs and carpentry work", 699.0, 4.1),

                    new ServiceEntity(null, "Pest Control",
                        "Home pest inspection and treatment", 899.0, 4.5)
                );

                serviceRepository.saveAll(services);
            }
        };
    }
}