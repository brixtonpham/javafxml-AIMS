package com.aims.core.shared;

import com.aims.core.application.impl.*;
import com.aims.core.application.services.*;
import com.aims.core.infrastructure.database.dao.*;
import com.aims.core.infrastructure.adapters.external.email.IEmailSenderAdapter;
import com.aims.core.infrastructure.adapters.external.email.StubEmailSenderAdapter;
import com.aims.core.infrastructure.adapters.external.payment_gateway.IPaymentGatewayAdapter;
import com.aims.core.infrastructure.adapters.external.payment_gateway.StubPaymentGatewayAdapter;

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
    private IPaymentGatewayAdapter paymentGatewayAdapter;
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
    private IOrderValidationService orderValidationService;
    private IOrderDataLoaderService orderDataLoaderService;
    private ICartDataValidationService cartDataValidationService;
    private IOrderDataValidationService orderDataValidationService;
    
    // Payment Flow Monitoring Utilities
    private com.aims.core.presentation.utils.OrderValidationStateManager orderValidationStateManager;
    private com.aims.core.presentation.utils.PaymentFlowLogger paymentFlowLogger;
    
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
        
        // External Service Adapters (using stub implementation for testing)
        paymentGatewayAdapter = new StubPaymentGatewayAdapter();
        emailSenderAdapter = new StubEmailSenderAdapter();
        
        // Initialize audit service before ProductService
        productManagerAuditService = new ProductManagerAuditServiceImpl(productManagerAuditDAO);
        
        // Initialize order validation service (needed by PaymentService)
        orderValidationService = new OrderValidationServiceImpl(
            orderEntityDAO,
            orderItemDAO,
            deliveryInfoDAO,
            productDAO,
            userAccountDAO,
            invoiceDAO,
            paymentTransactionDAO
        );
        
        // Services (ProductService needs audit service)
        productService = new ProductServiceImpl(productDAO, productManagerAuditService);
        authenticationService = new AuthenticationServiceImpl(userAccountDAO, userRoleAssignmentDAO);
        cartService = new CartServiceImpl(cartDAO, cartItemDAO, productDAO, userAccountDAO);
        deliveryCalculationService = new DeliveryCalculationServiceImpl();
        
        // External services with stub adapters
        notificationService = new NotificationServiceImpl(emailSenderAdapter);
        paymentService = new PaymentServiceImpl(paymentTransactionDAO, paymentMethodDAO, cardDetailsDAO, paymentGatewayAdapter, orderValidationService);
        
        // Initialize order data loader service first
        orderDataLoaderService = new OrderDataLoaderServiceImpl(
            orderEntityDAO,
            orderItemDAO,
            deliveryInfoDAO,
            invoiceDAO,
            paymentTransactionDAO,
            userAccountDAO,
            productDAO
        );
        
        // Initialize cart data validation service
        cartDataValidationService = new CartDataValidationServiceImpl(productDAO);
        
        // Initialize order data validation service
        orderDataValidationService = new OrderDataValidationServiceImpl(
            orderDataLoaderService,
            cartDataValidationService,
            deliveryCalculationService,
            productService
        );
        
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
            userAccountDAO,
            orderDataLoaderService
        );
        
        // Initialize payment flow monitoring utilities
        orderValidationStateManager = com.aims.core.presentation.utils.OrderValidationStateManager.getInstance();
        paymentFlowLogger = com.aims.core.presentation.utils.PaymentFlowLogger.getInstance();
    }
    
    // Static helper methods
    public static IProductService getProductService() {
        return getInstance().productService;
    }
    
    public static IAuthenticationService getAuthenticationService() {
        return getInstance().authenticationService;
    }
    
    public static IUserAccountService getUserAccountService() {
        return getInstance().userAccountService;
    }
    
    public static ICartService getCartService() {
        return getInstance().cartService;
    }
    
    public static IOrderService getOrderService() {
        return getInstance().orderService;
    }
    
    public static IPaymentService getPaymentService() {
        return getInstance().paymentService;
    }
    
    public static INotificationService getNotificationService() {
        return getInstance().notificationService;
    }
    
    public static IDeliveryCalculationService getDeliveryCalculationService() {
        return getInstance().deliveryCalculationService;
    }
    
    public static IProductDAO getProductDAO() {
        return getInstance().productDAO;
    }
    
    public static IUserAccountDAO getUserAccountDAO() {
        return getInstance().userAccountDAO;
    }
    
    public static ICartDAO getCartDAO() {
        return getInstance().cartDAO;
    }
    
    public static IOrderEntityDAO getOrderEntityDAO() {
        return getInstance().orderEntityDAO;
    }
    
    public static IPaymentMethodDAO getPaymentMethodDAO() {
        return getInstance().paymentMethodDAO;
    }
    
    public static ICardDetailsDAO getCardDetailsDAO() {
        return getInstance().cardDetailsDAO;
    }
    
    public static IPaymentTransactionDAO getPaymentTransactionDAO() {
        return getInstance().paymentTransactionDAO;
    }
    
    public static IPaymentGatewayAdapter getPaymentGatewayAdapter() {
        return getInstance().paymentGatewayAdapter;
    }

    public static IEmailSenderAdapter getEmailSenderAdapter() {
        return getInstance().emailSenderAdapter;
    }
    
    public static IProductManagerAuditService getProductManagerAuditService() {
        return getInstance().productManagerAuditService;
    }
    
    public static IOrderValidationService getOrderValidationService() {
        return getInstance().orderValidationService;
    }
    
    public static IOrderDataLoaderService getOrderDataLoaderService() {
        return getInstance().orderDataLoaderService;
    }
    
    public static ICartDataValidationService getCartDataValidationService() {
        return getInstance().cartDataValidationService;
    }
    
    public static IOrderDataValidationService getOrderDataValidationService() {
        return getInstance().orderDataValidationService;
    }
    
    public static com.aims.core.presentation.utils.OrderValidationStateManager getOrderValidationStateManager() {
        return getInstance().orderValidationStateManager;
    }
    
    public static com.aims.core.presentation.utils.PaymentFlowLogger getPaymentFlowLogger() {
        return getInstance().paymentFlowLogger;
    }
}
