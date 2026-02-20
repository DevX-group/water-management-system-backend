package com.backend.water_management_system.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.backend.water_management_system.entity.Region;
import com.backend.water_management_system.repository.CustomerRepository;
import com.backend.water_management_system.repository.RegionRepository;
import com.backend.water_management_system.entity.Customer;
import java.math.BigDecimal;

@Component
public class DataSeeder implements CommandLineRunner {

    private final CustomerRepository customerRepository;
    private final RegionRepository regionRepository;

    public DataSeeder(CustomerRepository customerRepository, RegionRepository regionRepository) {
        this.customerRepository = customerRepository;
        this.regionRepository = regionRepository;
    }

    
    @Override
    public void run(String... args) throws Exception {

        if (customerRepository.count() > 0) {
            return;
        }
        // Seed regions
        Region northRegion = new Region("R001", "North");
        Region southRegion = new Region("R002", "South");
        regionRepository.save(northRegion);
        regionRepository.save(southRegion);

        // Seed customers
        Customer customer1 = new Customer("SUB123", "John Doe", northRegion, new BigDecimal("100.00"));
        Customer customer2 = new Customer("SUB456", "Jane Smith", southRegion, new BigDecimal("150.00"));
        customerRepository.save(customer1);
        customerRepository.save(customer2);
    }
    
}
