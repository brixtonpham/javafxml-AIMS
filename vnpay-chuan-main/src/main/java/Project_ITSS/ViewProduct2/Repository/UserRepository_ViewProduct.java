package Project_ITSS.ViewProduct2.Repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository_ViewProduct {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public String verifyUserRole(int user_id) {
        String sql = "SELECT role FROM User WHERE user_id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, new Object[]{user_id}, String.class);
        } catch (Exception e) {
            return "NOT OK"; // user_id không tồn tại hoặc lỗi
        }
    }
}
