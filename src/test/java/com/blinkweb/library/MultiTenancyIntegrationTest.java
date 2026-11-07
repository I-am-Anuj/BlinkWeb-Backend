package com.blinkweb.library;

import com.blinkweb.library.dto.BookRequest;
import com.blinkweb.library.repository.BookRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MultiTenancyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookRepository bookRepository;

    @BeforeEach
    void setUp() {
        bookRepository.deleteAll();
    }

    @Test
    @DisplayName("Should reject request with 400 Bad Request when tenant identifier is missing")
    void shouldRejectRequestWhenTenantMissing() throws Exception {
        mockMvc.perform(get("/api/books"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error", is("MISSING_OR_INVALID_TENANT_ID")));
    }

    @Test
    @DisplayName("Should reject request with 402 Payment Required when tenant subscription is PAST_DUE")
    void shouldRejectRequestWhenSubscriptionPastDue() throws Exception {
        mockMvc.perform(get("/api/books")
                        .header("X-Tenant-ID", "expired-tenant"))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error", is("SUBSCRIPTION_INACTIVE")))
                .andExpect(jsonPath("$.data.subscriptionStatus", is("PAST_DUE")));
    }

    @Test
    @DisplayName("Should extract tenant from Host subdomain when X-Tenant-ID header is omitted")
    void shouldExtractTenantFromHostHeader() throws Exception {
        BookRequest request = new BookRequest(
                "978-0134685991", "Effective Java", "Joshua Bloch",
                "Addison-Wesley", 2018, 5, 5, null
        );

        mockMvc.perform(post("/api/books")
                        .header("Host", "city-books.blinkweb.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.tenantId", is("city-books")))
                .andExpect(jsonPath("$.data.tenantId", is("city-books")));
    }

    @Test
    @DisplayName("Should enforce strict row-level isolation between tenants")
    void shouldEnforceStrictRowLevelDataIsolation() throws Exception {
        // 1. Tenant 'acme-library' creates Book 1
        BookRequest acmeBook = new BookRequest(
                "978-1491950357", "Designing Data-Intensive Applications", "Martin Kleppmann",
                "O'Reilly", 2017, 3, 3, null
        );

        String acmeResponse = mockMvc.perform(post("/api/books")
                        .header("X-Tenant-ID", "acme-library")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(acmeBook)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.tenantId", is("acme-library")))
                .andReturn().getResponse().getContentAsString();

        Number acmeBookId = com.jayway.jsonpath.JsonPath.read(acmeResponse, "$.data.id");

        // 2. Tenant 'city-books' creates Book 2
        BookRequest cityBook = new BookRequest(
                "978-0132350884", "Clean Code", "Robert C. Martin",
                "Prentice Hall", 2008, 10, 10, null
        );

        mockMvc.perform(post("/api/books")
                        .header("X-Tenant-ID", "city-books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cityBook)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.tenantId", is("city-books")));

        // 3. Tenant 'acme-library' queries GET /api/books -> Should see ONLY its own book (1 book)
        mockMvc.perform(get("/api/books")
                        .header("X-Tenant-ID", "acme-library"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].title", is("Designing Data-Intensive Applications")))
                .andExpect(jsonPath("$.data[0].tenantId", is("acme-library")));

        // 4. Tenant 'city-books' queries GET /api/books -> Should see ONLY its own book (1 book)
        mockMvc.perform(get("/api/books")
                        .header("X-Tenant-ID", "city-books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].title", is("Clean Code")))
                .andExpect(jsonPath("$.data[0].tenantId", is("city-books")));

        // 5. Tenant 'city-books' attempts to directly query 'acme-library' book by ID -> Must return 404!
        mockMvc.perform(get("/api/books/" + acmeBookId)
                        .header("X-Tenant-ID", "city-books"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("RESOURCE_NOT_FOUND")));

        // 6. Tenant 'city-books' attempts to DELETE 'acme-library' book by ID -> Must return 404!
        mockMvc.perform(delete("/api/books/" + acmeBookId)
                        .header("X-Tenant-ID", "city-books"))
                .andExpect(status().isNotFound());
    }
}
