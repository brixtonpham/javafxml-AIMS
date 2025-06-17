package Project_ITSS.AddProduct.Repository;


import Project_ITSS.AddProduct.Entity.CD;
import Project_ITSS.AddProduct.Entity.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public class CDRepository_AddProduct implements  DetailProductRepository_AddProduct{
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void insertProductInfo(Product product){
         CD cd = (CD)product;
        String importDateStr = cd.getReleaseDate(); // ví dụ "2023-05-30"
        LocalDate localDate = LocalDate.parse(importDateStr);
        java.sql.Date sqlDate = java.sql.Date.valueOf(localDate);
         jdbcTemplate.update(
                 "INSERT INTO CD (" +
                 "    Product_id," +
                 "    Track_list," +
                 "    genre," +
                 "    recordLabel," +
                 "    artists," +
                 "    releaseDate" +
                 ") VALUES (" +
                 "    ?, ?, ?, ?, ?, ?" + ")",
                 cd.getProduct_id(),
                 cd.getTrackList(),
                 cd.getGenre(),
                 cd.getRecordLabel(),
                 cd.getArtists(),
                 sqlDate);
    }

    @Override
    public String getType() {
        return "cd";
    }


}
