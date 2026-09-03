package com.kartify.api.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_files")
public class ProductFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Long size;

    @Column
    private String extension;

    @Column(name = "mime_type", nullable = false)
    private String mimeType;

    // --- Constructor ---
    public ProductFile() {}
    public ProductFile(Product product, String filename, String name, Long size, String extension, String mimeType) {
        this.product = product;
        this.filename = filename;
        this.name = name;
        this.size = size;
        this.extension = extension;
        this.mimeType = mimeType;
    }

    // --- Getters & Setters ---
    public Long getId() { return id; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }

    public String getExtension() { return extension; }
    public void setExtension(String extension) { this.extension = extension; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

}
