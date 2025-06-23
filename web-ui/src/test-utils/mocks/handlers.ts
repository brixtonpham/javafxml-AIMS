import { http, HttpResponse } from 'msw';
import type { Product, CartItem, Order, User, OrderStatus, PaymentMethodType, ProductType } from '../../types';

// Mock data
const mockProducts: Product[] = [
  {
    id: 'book-1',
    title: 'Harry Potter và Hòn đá Phù thủy',
    productType: 'BOOK',
    price: 150000,
    value: 150000,
    quantity: 50,
    description: 'Cuốn sách đầu tiên trong series Harry Potter',
    category: 'Fiction',
    imageUrl: '/images/harry-potter-1.jpg',
    entryDate: '2024-01-01T00:00:00Z',
    author: 'J.K. Rowling',
    publisher: 'NXB Trẻ',
    language: 'Vietnamese'
  },
  {
    id: 'cd-1',
    title: 'Best of Sơn Tùng M-TP',
    productType: 'CD',
    price: 200000,
    value: 200000,
    quantity: 30,
    description: 'Album tuyển tập những ca khúc hay nhất',
    category: 'Music',
    imageUrl: '/images/son-tung-best.jpg',
    entryDate: '2024-01-01T00:00:00Z',
    artists: 'Sơn Tùng M-TP',
    genre: 'Pop'
  },
  {
    id: 'dvd-1',
    title: 'Avengers: Endgame',
    productType: 'DVD',
    price: 120000,
    value: 120000,
    quantity: 25,
    description: 'Phim Marvel Studios',
    category: 'Movies',
    imageUrl: '/images/avengers-endgame.jpg',
    entryDate: '2024-01-01T00:00:00Z',
    director: 'Anthony Russo, Joe Russo',
    genre: 'Action',
    runtime: 181
  }
];

const mockUser: User = {
  id: 'user-1',
  username: 'testuser',
  email: 'test@example.com',
  fullName: 'Test User',
  status: 'ACTIVE',
  roles: [{
    id: 'role-1',
    name: 'CUSTOMER',
    permissions: ['read:products', 'create:orders']
  }],
  createdAt: '2024-01-01T00:00:00Z'
};

const mockOrders: Order[] = [
  {
    id: 'order-1',
    userId: 'user-1',
    items: [
      {
        productId: 'book-1',
        productTitle: 'Harry Potter và Hòn đá Phù thủy',
        productType: 'BOOK',
        quantity: 2,
        unitPrice: 150000,
        subtotal: 300000,
        productMetadata: {
          author: 'J.K. Rowling',
          category: 'Fiction'
        }
      }
    ],
    subtotal: 300000,
    vatAmount: 30000,
    shippingFee: 22000,
    totalAmount: 352000,
    status: 'PENDING',
    isRushOrder: false,
    createdAt: '2024-01-15T10:00:00Z',
    updatedAt: '2024-01-15T10:00:00Z',
    deliveryInfo: {
      recipientName: 'Test User',
      phone: '0123456789',
      email: 'test@example.com',
      address: '123 Test Street',
      city: 'HN_HOAN_KIEM',
      province: 'HN',
      postalCode: '10000',
      deliveryInstructions: 'Call before delivery'
    },
    paymentMethod: {
      id: 'vnpay-1',
      type: 'VNPAY',
      name: 'VNPay',
      isActive: true
    },
    paymentStatus: 'COMPLETED'
  }
];

