package Project_ITSS.ViewProduct2.Service;

import Project_ITSS.ViewProduct2.Repository.UserRepository_ViewProduct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService_ViewProduct {
    @Autowired
    private UserRepository_ViewProduct userRepository;
    public boolean checkUserRole(int userId)
    {
        String role = userRepository.verifyUserRole(userId);
        return "ProductManager".equalsIgnoreCase(role);
    }

    public boolean verifyUserRole(String role) {
        return role.equalsIgnoreCase("ProductManager");
    }
}
