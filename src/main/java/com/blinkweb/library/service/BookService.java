package com.blinkweb.library.service;

import com.blinkweb.library.dto.BookRequest;
import com.blinkweb.library.dto.BookResponse;
import com.blinkweb.library.entity.Book;
import com.blinkweb.library.entity.Category;
import com.blinkweb.library.exception.ResourceNotFoundException;
import com.blinkweb.library.repository.BookRepository;
import com.blinkweb.library.repository.CategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service managing books.
 * Notice that methods do not need explicit "WHERE tenant_id = ?" clauses:
 * The Hibernate tenant filter (activated via HibernateFilterAspect) and
 * the JPA EntityListener seamlessly ensure complete multi-tenant isolation.
 */
@Service
@Transactional
public class BookService {

    private static final Logger log = LoggerFactory.getLogger(BookService.class);

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;

    public BookService(BookRepository bookRepository, CategoryRepository categoryRepository) {
        this.bookRepository = bookRepository;
        this.categoryRepository = categoryRepository;
    }

    /**
     * Creates a new book for the current tenant.
     * The tenant_id is automatically assigned by TenantEntityListener during persist.
     */
    public BookResponse createBook(BookRequest request) {
        log.info("Creating book with ISBN [{}] and title [{}]", request.getIsbn(), request.getTitle());

        // Uniqueness check is automatically scoped to the current tenant by Hibernate filter
        if (bookRepository.existsByIsbn(request.getIsbn())) {
            throw new IllegalArgumentException(String.format("Book with ISBN '%s' already exists in this library.", request.getIsbn()));
        }

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));
        }

        Book book = new Book();
        book.setIsbn(request.getIsbn().trim());
        book.setTitle(request.getTitle().trim());
        book.setAuthor(request.getAuthor().trim());
        book.setPublisher(request.getPublisher());
        book.setPublishedYear(request.getPublishedYear());
        book.setTotalCopies(request.getTotalCopies());
        book.setAvailableCopies(request.getAvailableCopies());
        book.setCategory(category);

        Book savedBook = bookRepository.save(book);
        log.info("Book saved with ID [{}] for tenant [{}]", savedBook.getId(), savedBook.getTenantId());

        return BookResponse.fromEntity(savedBook);
    }

    /**
     * Returns all books belonging ONLY to the active tenant.
     */
    @Transactional(readOnly = true)
    public List<BookResponse> getAllBooks() {
        return bookRepository.findAll().stream()
                .map(BookResponse::fromEntity)
                .toList();
    }

    /**
     * Returns a paginated list of books with optional category filtering.
     */
    @Transactional(readOnly = true)
    public Page<BookResponse> getBooksPaged(Long categoryId, Pageable pageable) {
        return bookRepository.findAllWithCategoryFilter(categoryId, pageable)
                .map(BookResponse::fromEntity);
    }

    /**
     * Finds a book by ID.
     * Guaranteed to return 404 if the book belongs to a different tenant.
     */
    @Transactional(readOnly = true)
    public BookResponse getBookById(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book", id));
        return BookResponse.fromEntity(book);
    }

    /**
     * Searches books by title or author for the current tenant.
     */
    @Transactional(readOnly = true)
    public List<BookResponse> searchBooks(String query) {
        if (query == null || query.isBlank()) {
            return getAllBooks();
        }
        return bookRepository.searchBooks(query.trim()).stream()
                .map(BookResponse::fromEntity)
                .toList();
    }

    /**
     * Updates an existing book for the current tenant.
     */
    public BookResponse updateBook(Long id, BookRequest request) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book", id));

        // If ISBN changed, check uniqueness within tenant
        if (!book.getIsbn().equalsIgnoreCase(request.getIsbn()) && bookRepository.existsByIsbn(request.getIsbn())) {
            throw new IllegalArgumentException(String.format("Book with ISBN '%s' already exists in this library.", request.getIsbn()));
        }

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));
        }

        book.setIsbn(request.getIsbn().trim());
        book.setTitle(request.getTitle().trim());
        book.setAuthor(request.getAuthor().trim());
        book.setPublisher(request.getPublisher());
        book.setPublishedYear(request.getPublishedYear());
        book.setTotalCopies(request.getTotalCopies());
        book.setAvailableCopies(request.getAvailableCopies());
        book.setCategory(category);

        Book updatedBook = bookRepository.save(book);
        return BookResponse.fromEntity(updatedBook);
    }

    /**
     * Deletes a book by ID for the current tenant.
     */
    public void deleteBook(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book", id));
        bookRepository.delete(book);
        log.info("Book with ID [{}] deleted for tenant [{}]", id, book.getTenantId());
    }
}
