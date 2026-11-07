package com.blinkweb.library.controller;

import com.blinkweb.library.context.TenantContext;
import com.blinkweb.library.dto.ApiResponse;
import com.blinkweb.library.dto.BookRequest;
import com.blinkweb.library.dto.BookResponse;
import com.blinkweb.library.service.BookService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller providing CRUD APIs for the Library Template.
 * All requests are automatically scoped to the calling tenant via TenantInterceptor
 * and Hibernate tenant filter.
 */
@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    /**
     * GET /api/books
     * Retrieves all books for the current tenant.
     * Supports optional pagination and category filtering.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Object>> getBooks(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false, defaultValue = "false") boolean paged,
            @PageableDefault(size = 20) Pageable pageable) {

        String tenantId = TenantContext.getTenantId();

        if (paged) {
            Page<BookResponse> page = bookService.getBooksPaged(categoryId, pageable);
            return ResponseEntity.ok(ApiResponse.success(page, "Books retrieved successfully", tenantId));
        }

        List<BookResponse> books = (categoryId != null)
                ? bookService.getBooksPaged(categoryId, pageable).getContent()
                : bookService.getAllBooks();

        return ResponseEntity.ok(ApiResponse.success(books, "Books retrieved successfully", tenantId));
    }

    /**
     * GET /api/books/{id}
     * Retrieves a single book by ID for the current tenant.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookResponse>> getBookById(@PathVariable Long id) {
        String tenantId = TenantContext.getTenantId();
        BookResponse book = bookService.getBookById(id);
        return ResponseEntity.ok(ApiResponse.success(book, "Book retrieved successfully", tenantId));
    }

    /**
     * GET /api/books/search?query={keyword}
     * Searches books by title or author.
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<BookResponse>>> searchBooks(@RequestParam String query) {
        String tenantId = TenantContext.getTenantId();
        List<BookResponse> results = bookService.searchBooks(query);
        return ResponseEntity.ok(ApiResponse.success(results, "Search completed successfully", tenantId));
    }

    /**
     * POST /api/books
     * Creates a new book for the current tenant.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<BookResponse>> createBook(@Valid @RequestBody BookRequest request) {
        String tenantId = TenantContext.getTenantId();
        BookResponse createdBook = bookService.createBook(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdBook, "Book created successfully", tenantId));
    }

    /**
     * PUT /api/books/{id}
     * Updates an existing book for the current tenant.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BookResponse>> updateBook(
            @PathVariable Long id,
            @Valid @RequestBody BookRequest request) {
        String tenantId = TenantContext.getTenantId();
        BookResponse updatedBook = bookService.updateBook(id, request);
        return ResponseEntity.ok(ApiResponse.success(updatedBook, "Book updated successfully", tenantId));
    }

    /**
     * DELETE /api/books/{id}
     * Deletes a book for the current tenant.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBook(@PathVariable Long id) {
        String tenantId = TenantContext.getTenantId();
        bookService.deleteBook(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Book deleted successfully", tenantId));
    }
}
