package com.blinkweb.library.repository;

import com.blinkweb.library.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    /**
     * Overridden with JPQL to guarantee that Hibernate @Filter is triggered.
     * Direct entityManager.find() bypasses Hibernate filters, so using a JPQL query
     * ensures the row-level tenant filter is ALWAYS applied at the SQL level.
     */
    @Override
    @Query("SELECT b FROM Book b WHERE b.id = :id")
    Optional<Book> findById(@Param("id") Long id);

    @Query("SELECT b FROM Book b WHERE b.isbn = :isbn")
    Optional<Book> findByIsbn(@Param("isbn") String isbn);

    @Query("SELECT b FROM Book b WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(b.author) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Book> searchBooks(@Param("query") String query);

    @Query("SELECT b FROM Book b WHERE (:categoryId IS NULL OR b.category.id = :categoryId)")
    Page<Book> findAllWithCategoryFilter(@Param("categoryId") Long categoryId, Pageable pageable);

    boolean existsByIsbn(String isbn);
}
