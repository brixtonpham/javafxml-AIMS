package UnitTest;

import Project_ITSS.PlaceOrder.Controller.PlaceOrderController;
import Project_ITSS.PlaceOrder.Entity.DeliveryInformation;
import Project_ITSS.PlaceOrder.Entity.Order;
import Project_ITSS.PlaceOrder.Entity.Orderline;
import Project_ITSS.PlaceOrder.Repository.OrderlineRepository_PlaceOrder;
import Project_ITSS.PlaceOrder.Repository.OrderRepository_PlaceOrder;
import Project_ITSS.PlaceOrder.Service.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PlaceOrderControllerTest {
    private PlaceOrderController placeOrderController;
    private OrderRepository_PlaceOrder orderRepository;
    private OrderlineRepository_PlaceOrder orderlineRepository;
    private OrderService_PlaceOrder orderService;

    @Test
    public void CheckInfoValidity1(){
        assertTrue(placeOrderController.CheckInfoValidity("John", "0987654321", "john@gmail.com", "123 ABC", "HANOI", "CASH"));
    }

    @Test
    public void CheckInfoValidity2(){
        assertFalse(placeOrderController.CheckInfoValidity("Anna", "012345678901", "anna@gmail.com", "123 ABC", "HANOI", "CASH"));
    }

    @Test
    public void CheckInfoValidity3(){
        assertFalse(placeOrderController.CheckInfoValidity("Anna", "0987654321", "anna.gmail.com", "123 ABC", "HANOI", "CASH"));
    }

    @Test
    public void CheckInfoValidity4(){
        assertFalse(placeOrderController.CheckInfoValidity("Anna", "0987654321", "anna@gmail.com", "123 ABC", "SAIGON", "CASH"));
    }

    @Test
    public void CheckInfoValidity5(){
        assertTrue(placeOrderController.CheckInfoValidity("Trần An", "0987654321", "an@gmail.com", "123 ABC", "DaNang", "CreditCard"));
    }

//    @Test
//    public void CalculateDeliveryFee1(){
//        DeliveryInformation deliveryInformation = new DeliveryInformation();
//        deliveryInformation.setProvince("");
//        Order order = new Order();
//        List<Orderline> Orderlist = orderlineRepository.getOrderLinebyOrderId(1);
//        order.setOrderlineList(Orderlist);
//        int result = placeOrderController.CalculateDeliveryFee(deliveryInformation,order);
//        assertEquals("100000",result);
//
//    }
//
//    @Test
//    public void CalculateDeliveryFee2(){
//        DeliveryInformation deliveryInformation = new DeliveryInformation();
//        deliveryInformation.setProvince("");
//        Order order = new Order();
//        List<Orderline> Orderlist = orderlineRepository.getOrderLinebyOrderId(2);
//        order.setOrderlineList(Orderlist);
//        int result = placeOrderController.CalculateDeliveryFee(deliveryInformation,order);
//        assertEquals("65000",result);
//    }
//
//    @Test
//    public void CalculateDeliveryFee3(){
//        DeliveryInformation deliveryInformation = new DeliveryInformation();
//        deliveryInformation.setProvince("");
//        Order order = new Order();
//        List<Orderline> Orderlist = orderlineRepository.getOrderLinebyOrderId(3);
//        order.setOrderlineList(Orderlist);
//        int result = placeOrderController.CalculateDeliveryFee(deliveryInformation,order);
//        assertEquals("67000",result);
//    }
//
//    @Test
//    public void CalculateDeliveryFee4(){
//        DeliveryInformation deliveryInformation = new DeliveryInformation();
//        deliveryInformation.setProvince("");
//        Order order = new Order();
//        List<Orderline> Orderlist = orderlineRepository.getOrderLinebyOrderId(4);
//        order.setOrderlineList(Orderlist);
//        int result = placeOrderController.CalculateDeliveryFee(deliveryInformation,order);
//        assertEquals("25000",result);
//    }
}
