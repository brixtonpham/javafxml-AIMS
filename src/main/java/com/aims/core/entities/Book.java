package com.aims.core.entities;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.Objects;
import com.aims.core.enums.ProductType; // Fixed import for ProductType enum

@Entity
@Table(name = "BOOK")
@PrimaryKeyJoinColumn(name = "productID") // Liên kết khóa chính với bảng PRODUCT
public class Book extends Product {

    @Column(name = "authors", length = 500)
    private String authors; // Có thể có nhiều tác giả, ngăn cách bằng dấu phẩy

    @Column(name = "coverType", length = 50)
    private String coverType; // e.g., "Paperback", "Hardcover"

    @Column(name = "publisher", length = 255)
    private String publisher;

    @Column(name = "publicationDate")
    private LocalDate publicationDate;

    @Column(name = "numPages")
    private int numPages;

    @Column(name = "language", length = 50)
    private String language;

    @Column(name = "book_genre", length = 100)
    private String bookGenre;

    public Book() {
        super(); // Gọi constructor của lớp cha
        this.setProductType(ProductType.BOOK); // Set product type cho Book
    }

    // Constructor với các thuộc tính của Book và Product
    public Book(String productId, String title, String category, float valueAmount, float price, int quantityInStock, String description, String imageUrl, String barcode, String dimensionsCm, float weightKg, LocalDate entryDate,
                String authors, String coverType, String publisher, LocalDate publicationDate, int numPages, String language, String bookGenre) {
        super(productId, title, category, valueAmount, price, quantityInStock, description, imageUrl, barcode, dimensionsCm, weightKg, entryDate, ProductType.BOOK);
        this.authors = authors;
        this.coverType = coverType;
        this.publisher = publisher;
        this.publicationDate = publicationDate;
        this.numPages = numPages;
        this.language = language;
        this.bookGenre = bookGenre;
    }


    // Getters and Setters for Book specific fields
    public String getAuthors() {
        return authors;
    }

    public void setAuthors(String authors) {
        this.authors = authors;
    }

    public String getCoverType() {
        return coverType;
    }

    public void setCoverType(String coverType) {
        this.coverType = coverType;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public LocalDate getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(LocalDate publicationDate) {
        this.publicationDate = publicationDate;
    }

    public int getNumPages() {
        return numPages;
    }

    public void setNumPages(int numPages) {
        this.numPages = numPages;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getBookGenre() {
        return bookGenre;
    }

    public void setBookGenre(String bookGenre) {
        this.bookGenre = bookGenre;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false; // So sánh cả thuộc tính của lớp cha
        Book book = (Book) o;
        return Objects.equals(getProductId(), book.getProductId()); // So sánh dựa trên ID từ lớp cha là đủ
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getProductId());
    }

    @Override
    public String toString() {
        return "Book{" +
               "productId='" + getProductId() + '\'' +
               ", title='" + getTitle() + '\'' +
               ", authors='" + authors + '\'' +
               ", publisher='" + publisher + '\'' +
               '}';
    }

    @Override
    public void setProductType(ProductType productType) {
        super.setProductType(productType);
    }
}