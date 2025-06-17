package Project_ITSS.PlaceOrder.Entity;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DeliveryInformation {
     private Long delivery_id;
     private String name;
     private String phone;
     private String email;
     private String address;
     private String province;
     private String delivery_message;
     private int delivery_fee;

     public void createDeliveryInfo(String name, String phone, String email, String address, String province, String delivery_message){
          this.name = name;
          this.phone = phone;
          this.email = email;
          this.address = address;
          this.province = province;
          this.delivery_message = delivery_message;
     }
}
