package com.aims.core.shared;

import com.aims.core.application.impl.*;
import com.aims.core.application.services.*;
import com.aims.core.infrastructure.database.dao.*;
// Stub implementations for external service adapters
import com.aims.core.infrastructure.adapters.external.email.IEmailSenderAdapter;
import com.aims.core.infrastructure.adapters.external.email.StubEmailSenderAdapter;
import com.aims.core.infrastructure.adapters.external.payment_gateway.IVNPayAdapter;
import com.aims.core.infrastructure.adapters.external.payment_gateway.StubVNPayAdapter;

/**
 * Simple Service Factory for dependency injection.
 * This class handles the creation and wiring of all dependencies in the AIMS application.
 * In a production environment, this could be replaced with a DI framework like Spring.
 */
public class ServiceFactory {
    
    // Singleton instance
    private static ServiceFactory instance;
    
    // DAOs
    private IProductDAO productDAO;
    private IUserAccountDAO userAccountDAO;
    private IRoleDAO roleDAO;
    private IUserRoleAssignmentDAO userRoleAssignmentDAO;
    private ICartItemDAO cartItemDAO;
    private ICartDAO cartDAO;
    private IOrderItemDAO orderItemDAO;
    private IOrderEntityDAO orderEntityDAO;
    private IDeliveryInfoDAO deliveryInfoDAO;
    private IInvoiceDAO invoiceDAO;
    private IPaymentMethodDAO paymentMethodDAO;
    private IPaymentTransactionDAO paymentTransactionDAO;
    private ICardDetailsDAO cardDetailsDAO;
    private IProductManagerAuditDAO productManagerAuditDAO;
    
    // External Service Adapters
    private IVNPayAdapter vnPayAdapter;
    private IEmailSenderAdapter emailSenderAdapter;
    
    // Services
    private IProductService productService;
    private IAuthenticationService authenticationService;
    private IUserAccountService userAccountService;
    private ICartService cartService;
    private IDeliveryCalculationService deliveryCalculationService;
    private INotificationService notificationService;
    private IPaymentService paymentService;
    private IOrderService orderService;
    private IProductManagerAuditService productManagerAuditService;
    
    private ServiceFactory() {
        initializeDependencies();
    }
    
    public static synchronized ServiceFactory getInstance() {
        if (instance == null) {
            instance = new ServiceFactory();
        }
        return instance;
    }
    
    private void initializeDependencies() {
        // Initialize DAOs (simple ones first, complex ones with dependencies later)
        productDAO = new ProductDAOImpl();
        userAccountDAO = new UserAccountDAOImpl();
        roleDAO = new RoleDAOImpl();
        deliveryInfoDAO = new DeliveryInfoDAOImpl();
        invoiceDAO = new InvoiceDAOImpl();
        cardDetailsDAO = new CardDetailsDAOImpl();
        productManagerAuditDAO = new ProductManagerAuditDAOImpl();
        
        // DAOs with dependencies
        userRoleAssignmentDAO = new UserRoleAssignmentDAOImpl();
        cartItemDAO = new CartItemDAOImpl(productDAO);
        cartDAO = new CartDAOImpl(cartItemDAO, productDAO, userAccountDAO);
        orderItemDAO = new OrderItemDAOImpl(productDAO);
        orderEntityDAO = new OrderEntityDAOImpl(orderItemDAO, userAccountDAO);
        paymentMethodDAO = new PaymentMethodDAOImpl(userAccountDAO, cardDetailsDAO);
        paymentTransactionDAO = new PaymentTransactionDAOImpl(orderEntityDAO, paymentMethodDAO);
        
        // External Service Adapters (using stub implementations)
        vnPayAdapter = new StubVNPayAdapter();
        emailSenderAdapter = new StubEmailSenderAdapter();
        
        // Initialize audit service before ProductService
        productManagerAuditService = new ProductManagerAuditServiceImpl(productManagerAuditDAO);
        
        // Services (ProductService needs audit service)
        productService = new ProductServiceImpl(productDAO, productManagerAuditService);
        authenticationService = new AuthenticationServiceImpl(userAccountDAO, userRoleAssignmentDAO);
        cartService = new CartServiceImpl(cartDAO, cartItemDAO, productDAO, userAccountDAO);
        deliveryCalculationService = new DeliveryCalculationServiceImpl();
        
        // External services with stub adapters
        notificationService = new NotificationServiceImpl(emailSenderAdapter);
        paymentService = new PaymentServiceImpl(paymentTransactionDAO, paymentMethodDAO, cardDetailsDAO, vnPayAdapter);
        
        // Services with many dependencies
        userAccountService = new UserAccountServiceImpl(userAccountDAO, roleDAO, userRoleAssignmentDAO, notificationService);
        orderService = new OrderServiceImpl(
            orderEntityDAO,
            orderItemDAO,
            deliveryInfoDAO,
            invoiceDAO,
            productDAO,
            productService,
            cartService,
            paymentService,
            deliveryCalculationService,
            notificationService,
            userAccountDAO
        );
    }
    
    // Getters for Services
    public IProductService getProductService() {
        return productService;
    }
    
    public IAuthenticationService getAuthenticationService() {
        return authenticationService;
    }
    
    public IUserAccountService getUserAccountService() {
        return userAccountService;
    }
    
    public ICartService getCartService() {
        return cartService;
    }
    
    public IOrderService getOrderService() {
        return orderService;
    }
    
    public IPaymentService getPaymentService() {
        return paymentService;
    }
    
    public INotificationService getNotificationService() {
        return notificationService;
    }
    
    public IDeliveryCalculationService getDeliveryCalculationService() {
        return deliveryCalculationService;
    }
    
    // Getters for DAOs (if needed by controllers)
    public IProductDAO getProductDAO() {
        return productDAO;
    }
    
    public IUserAccountDAO getUserAccountDAO() {
        return userAccountDAO;
    }
    
    public ICartDAO getCartDAO() {
        return cartDAO;
    }
    
    public IOrderEntityDAO getOrderEntityDAO() {
        return orderEntityDAO;
    }
    
    // Getters for External Adapters (if needed)
    public IVNPayAdapter getVNPayAdapter() {
        return vnPayAdapter;
    }

    public IEmailSenderAdapter getEmailSenderAdapter() {
        return emailSenderAdapter;
    }
    
    public IProductManagerAuditService getProductManagerAuditService() {
        return productManagerAuditService;
    }
}
