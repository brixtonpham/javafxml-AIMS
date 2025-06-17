package Project_ITSS.PlaceOrder.Controller;

import Project_ITSS.PlaceOrder.Entity.DeliveryInfo;
import Project_ITSS.PlaceOrder.Entity.DeliveryInformation;
import Project_ITSS.PlaceOrder.Entity.Invoice;
import Project_ITSS.PlaceOrder.Entity.Order;
import Project_ITSS.PlaceOrder.Service.NonDBService_PlaceOrder;
import Project_ITSS.PlaceOrder.Service.OrderService_PlaceOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/rush-order")
public class RushOrderController {

    @Autowired
    private NonDBService_PlaceOrder nonDBService;
    @Autowired
    private OrderService_PlaceOrder orderService;

    // 1. Customer chọn rush order
    @GetMapping("/select")
    public String selectRushOrder(Model model) {
        // Chuyển sang form nhập thông tin giao hàng nhanh
        return "rush-delivery-form";
    }

    // 2. Kiểm tra tính hợp lệ điểm đến
//    @PostMapping("/check-validity")
//    @ResponseBody
//    public boolean checkValidity(@RequestParam String destination) {
//        // Chỉ cho phép giao hàng nhanh đến các thành phố lớn
//        return destination != null && (
//            destination.equalsIgnoreCase("HaNoi") ||
//            destination.equalsIgnoreCase("HoChiMinhCity") ||
//            destination.equalsIgnoreCase("DaNang")
//        );
//    }

    // 3. Nhận thông tin giao hàng, tạo DeliveryInfo
//    @PostMapping("/submit-info")
//    public String submitInfo(@ModelAttribute DeliveryInfo deliveryInfo,@ModelAttribute Order order, Model model) {
//        if (!checkValidity(deliveryInfo.getDestination())) {
//            model.addAttribute("error", "Destination is not eligible for rush delivery!");
//            return "rush-delivery-form";
//        }
//        // Tính phí giao hàng nhanh (ví dụ: 50k + 10k mỗi kg)
//        int rushFee = 50000 + (int)(deliveryInfo.getWeight() * 10000);
//        deliveryInfo.setTotalPaid(rushFee);
//        // Lưu DeliveryInfo (có thể lưu vào DB hoặc session)
//        model.addAttribute("deliveryInfo", deliveryInfo);
//        // Gửi thông báo thành công
//        model.addAttribute("message", "Rush order placed successfully!");
//        return "rush-delivery-success";
//    }

    @PostMapping("/Rushdeliveryinfo")
    public Map<String, Object> SubmitDeliveryInformation(@RequestParam String name, @RequestParam String phone, @RequestParam String email, @RequestParam String address, @RequestParam String province, @RequestParam String payMethod, @RequestParam String delivery_message, @RequestBody Order order){
        // Kiểm tra tính hợp lệ của các thông tin được nhập vào
        boolean result = nonDBService.CheckInfoValidity(name,phone,email,address,province,payMethod);
        Map<String, Object> json = new HashMap<>();
        // Nếu thông tin không hợp lệ
        // Từ đoạn này có thể merge với RushOrder, nhưng tôi chưa biết merge thế nào
        if(!result){
            json.put("message","Your provided information is invalid, please select again");
            return json;
        }
        DeliveryInformation deliveryInformation = new DeliveryInformation();                        // Tạo entity deliveryInfo
        deliveryInformation.createDeliveryInfo(name,phone,email,address,province,delivery_message); // Điền thông tin vào entity đó
        int[] deliveryfees = orderService.CalculateDeliveryFee(province,order);
        int deliveryfee = deliveryfees[0] + deliveryfees[1];                                        // Tính toán giá tiền phải nộp
        deliveryInformation.setDelivery_fee(deliveryfee);
        Invoice invoice = new Invoice();                                                            // Tạo entity về Invoice
        invoice.CreateInvoice(order.getOrder_id(),"Your total delivery fee is " + String.valueOf(deliveryfee) + " and your total amount needed to be paid is " + String.valueOf(order.getTotal_after_VAT()));
        json.put("Invoice_ìnfo",invoice);
        json.put("total",deliveryfee);
        return json;                                // Trả lại thông tin về invoice lẫn chi phí vận chuyển
    }

    // 4. Tính lại phí giao hàng nếu có thay đổi
    @PostMapping("/recalculate")
    @ResponseBody
    public int[] recalculate(String province,@RequestBody Order order) {
        // Phí = 50k + 10k mỗi kg
        int[] deliveryfees = orderService.CalculateDeliveryFee(province,order);
        int deliveryfee = deliveryfees[0] + deliveryfees[1];
        return deliveryfees;
    }

    // 5. Thông báo lỗi
    @GetMapping("/notify-error")
    public String notifyError(Model model) {
        model.addAttribute("error", "Rush order delivery is not eligible for this destination.");
        return "rush-delivery-form";
    }
} 