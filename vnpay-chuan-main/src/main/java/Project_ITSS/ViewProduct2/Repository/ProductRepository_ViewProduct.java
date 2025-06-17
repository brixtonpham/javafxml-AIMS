package Project_ITSS.ViewProduct2.Repository;


import Project_ITSS.ViewProduct2.Entity.Product;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public  class ProductRepository_ViewProduct {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public int getProductQuantity(long product_id){
        return jdbcTemplate.queryForObject("SELECT quantity FROM Product WHERE product_id = ?", new Object[]{product_id}, Integer.class);
    }

    public int getProductPrice(long product_id){
        return jdbcTemplate.queryForObject("SELECT price FROM Product WHERE product_id = ?",new Object[]{product_id}, Integer.class);
    }

    public double getProductWeight(long product_id){
        return jdbcTemplate.queryForObject("SELECT weight FROM Product WHERE product_id = ?",new Object[]{product_id}, Double.class);
    }


    public Product findById(long id) {
        String sql = "SELECT * FROM product WHERE product_id = ?";
        return jdbcTemplate.queryForObject(sql, new Object[]{id}, new ProductRowMapper());
    }

    public List<Product> getAllProduct(){
        try {
            String sql = "SELECT * FROM product";
            return jdbcTemplate.query(sql, new ProductRowMapper());
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    



}