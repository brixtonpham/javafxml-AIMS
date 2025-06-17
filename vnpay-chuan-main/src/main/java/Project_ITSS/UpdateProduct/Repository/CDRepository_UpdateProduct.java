package Project_ITSS.UpdateProduct.Repository;


import Project_ITSS.UpdateProduct.Entity.CD;
import Project_ITSS.UpdateProduct.Entity.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public class CDRepository_UpdateProduct implements  DetailProductRepository_UpdateProduct{
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void updateProductInfo(Product product){
        CD cd = (CD)product;
        String importDateStr = cd.getReleaseDate(); // ví dụ "2023-05-30"
        LocalDate localDate = LocalDate.parse(importDateStr);
        java.sql.Date sqlDate = java.sql.Date.valueOf(localDate);
        jdbcTemplate.update(    "UPDATE CD " +                      // ← dấu cách sau CD
                        "SET " +                            // ← dấu cách sau SET
                        "Product_id = ?, " +
                        "Track_List = ?, " +
                        "genre = ?, " +
                        "recordLabel = ?, " +
                        "artists = ?, " +
                        "releaseDate = ? " +               // ← dấu cách ở cuối dòng này
                        "WHERE CD_id = ?;",
                cd.getProduct_id(),
                cd.getTrackList(),
                cd.getGenre(),
                cd.getRecordLabel(),
                cd.getArtists(),
                sqlDate,
                cd.getCD_id());
    }

    @Override
    public String getType() {
        return "cd";
    }


}
