package com.blinkweb.library.config;

import com.blinkweb.library.context.TenantContext;
import com.blinkweb.library.entity.Book;
import com.blinkweb.library.entity.Category;
import com.blinkweb.library.entity.LibrarySettings;
import com.blinkweb.library.repository.BookRepository;
import com.blinkweb.library.repository.CategoryRepository;
import com.blinkweb.library.repository.LibrarySettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final LibrarySettingsRepository settingsRepository;

    public DataInitializer(BookRepository bookRepository,
                           CategoryRepository categoryRepository,
                           LibrarySettingsRepository settingsRepository) {
        this.bookRepository = bookRepository;
        this.categoryRepository = categoryRepository;
        this.settingsRepository = settingsRepository;
    }

    @Override
    public void run(String... args) {
        log.info("Seeding initial demonstration data for tenants 'acme-library' and 'city-books'...");

        try {
            // Seed Tenant 1: acme-library
            TenantContext.setTenantId("acme-library");
            if (bookRepository.count() == 0) {
                Category techCat = categoryRepository.save(new Category("Technology & Architecture", "Software and system design books"));
                Category sciCat = categoryRepository.save(new Category("Science Fiction", "Speculative fiction"));

                bookRepository.save(new Book("978-0132350884", "Clean Code", "Robert C. Martin", "Prentice Hall", 2008, 5, 5, techCat));
                bookRepository.save(new Book("978-0134685991", "Effective Java", "Joshua Bloch", "Addison-Wesley", 2018, 4, 3, techCat));
                bookRepository.save(new Book("978-0441172719", "Dune", "Frank Herbert", "Chilton Books", 1965, 3, 2, sciCat));

                settingsRepository.save(new LibrarySettings("Acme Central Tech Library", 5, 14, BigDecimal.valueOf(0.50), "admin@acme-library.com"));
                log.info("Seeded initial books and settings for tenant 'acme-library'");
            }
        } finally {
            TenantContext.clear();
        }

        try {
            // Seed Tenant 2: city-books
            TenantContext.setTenantId("city-books");
            if (bookRepository.count() == 0) {
                Category litCat = categoryRepository.save(new Category("Classic Literature", "Historical and acclaimed works"));

                bookRepository.save(new Book("978-0141439518", "Pride and Prejudice", "Jane Austen", "T. Egerton", 1813, 8, 8, litCat));
                bookRepository.save(new Book("978-0451524935", "1984", "George Orwell", "Secker & Warburg", 1949, 12, 11, litCat));

                settingsRepository.save(new LibrarySettings("Metropolitan City Public Library", 10, 21, BigDecimal.valueOf(1.00), "info@city-books.org"));
                log.info("Seeded initial books and settings for tenant 'city-books'");
            }
        } finally {
            TenantContext.clear();
        }

        log.info("Data initialization complete.");
    }
}
