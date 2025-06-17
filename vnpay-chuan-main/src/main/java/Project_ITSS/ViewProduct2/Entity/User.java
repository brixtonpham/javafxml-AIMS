package Project_ITSS.ViewProduct2.Entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class User {
    private int user_id;
    private int password;
    private String name;
    private String email;
    private String phone;
    private String role;
    private String registration_date;
    private double salary;
}
