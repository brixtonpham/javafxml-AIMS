package com.aims.core.entities;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.Objects;
import com.aims.core.enums.ProductType;

@Entity
@Table(name = "DVD")
@PrimaryKeyJoinColumn(name = "productID")
public class DVD extends Product {

    @Column(name = "discType", length = 50) // e.g., "Blu-ray", "HD-DVD", "DVD"
    private String discType;

    @Column(name = "director", length = 255)
    private String director;

    @Column(name = "runtime_minutes")
    private int runtimeMinutes;

    @Column(name = "studio", length = 255)
    private String studio;

    @Column(name = "dvd_language", length = 50)
    private String dvdLanguage;

    @Column(name = "subtitles", length = 255) // e.g., "English, Vietnamese"
    private String subtitles;

    @Column(name = "dvd_releaseDate")
    private LocalDate dvdReleaseDate;

    @Column(name = "dvd_genre", length = 100)
    private String dvdGenre;

    public DVD() {
        super();
        this.setProductType(ProductType.DVD);
    }

    public DVD(String productId, String title, String category, float valueAmount, float price, int quantityInStock, String description, String imageUrl, String barcode, String dimensionsCm, float weightKg, LocalDate entryDate,
               String discType, String director, int runtimeMinutes, String studio, String dvdLanguage, String subtitles, LocalDate dvdReleaseDate, String dvdGenre) {
        super(productId, title, category, valueAmount, price, quantityInStock, description, imageUrl, barcode, dimensionsCm, weightKg, entryDate, ProductType.DVD);
        this.discType = discType;
        this.director = director;
        this.runtimeMinutes = runtimeMinutes;
        this.studio = studio;
        this.dvdLanguage = dvdLanguage;
        this.subtitles = subtitles;
        this.dvdReleaseDate = dvdReleaseDate;
        this.dvdGenre = dvdGenre;
    }

    // Getters and Setters
    public String getDiscType() {
        return discType;
    }

    public void setDiscType(String discType) {
        this.discType = discType;
    }

    public String getDirector() {
        return director;
    }

    public void setDirector(String director) {
        this.director = director;
    }

    public int getRuntimeMinutes() {
        return runtimeMinutes;
    }

    public void setRuntimeMinutes(int runtimeMinutes) {
        this.runtimeMinutes = runtimeMinutes;
    }

    public String getStudio() {
        return studio;
    }

    public void setStudio(String studio) {
        this.studio = studio;
    }

    public String getDvdLanguage() {
        return dvdLanguage;
    }

    public void setDvdLanguage(String dvdLanguage) {
        this.dvdLanguage = dvdLanguage;
    }

    public String getSubtitles() {
        return subtitles;
    }

    public void setSubtitles(String subtitles) {
        this.subtitles = subtitles;
    }

    public LocalDate getDvdReleaseDate() {
        return dvdReleaseDate;
    }

    public void setDvdReleaseDate(LocalDate dvdReleaseDate) {
        this.dvdReleaseDate = dvdReleaseDate;
    }

    public String getDvdGenre() {
        return dvdGenre;
    }

    public void setDvdGenre(String dvdGenre) {
        this.dvdGenre = dvdGenre;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        DVD dvd = (DVD) o;
        return Objects.equals(getProductId(), dvd.getProductId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getProductId());
    }
    
    @Override
    public String toString() {
         return "DVD{" +
               "productId='" + getProductId() + '\'' +
               ", title='" + getTitle() + '\'' +
               ", director='" + director + '\'' +
               '}';
    }
}