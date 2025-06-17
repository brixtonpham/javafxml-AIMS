package Project_ITSS.PlaceOrder.Repository;

import Project_ITSS.PlaceOrder.Entity.DeliveryInformation;
import Project_ITSS.PlaceOrder.Entity.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OrderRepository_PlaceOrder {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void saveOrder(Order order, DeliveryInformation dI){
        jdbcTemplate.update("INSERT INTO Order (Total_before_VAT,Total_after_VAT,status,VAT) VALUES (?,?,?,?)",
                order.getTotal_before_VAT(),
                order.getTotal_after_VAT(),
                order.getStatus(),
                order.getVAT());

        jdbcTemplate.update("INSERT INTO DeliveryInformation (Name, Phone, Email, Address, Province, Shipping_message, shipping_fee) VALUES (?, ?, ?, ?, ?, ?, ?)",
                dI.getName(),
                dI.getPhone(),
                dI.getEmail(),
                dI.getAddress(),
                dI.getProvince(),
                dI.getDelivery_message(),
                dI.getDelivery_fee());
    }

    public Order getOrderById(long order_id){
        String sql = "SELECT * FROM Order WHERE order_id = ?";
        return jdbcTemplate.queryForObject(sql,new Object[]{order_id},new BeanPropertyRowMapper<>(Order.class));
    }
}


