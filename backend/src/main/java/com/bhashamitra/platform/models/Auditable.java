package com.bhashamitra.platform.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.ZonedDateTime;
import java.util.UUID;

@MappedSuperclass
public abstract class Auditable {

    private static final String SYSTEM_USER = "system";

    @Id
    @Column(name = "id", length = 36, nullable = false)
    protected String id;

    @Column(name = "created_by", nullable = false)
    protected String createdBy;

    @Column(name = "created_date", nullable = false)
    protected ZonedDateTime createdDate;

    @Column(name = "last_modified_by", nullable = false)
    protected String lastModifiedBy;

    @Column(name = "last_modified_date", nullable = false)
    protected ZonedDateTime lastModifiedDate;

    @JsonIgnore
    @Version
    @Column(name = "version", nullable = false)
    protected Long version;

    protected Auditable() {
        this.id = UUID.randomUUID().toString();
    }

    @PrePersist
    @PreUpdate
    protected void onPersistOrUpdate() {
        ZonedDateTime now = ZonedDateTime.now();

        if (createdDate == null) {
            createdDate = now;
        }
        lastModifiedDate = now;

        if (createdBy == null) {
            createdBy = SYSTEM_USER;
        }
        if (lastModifiedBy == null) {
            lastModifiedBy = SYSTEM_USER;
        }
    }

    // ---------- getters & setters ----------

    public String getId() {
        return id;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public ZonedDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(ZonedDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public ZonedDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(ZonedDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public Long getVersion() {
        return version;
    }
}
