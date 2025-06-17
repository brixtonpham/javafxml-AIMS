package Project_ITSS.PlaceOrder.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

enum Province{
    HaNoi,
    HoChiMinhCity,
    HaiPhong,
    ThanhHoa,
    NgheAn,
    HungYen,
    DaNang,
    Hue,
    NhaTrang
}

enum PaymentMethod{
    CreditCard
}


@Service
public class NonDBService_PlaceOrder {
    @Autowired
    private JavaMailSender javaMailSender;
    public void sendSuccessNotification(String customer, String message) {
        // TODO: Gửi email, SMS, hoặc notification khác
        System.out.println("Notification to " + customer + ": " + message);
    }

    // Kiểm tra độ hợp lý của thông tin được nộp lên
    public boolean CheckInfoValidity(String name, String phone, String email, String address, String province,String paymentMethod){
        if(phone.length() > 10) return false;
        if(!name.matches("^[\\p{L}]{3,50}$")) return false;
        if(!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) return false;
        try{
            Province.valueOf(province);
        }catch (Exception e){
            return false;
        }
        try{
            PaymentMethod.valueOf(paymentMethod);
        }catch(Exception e){
            return false;
        }
        return true;
    }

    public void SendSuccessEmail(String toEmail,String subject,String content){
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(toEmail);
        message.setSubject(subject);
        message.setText(content);
        javaMailSender.send(message);
    }
} 