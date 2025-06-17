package Project_ITSS.PlaceOrder.Repository;

//import Project_ITSS.demo.Entity.Product;
import Project_ITSS.PlaceOrder.Entity.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProductRepository_PlaceOrder {


    private JdbcTemplate jdbcTemplate = new JdbcTemplate();

    public int getProductQuantity(Long product_id) {
        return jdbcTemplate.queryForObject("SELECT quantity FROM Product WHERE product_id = ?", new Object[]{product_id}, Integer.class);
    }

    public int getProductPrice(long product_id){
        return jdbcTemplate.queryForObject("SELECT price FROM Product WHERE product_id = ?",new Object[]{product_id}, Integer.class);
    }

    public double getProductWeight(long product_id){
        return jdbcTemplate.queryForObject("SELECT weight FROM Product WHERE product_id = ?",new Object[]{product_id}, Double.class);
    }

    public Product getProductById(long product_id){
        String sql = "SELECT * FROM product WHERE product_id = ?";
        return jdbcTemplate.queryForObject(sql,new Object[]{product_id},new BeanPropertyRowMapper<>(Product.class));
    }
}
