package Project_ITSS.AddProduct.Repository;


import Project_ITSS.AddProduct.Entity.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.time.LocalDate;

@Repository
public class ProductRepository_AddProduct {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public int insertProductInfo(Product product) {
        String importDateStr = product.getImport_date(); // ví dụ "2023-05-30"
        LocalDate localDate = LocalDate.parse(importDateStr);
        java.sql.Date sqlDate = java.sql.Date.valueOf(localDate);

        try{
            return jdbcTemplate.queryForObject(
                    "INSERT INTO product " +
                            "(title, price, weight, rush_order_supported, image_url, barcode, import_date, introduction, quantity) " +
                            "VALUES (?, ?, ?, ?, ?,?,?, ?, ?) " +
                            "RETURNING product_id",
                    Integer.class,
                    product.getTitle(),
                    product.getPrice(),
                    product.getWeight(),
                    product.isRush_order_supported(),
                    product.getImage_url(),
                    product.getBarcode(),
                    sqlDate,
                    product.getIntroduction(),
                    product.getQuantity()
            );
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }

    }

    public void updateProduct(Product product){
        jdbcTemplate.update("UPDATE Product " +
                        "SET " +
                        "    title = ?, " +
                        "    price = ?, " +
                        "    weight = ?, " +
                        "    rush_order_supported = ?, " +
                        "    image_url = ?, " +
                        "    barcode = ?, " +
                        "    import_date = ?, " +
                        "    introduction = ?, " +
                        "    quantity = ? " +
                        "WHERE " +
                        "    product_id = ?",
                product.getTitle(),
                product.getPrice(),
                product.getWeight(),
                product.isRush_order_supported(),
                product.getImage_url(),
                product.getBarcode(),
                product.getImport_date(),
                product.getIntroduction(),
                product.getQuantity(),
                product.getProduct_id());
    }
}