export const handlers = [
  // Products API
  http.get('http://localhost:8080/api/products', () => {
    return HttpResponse.json({
      success: true,
      data: mockProducts,
      message: 'Products retrieved successfully',
      pagination: {
        page: 1,
        limit: 20,
        total: mockProducts.length,
        totalPages: 1
      }
    });
  }),

  http.get('http://localhost:8080/api/products/:id', ({ params }) => {
    const product = mockProducts.find(p => p.id === params.id);
    if (!product) {
      return HttpResponse.json(
        { success: false, message: 'Product not found', data: null },
        { status: 404 }
      );
    }
    return HttpResponse.json({ 
      success: true, 
      data: product, 
      message: 'Product retrieved successfully' 
    });
  }),

  // Cart API
  http.get('http://localhost:8080/api/cart', () => {
    return HttpResponse.json({
      success: true,
      data: {
        id: 'test-cart',
        sessionId: 'test-session',
        items: [],
        totalItems: 0,
        totalPrice: 0,
        totalPriceWithVAT: 0,
        stockWarnings: [],
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString()
      },
      message: 'Cart retrieved successfully'
    });
  }),

  http.post('http://localhost:8080/api/cart/items', async ({ request }) => {
    const body = await request.json() as { productId: string; quantity: number };
    const product = mockProducts.find(p => p.id === body.productId);
    
    if (!product) {
      return HttpResponse.json(
        { success: false, message: 'Product not found', data: null },
        { status: 404 }
      );
    }

    const cartItem: CartItem = {
      productId: product.id,
      product: product,
      quantity: body.quantity,
      subtotal: product.price * body.quantity
    };

    return HttpResponse.json({ 
      success: true, 
      data: cartItem, 
      message: 'Item added to cart successfully' 
    });
  }),

  http.post('http://localhost:8080/api/cart/create', async ({ request }) => {
    try {
      // Handle empty body case for cart creation
      let body = {};
      const text = await request.text();
      if (text && text.trim()) {
        body = JSON.parse(text);
      }

      // Return empty cart for cart creation
      return HttpResponse.json({
        success: true,
        data: {
          id: 'test-cart',
          sessionId: 'test-session',
          items: [],
          subtotal: 0,
          itemCount: 0,
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString()
        },
        message: 'Cart created successfully'
      });
    } catch (error) {
      console.error('Cart create handler error:', error);
      return HttpResponse.json(
        { success: false, message: 'Failed to create cart', data: null },
        { status: 500 }
      );
    }
  }),

  http.put('http://localhost:8080/api/cart/items/:productId', async ({ params, request }) => {
    const body = await request.json() as { quantity: number };
    const product = mockProducts.find(p => p.id === params.productId);
    
    if (!product) {
      return HttpResponse.json(
        { success: false, message: 'Product not found', data: null },
        { status: 404 }
      );
    }

    const cartItem: CartItem = {
      productId: product.id,
      product: product,
      quantity: body.quantity,
      subtotal: product.price * body.quantity
    };

    return HttpResponse.json({ 
      success: true, 
      data: cartItem, 
      message: 'Cart item updated successfully' 
    });
  }),

  http.delete('http://localhost:8080/api/cart/items/:productId', ({ params }) => {
    const product = mockProducts.find(p => p.id === params.productId);
    
    if (!product) {
      return HttpResponse.json(
        { success: false, message: 'Product not found', data: null },
        { status: 404 }
      );
    }

    return HttpResponse.json({ 
      success: true, 
      data: null,
      message: 'Item removed from cart successfully'
    });
  }),

  // Orders API
  http.get('http://localhost:8080/api/orders', () => {
    return HttpResponse.json({
      success: true,
      data: mockOrders,
      message: 'Orders retrieved successfully'
    });
  }),

  http.get('http://localhost:8080/api/orders/:id', ({ params }) => {
    const order = mockOrders.find(o => o.id === params.id);
    if (!order) {
      return HttpResponse.json(
        { success: false, message: 'Order not found', data: null },
        { status: 404 }
      );
    }
    return HttpResponse.json({ 
      success: true, 
      data: order, 
      message: 'Order retrieved successfully' 
    });
  }),

  http.post('http://localhost:8080/api/orders', async ({ request }) => {
    const body = await request.json() as any;
    const newOrder = {
      id: `order-${Date.now()}`,
      userId: body.userId || 'anonymous',
      items: body.items || [],
      subtotal: body.subtotal || 0,
      vatAmount: body.vatAmount || 0,
      shippingFee: body.shippingFee || 0,
      totalAmount: body.totalAmount || 0,
      status: 'PENDING' as OrderStatus,
      isRushOrder: body.isRushOrder || false,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      deliveryInfo: body.deliveryInfo,
      paymentMethod: body.paymentMethod,
      paymentStatus: 'PENDING'
    };
    return HttpResponse.json({ 
      success: true, 
      data: newOrder, 
      message: 'Order created successfully' 
    });
  }),

  // Payment API
  http.post('http://localhost:8080/api/payment/vnpay/create', async ({ request }) => {
    const body = await request.json() as any;
    return HttpResponse.json({
      success: true,
      data: {
        paymentUrl: `https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_Amount=${body.amount}&vnp_Command=pay&vnp_CreateDate=${Date.now()}&vnp_CurrCode=VND&vnp_IpAddr=127.0.0.1&vnp_Locale=vn&vnp_OrderInfo=${encodeURIComponent(body.orderInfo || 'Test payment')}&vnp_OrderType=other&vnp_ReturnUrl=${encodeURIComponent(body.returnUrl || 'http://localhost:3000/payment/return')}&vnp_TmnCode=TEST_TMN_CODE&vnp_TxnRef=${body.orderId}&vnp_Version=2.1.0`,
        orderId: body.orderId || 'test-order'
      },
      message: 'Payment URL created successfully'
    });
  }),

  // Authentication API
  http.post('http://localhost:8080/api/auth/login', async ({ request }) => {
    const body = await request.json() as { email: string; password: string };
    
    if (body.email === 'test@example.com' && body.password === 'password') {
      return HttpResponse.json({
        success: true,
        data: {
          user: mockUser,
          token: 'mock-jwt-token',
          expiresAt: new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString()
        },
        message: 'Login successful'
      });
    }
    
    return HttpResponse.json(
      { success: false, message: 'Invalid credentials', data: null },
      { status: 401 }
    );
  }),

  http.post('http://localhost:8080/api/auth/logout', () => {
    return HttpResponse.json({ 
      success: true, 
      data: null, 
      message: 'Logout successful' 
    });
  }),

  // Delivery API
  http.post('http://localhost:8080/api/delivery/calculate', async ({ request }) => {
    const body = await request.json() as any;
    return HttpResponse.json({
      success: true,
      data: {
        baseShippingFee: 22000,
        rushDeliveryFee: (body.isRushOrder) ? 30000 : 0,
        totalShippingFee: (body.isRushOrder) ? 52000 : 22000,
        estimatedDeliveryDays: (body.isRushOrder) ? 0.5 : 1,
        freeShippingDiscount: (body.subtotal >= 100000) ? 22000 : 0,
        breakdown: {
          totalWeight: 0.8,
          baseWeightFee: 22000,
          additionalWeightFee: 0,
          rushFeePerItem: (body.isRushOrder) ? 10000 : 0,
          applicableItems: body.items?.length || 0,
          location: 'Hoàn Kiếm, Hà Nội',
          deliveryZone: 'Hanoi Inner City'
        }
      },
      message: 'Delivery calculation completed successfully'
    });
  }),

  // Error reporting API
  http.post('http://localhost:8080/api/errors/report', async ({ request }) => {
    const body = await request.json() as any;
    console.warn('Test error reported:', body);
    return HttpResponse.json({
      success: true,
      data: { errorId: body.errorId || 'test-error-id' },
      message: 'Error reported successfully'
    });
  })
];