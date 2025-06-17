package Project_ITSS.PlaceOrder.Repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class InvoiceRepository_PlaceOrder {

    @Autowired
    private JdbcTemplate jdbcTemplate;
}
