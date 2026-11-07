package com.blinkweb.library.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.Filter;

import java.math.BigDecimal;

@Entity
@Table(
        name = "library_settings",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_library_settings_tenant",
                columnNames = {"tenant_id"}
        )
)
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class LibrarySettings extends BaseTenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "library_name", nullable = false)
    private String libraryName;

    @Column(name = "borrow_limit_per_user", nullable = false)
    private int borrowLimitPerUser = 5;

    @Column(name = "borrow_duration_days", nullable = false)
    private int borrowDurationDays = 14;

    @Column(name = "late_fine_per_day", precision = 10, scale = 2)
    private BigDecimal lateFinePerDay = BigDecimal.valueOf(1.00);

    @Column(name = "contact_email")
    private String contactEmail;

    public LibrarySettings() {
    }

    public LibrarySettings(String libraryName, int borrowLimitPerUser, int borrowDurationDays, BigDecimal lateFinePerDay, String contactEmail) {
        this.libraryName = libraryName;
        this.borrowLimitPerUser = borrowLimitPerUser;
        this.borrowDurationDays = borrowDurationDays;
        this.lateFinePerDay = lateFinePerDay;
        this.contactEmail = contactEmail;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLibraryName() {
        return libraryName;
    }

    public void setLibraryName(String libraryName) {
        this.libraryName = libraryName;
    }

    public int getBorrowLimitPerUser() {
        return borrowLimitPerUser;
    }

    public void setBorrowLimitPerUser(int borrowLimitPerUser) {
        this.borrowLimitPerUser = borrowLimitPerUser;
    }

    public int getBorrowDurationDays() {
        return borrowDurationDays;
    }

    public void setBorrowDurationDays(int borrowDurationDays) {
        this.borrowDurationDays = borrowDurationDays;
    }

    public BigDecimal getLateFinePerDay() {
        return lateFinePerDay;
    }

    public void setLateFinePerDay(BigDecimal lateFinePerDay) {
        this.lateFinePerDay = lateFinePerDay;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }
}
