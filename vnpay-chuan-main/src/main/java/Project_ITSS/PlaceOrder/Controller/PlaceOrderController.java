package Project_ITSS.PlaceOrder.Controller;

import Project_ITSS.PlaceOrder.Entity.*;
import Project_ITSS.PlaceOrder.Repository.ProductRepository_PlaceOrder;
import Project_ITSS.PlaceOrder.Service.NonDBService_PlaceOrder;
import Project_ITSS.PlaceOrder.Service.OrderService_PlaceOrder;
import Project_ITSS.PlaceOrder.Service.ProductService_PlaceOrder;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;


import java.util.HashMap;
import java.util.Map;


@RequestMapping("/")
@RestController
@NoArgsConstructor
public class PlaceOrderController {
    @Autowired
    private ProductService_PlaceOrder productService;
    @Autowired
    private ProductRepository_PlaceOrder productRepository;
    @Autowired
    private JavaMailSender mailSender;
    @Autowired
    private OrderService_PlaceOrder orderService;
    @Autowired
    private NonDBService_PlaceOrder nonDBService;

    @GetMapping("/test")
    public void JustForFun(){
        System.out.println(100);
        return;
    }

    @PostMapping("/placeorder")
    // Yêu cầu việc đặt hàng và kiểm tra số lượng sản phẩm trong cart liệu có phù hợp
    public Object RequestToPlaceOrder(@RequestBody Cart cart){
        // Kiểm tra số lựong product trong cart có đủ để bán không
        System.out.println(1);
        Map<String, Object> json = new HashMap<>();
        for(CartItem cartItem : cart.getProducts()){
            Product product = cartItem.getProduct();
            int quantity = cartItem.getQuantity();
            boolean result = productService.checkProductValidity(quantity,product.getProduct_id());
            if(!result){
                json.put("message","The number of products is inadequate, please select again");
                return json;
            }
        }
        Order order = new Order();    // Tạo một entity là order
        order.createOrder(cart);      // Điền thông tin các thuộc tính cho order, dựa trên thông tin của cart
        return order;                 // trả về entity order
    }

    // Sau khi người dùng điền thông tin cá nhân, các thông tin sẽ được đưa vào trong hàm này để xử lý tiếp
    @PostMapping("/deliveryinfo")
    public Map<String, Object> SubmitDeliveryInformation(@RequestParam String name,@RequestParam String phone,@RequestParam String email,@RequestParam String address,@RequestParam String province,@RequestParam String payMethod,@RequestParam String delivery_message,@RequestBody Order order){
        // Kiểm tra tính hợp lệ của các thông tin được nhập vào
//        boolean result = nonDBService.CheckInfoValidity(name,phone,email,address,province,payMethod);
        Map<String, Object> json = new HashMap<>();
        // Nếu thông tin không hợp lệ
        // Từ đoạn này có thể merge với RushOrder, nhưng tôi chưa biết merge thế nào
//        if(!result){
//            json.put("message","Your provided information is invalid, please select again");
//            return json;
//        }

        DeliveryInformation deliveryInformation = new DeliveryInformation();                        // Tạo entity deliveryInfo
        deliveryInformation.createDeliveryInfo(name,phone,email,address,province,delivery_message); // Điền thông tin vào entity đó
        int[] deliveryfees = orderService.CalculateDeliveryFee(province,order);
        int deliveryfee = deliveryfees[0] + deliveryfees[1];   // Tính toán giá tiền phải nộp
        System.out.println(deliveryfees[0]);
        System.out.println(deliveryfees[1]);
        deliveryInformation.setDelivery_fee(deliveryfee);
        Invoice invoice = new Invoice();                                                            // Tạo entity về Invoice
        invoice.CreateInvoice(order.getOrder_id(),"Your total delivery fee is " + String.valueOf(deliveryfee) + " and your total amount needed to be paid is " + String.valueOf(order.getTotal_after_VAT()));
        json.put("Invoice_ìnfo",invoice);
        json.put("delivery fee",deliveryfee);
        json.put("order information",order);
        return json;                                // Trả lại thông tin về invoice lẫn chi phí vận chuyển
    }

    // Sau khi thanh toán xong (Tức sau khi kết thúc PayOrder UseCase) , gọi đến hàm này để xử lý nốt các công đoạn cuối
    public void FinishPlaceOrder(Cart cart,DeliveryInformation deliveryInformation,Order order){
        cart.EmptyCart();                                //   Dọn sạch giỏ hàng sau khi thanh toán xong
        order.saveOrder(order,deliveryInformation);      //   Lưu lại thông tin order trong database
        nonDBService.SendSuccessEmail(deliveryInformation.getEmail(),"Thông báo về việc đặt hàng","Bạn đã đật hàng thành công !!!");  // Gửi email thông báo
        // khi gửi email , cần gửi một mã order để xử lý refund nếu có.
    }
}
