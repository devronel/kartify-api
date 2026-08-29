package com.kartify.api.category.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.kartify.api.shared.BaseEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "categories")
public class Category extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @OneToMany(mappedBy = "parent")
    private List<Category> children = new ArrayList<>();

    private String name;

    private String slug;

    private String description;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // --- Constructor ---
    public Category(){}
    public Category(
        String name,
        String slug,
        String description,
        Boolean isActive,
        Integer sortOrder
    ){
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.isActive = isActive;
        this.sortOrder = sortOrder;
    }

    // --- Getter & Setter ---
    public Long getId() { return id; }

    public Category getParent() { return parent; }
    public void setParent(Category parent) { this.parent = parent; }

    public List<Category> getChildren() { return children; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    // --- Helper Methods ---
    public void addChild(Category child) {
        this.children.add(child);
        child.setParent(this);
    }

    public void removeChild(Category child) {
        this.children.remove(child);
        child.setParent(null);
    }

}
