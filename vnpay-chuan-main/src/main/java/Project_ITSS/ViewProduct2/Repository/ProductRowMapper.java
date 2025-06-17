package Project_ITSS.ViewProduct2.Repository;

import org.springframework.jdbc.core.RowMapper;
import java.sql.ResultSet;
import java.sql.SQLException;

import Project_ITSS.ViewProduct2.Entity.Product;

public class ProductRowMapper implements RowMapper<Product> {
        @Override
    public Product mapRow(ResultSet rs, int rowNum) throws SQLException {
        Product product = new Product();
        product.setProduct_id(rs.getLong("product_id"));
        product.setTitle(rs.getString("title"));
        product.setPrice(rs.getInt("price"));
        product.setWeight(rs.getFloat("weight"));
        product.setRush_order_supported(rs.getBoolean("rush_order_supported"));
        product.setImage_url(rs.getString("image_url"));
        product.setBarcode(rs.getString("barcode"));
        product.setImport_date(rs.getString("import_date"));
        product.setIntroduction(rs.getString("introduction"));
        product.setQuantity(rs.getInt("quantity"));
        return product;
    }
    
}
