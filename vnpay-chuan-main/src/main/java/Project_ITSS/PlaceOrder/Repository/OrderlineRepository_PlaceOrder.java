package Project_ITSS.PlaceOrder.Repository;

import Project_ITSS.PlaceOrder.Entity.Orderline;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class OrderlineRepository_PlaceOrder {
    private JdbcTemplate jdbcTemplate;

    public Orderline getOrderlinebyId(long odrline_id){
        String sql = "SELECT * FROM OrderLines WHERE odrline_id = ?";
        return jdbcTemplate.queryForObject(sql,new Object[]{odrline_id},new BeanPropertyRowMapper<>(Orderline.class));
    }

    public List<Orderline> getOrderLinebyOrderId(long order_id){
        String sql = "SELECT * FROM OrderLines WHERE order_id = ?";
        return jdbcTemplate.query(sql,new Object[]{order_id},new BeanPropertyRowMapper<>(Orderline.class));
    }

}
