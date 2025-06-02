package com.aims.core.application.dtos;

import com.aims.core.enums.ProductType;
import java.time.LocalDate;

// Using a class for potential future flexibility, records are also a good option.
public class ProductDTO {
    private String productId; // Nullable for creation
    private String title;
    private String category;
    private float valueAmount; // Price without VAT
    private float price;       // Price without VAT
    private int quantityInStock;
    private String description;
    private String imageUrl;
    private String barcode;
    private String dimensionsCm;
    private float weightKg;
    private ProductType productType; // To determine which specific fields below are relevant

    // Book specific
    private String authors;
    private String coverType;
    private String publisher;
    private LocalDate publicationDate;
    private Integer numPages; // Use Integer to allow null if not applicable/provided
    private String language;
    private String bookGenre;

    // CD specific
    private String artists;
    private String recordLabel;
    private String tracklist;
    private String cdGenre;
    private LocalDate releaseDateCD; // Renamed to avoid clash if ProductDTO becomes very generic

    // DVD specific
    private String discType;
    private String director;
    private Integer runtimeMinutes; // Use Integer to allow null
    private String studio;
    private String dvdLanguage;
    private String subtitles;
    private LocalDate releaseDateDVD; // Renamed
    private String dvdGenre;

    // AudioBook specific (future)
    private String audiobookAuthor;
    private String format;
    private String audiobookLanguage;
    private String accent;
    private Integer lengthInMinutes;


    public ProductDTO() {
    }

    // Getters and Setters for all fields
    // --- Common Product Fields ---
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public float getValueAmount() { return valueAmount; }
    public void setValueAmount(float valueAmount) { this.valueAmount = valueAmount; }

    public float getPrice() { return price; }
    public void setPrice(float price) { this.price = price; }

    public int getQuantityInStock() { return quantityInStock; }
    public void setQuantityInStock(int quantityInStock) { this.quantityInStock = quantityInStock; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public String getDimensionsCm() { return dimensionsCm; }
    public void setDimensionsCm(String dimensionsCm) { this.dimensionsCm = dimensionsCm; }

    public float getWeightKg() { return weightKg; }
    public void setWeightKg(float weightKg) { this.weightKg = weightKg; }

    public ProductType getProductType() { return productType; }
    public void setProductType(ProductType productType) { this.productType = productType; }

    // --- Book Specific Fields ---
    public String getAuthors() { return authors; }
    public void setAuthors(String authors) { this.authors = authors; }

    public String getCoverType() { return coverType; }
    public void setCoverType(String coverType) { this.coverType = coverType; }

    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }

    public LocalDate getPublicationDate() { return publicationDate; }
    public void setPublicationDate(LocalDate publicationDate) { this.publicationDate = publicationDate; }

    public Integer getNumPages() { return numPages; }
    public void setNumPages(Integer numPages) { this.numPages = numPages; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getBookGenre() { return bookGenre; }
    public void setBookGenre(String bookGenre) { this.bookGenre = bookGenre; }

    // --- CD Specific Fields ---
    public String getArtists() { return artists; }
    public void setArtists(String artists) { this.artists = artists; }

    public String getRecordLabel() { return recordLabel; }
    public void setRecordLabel(String recordLabel) { this.recordLabel = recordLabel; }

    public String getTracklist() { return tracklist; }
    public void setTracklist(String tracklist) { this.tracklist = tracklist; }

    public String getCdGenre() { return cdGenre; }
    public void setCdGenre(String cdGenre) { this.cdGenre = cdGenre; }

    public LocalDate getReleaseDateCD() { return releaseDateCD; }
    public void setReleaseDateCD(LocalDate releaseDateCD) { this.releaseDateCD = releaseDateCD; }

    // --- DVD Specific Fields ---
    public String getDiscType() { return discType; }
    public void setDiscType(String discType) { this.discType = discType; }

    public String getDirector() { return director; }
    public void setDirector(String director) { this.director = director; }

    public Integer getRuntimeMinutes() { return runtimeMinutes; }
    public void setRuntimeMinutes(Integer runtimeMinutes) { this.runtimeMinutes = runtimeMinutes; }

    public String getStudio() { return studio; }
    public void setStudio(String studio) { this.studio = studio; }

    public String getDvdLanguage() { return dvdLanguage; }
    public void setDvdLanguage(String dvdLanguage) { this.dvdLanguage = dvdLanguage; }

    public String getSubtitles() { return subtitles; }
    public void setSubtitles(String subtitles) { this.subtitles = subtitles; }

    public LocalDate getReleaseDateDVD() { return releaseDateDVD; }
    public void setReleaseDateDVD(LocalDate releaseDateDVD) { this.releaseDateDVD = releaseDateDVD; }

    public String getDvdGenre() { return dvdGenre; }
    public void setDvdGenre(String dvdGenre) { this.dvdGenre = dvdGenre; }

    // --- AudioBook Specific Fields ---
    public String getAudiobookAuthor() { return audiobookAuthor; }
    public void setAudiobookAuthor(String audiobookAuthor) { this.audiobookAuthor = audiobookAuthor; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public String getAudiobookLanguage() { return audiobookLanguage; }
    public void setAudiobookLanguage(String audiobookLanguage) { this.audiobookLanguage = audiobookLanguage; }

    public String getAccent() { return accent; }
    public void setAccent(String accent) { this.accent = accent; }

    public Integer getLengthInMinutes() { return lengthInMinutes; }
    public void setLengthInMinutes(Integer lengthInMinutes) { this.lengthInMinutes = lengthInMinutes; }
}