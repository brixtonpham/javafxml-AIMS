package Project_ITSS.UpdateProduct.Repository;


import Project_ITSS.UpdateProduct.Entity.DVD;
import Project_ITSS.UpdateProduct.Entity.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public class DVDRepository_UpdateProduct implements DetailProductRepository_UpdateProduct {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void updateProductInfo(Product product){
        DVD dvd = (DVD)product;
        String importDateStr = dvd.getReleaseDate(); // ví dụ "2023-05-30"
        LocalDate localDate = LocalDate.parse(importDateStr);
        java.sql.Date sqlDate = java.sql.Date.valueOf(localDate);
        jdbcTemplate.update("UPDATE DVD " +
                        "SET title = ?, " +
                        "product_id = ?, " +
                        "    release_Date = ?, " +
                        "    DVD_type = ?, " +
                        "    genre = ?, " +
                        "    studio = ?, " +
                        "    director = ? " +
                        "WHERE DVD_id = ?",
                dvd.getTitle(),
                dvd.getProduct_id(),
                sqlDate,
                dvd.getDVD_type(),
                dvd.getGenre(),
                dvd.getStudio(),
                dvd.getDirectors(),
                dvd.getDVD_id());
    }

    @Override
    public String getType() {
        return "dvd";
    }
}