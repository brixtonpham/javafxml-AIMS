package com.aims.core.entities;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.Objects;
import com.aims.core.enums.ProductType;

@Entity
@Table(name = "LP")
@PrimaryKeyJoinColumn(name = "productID")
public class LP extends Product {

    @Column(name = "artists", length = 500)
    private String artists;

    @Column(name = "recordLabel", length = 255)
    private String recordLabel;

    @Lob
    @Column(name = "tracklist")
    private String tracklist; // Can be a long string, e.g., JSON or comma-separated

    @Column(name = "genre", length = 100)
    private String genre;

    @Column(name = "releaseDate")
    private LocalDate releaseDate;

    public LP() {
        super();
        this.setProductType(ProductType.LP);
    }
    
    public LP(String productId, String title, String category, float valueAmount, float price, int quantityInStock, String description, String imageUrl, String barcode, String dimensionsCm, float weightKg, LocalDate entryDate,
              String artists, String recordLabel, String tracklist, String genre, LocalDate releaseDate) {
        super(productId, title, category, valueAmount, price, quantityInStock, description, imageUrl, barcode, dimensionsCm, weightKg, entryDate, ProductType.LP);
        this.artists = artists;
        this.recordLabel = recordLabel;
        this.tracklist = tracklist;
        this.genre = genre;
        this.releaseDate = releaseDate;
    }

    // Getters and Setters
    public String getArtists() {
        return artists;
    }

    public void setArtists(String artists) {
        this.artists = artists;
    }

    public String getRecordLabel() {
        return recordLabel;
    }

    public void setRecordLabel(String recordLabel) {
        this.recordLabel = recordLabel;
    }

    public String getTracklist() {
        return tracklist;
    }

    public void setTracklist(String tracklist) {
        this.tracklist = tracklist;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public LocalDate getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(LocalDate releaseDate) {
        this.releaseDate = releaseDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        LP lp = (LP) o;
        return Objects.equals(getProductId(), lp.getProductId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getProductId());
    }

    @Override
    public String toString() {
         return "LP{" +
               "productId='" + getProductId() + '\'' +
               ", title='" + getTitle() + '\'' +
               ", artists='" + artists + '\'' +
               '}';
    }
}