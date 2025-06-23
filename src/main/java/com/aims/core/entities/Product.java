package com.aims.core.entities;

import com.aims.core.enums.ProductType;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.Objects;
import java.util.List; // For potential OrderItems relationship

@Entity
@Table(name = "PRODUCT")
@Inheritance(strategy = InheritanceType.JOINED) // Sử dụng JOINED strategy cho kế thừa
public class Product {

    @Id
    @Column(name = "productID", length = 50)
    private String productId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "value_amount") // Đổi tên để tránh trùng keyword 'value'
    private float valueAmount;

    @Column(name = "price", nullable = false)
    private float price;

    @Column(name = "quantityInStock")
    private int quantityInStock;

    @Lob // For potentially long descriptions
    @Column(name = "description")
    private String description;

    @Column(name = "imageURL", length = 512)
    private String imageUrl;

    @Column(name = "barcode", length = 100, unique = true)
    private String barcode;

    @Column(name = "dimensions_cm", length = 50)
    private String dimensionsCm; // e.g., "30x20x10"

    @Column(name = "weight_kg")
    private float weightKg;

    @Column(name = "entryDate")
    private LocalDate entryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "productType", nullable = false, length = 50)
    private ProductType productType;

    // Optimistic locking field for preventing race conditions
    @Version
    @Column(name = "version")
    private Long version = 0L;

    // Relationship to OrderItem (one product can be in many order items)
    // MappedBy refers to the 'product' field in OrderItem entity
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> orderItems;
    
    // Relationship to CartItem
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CartItem> cartItems;


    public Product() {
    }

    public Product(String productId, String title, String category, float valueAmount, float price, int quantityInStock, String description, String imageUrl, String barcode, String dimensionsCm, float weightKg, LocalDate entryDate, ProductType productType) {
        this.productId = productId;
        this.title = title;
        this.category = category;
        this.valueAmount = valueAmount;
        this.price = price;
        this.quantityInStock = quantityInStock;
        this.description = description;
        this.imageUrl = imageUrl;
        this.barcode = barcode;
        this.dimensionsCm = dimensionsCm;
        this.weightKg = weightKg;
        this.entryDate = entryDate;
        this.productType = productType;
    }

    // Getters and Setters
    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    /**
     * Alias for getProductId() to maintain compatibility with legacy code
     */
    public String getId() {
        return productId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public float getValueAmount() {
        return valueAmount;
    }

    public void setValueAmount(float valueAmount) {
        this.valueAmount = valueAmount;
    }

    public float getPrice() {
        return price;
    }

    public void setPrice(float price) {
        this.price = price;
    }

    public int getQuantityInStock() {
        return quantityInStock;
    }

    public void setQuantityInStock(int quantityInStock) {
        this.quantityInStock = quantityInStock;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getDimensionsCm() {
        return dimensionsCm;
    }

    public void setDimensionsCm(String dimensionsCm) {
        this.dimensionsCm = dimensionsCm;
    }

    public float getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(float weightKg) {
        this.weightKg = weightKg;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(LocalDate entryDate) {
        this.entryDate = entryDate;
    }

    public ProductType getProductType() {
        return productType;
    }

    public void setProductType(ProductType productType) {
        this.productType = productType;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public List<OrderItem> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(List<OrderItem> orderItems) {
        this.orderItems = orderItems;
    }
    
    public List<CartItem> getCartItems() {
        return cartItems;
    }

    public void setCartItems(List<CartItem> cartItems) {
        this.cartItems = cartItems;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return Objects.equals(productId, product.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId);
    }

    @Override
    public String toString() {
        return "Product{" +
               "productId='" + productId + '\'' +
               ", title='" + title + '\'' +
               ", productType=" + productType +
               ", price=" + price +
               '}';
    }
}