package Project_ITSS.UpdateProduct.Repository;


import Project_ITSS.UpdateProduct.Entity.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public class ProductRepository_UpdateProduct {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void updateProductInfo(Product product){
        try{
            String importDateStr = product.getImport_date(); // ví dụ "2023-05-30"
            LocalDate localDate = LocalDate.parse(importDateStr);
            java.sql.Date sqlDate = java.sql.Date.valueOf(localDate);
            jdbcTemplate.update(    "UPDATE Product " +       // <-- thêm dấu cách sau 'Product'
                            "SET " +                  // <-- thêm dấu cách sau 'SET'
                            "title = ?, " +
                            "price = ?, " +
                            "weight = ?, " +
                            "rush_order_supported = ?, " +
                            "image_url = ?, " +
                            "barcode = ?, " +
                            "import_date = ?, " +
                            "introduction = ?, " +
                            "quantity = ? " +
                            "WHERE product_id = ?",
                    product.getTitle(),
                    product.getPrice(),
                    product.getWeight(),
                    product.isRush_order_supported(),
                    product.getImage_url(),
                    product.getBarcode(),
                    sqlDate,
                    product.getIntroduction(),
                    product.getQuantity(),
                    product.getProduct_id());
        }catch (Exception e){
             e.printStackTrace();
        }


    }
}




