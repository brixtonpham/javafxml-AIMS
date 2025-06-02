package com.aims.core.application.impl;

import com.aims.core.entities.Cart;
import com.aims.core.entities.CartItem;
import com.aims.core.entities.Product;
import com.aims.core.entities.UserAccount;
import com.aims.core.infrastructure.database.dao.ICartDAO;
import com.aims.core.infrastructure.database.dao.ICartItemDAO;
import com.aims.core.infrastructure.database.dao.IProductDAO;
import com.aims.core.infrastructure.database.dao.IUserAccountDAO;
import com.aims.core.shared.exceptions.ValidationException;
import com.aims.core.shared.exceptions.ResourceNotFoundException;
import com.aims.core.shared.exceptions.InventoryException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private ICartDAO cartDAO;

    @Mock
    private ICartItemDAO cartItemDAO;

    @Mock
    private IProductDAO productDAO;

    @Mock
    private IUserAccountDAO userAccountDAO;

    @InjectMocks
    private CartServiceImpl cartService;

    private Cart mockCart;
    private Product mockProduct;
    private UserAccount mockUser;
    private String cartSessionId;
    private String productId;
    private String userId;

    @BeforeEach
    void setUp() {
        cartSessionId = UUID.randomUUID().toString();
        productId = "prod123";
        userId = "user123";

        mockUser = new UserAccount();
        mockUser.setUserId(userId);
        mockUser.setUsername("testuser");

        mockProduct = new Product();
        mockProduct.setProductId(productId);
        mockProduct.setTitle("Test Product");
        mockProduct.setPrice(100.0f); 
        mockProduct.setQuantityInStock(10);

        mockCart = new Cart(cartSessionId, null, LocalDateTime.now());
        mockCart.setItems(new ArrayList<>()); // Initialize items list
    }

    @Test
    void getCart_existingCart_returnsCart() throws SQLException {
        when(cartDAO.getBySessionId(cartSessionId)).thenReturn(mockCart);
        // Mock productDAO for refreshCartCalculationsAndStockStatus if items exist
        if (!mockCart.getItems().isEmpty()) {
            for (CartItem item : mockCart.getItems()) {
                when(productDAO.getById(item.getProduct().getProductId())).thenReturn(item.getProduct());
            }
        }

        Cart foundCart = cartService.getCart(cartSessionId);

        assertNotNull(foundCart);
        assertEquals(cartSessionId, foundCart.getCartSessionId());
        verify(cartDAO).getBySessionId(cartSessionId);
    }

    @Test
    void getCart_nonExistingCart_returnsNull() throws SQLException {
        when(cartDAO.getBySessionId(cartSessionId)).thenReturn(null);

        Cart foundCart = cartService.getCart(cartSessionId);

        assertNull(foundCart);
        verify(cartDAO).getBySessionId(cartSessionId);
    }
    
    @Test
    void getCart_nullSessionId_returnsNull() throws SQLException {
        Cart foundCart = cartService.getCart(null);
        assertNull(foundCart);
        verify(cartDAO, never()).getBySessionId(any());
    }


    @Test
    void addItemToCart_newProduct_addsItem() throws SQLException, ResourceNotFoundException, ValidationException, InventoryException {
        when(productDAO.getById(productId)).thenReturn(mockProduct);
        when(cartDAO.getBySessionId(cartSessionId)).thenReturn(mockCart); // Cart exists
        // No need for a separate mock for mockCart.getCartSessionId() if it's the same as cartSessionId

        Cart updatedCart = cartService.addItemToCart(cartSessionId, productId, 1);

        assertNotNull(updatedCart);
        assertEquals(1, updatedCart.getItems().size());
        assertEquals(productId, updatedCart.getItems().get(0).getProduct().getProductId());
        assertEquals(1, updatedCart.getItems().get(0).getQuantity());
        verify(productDAO, times(2)).getById(productId); // Expect 2 calls: initial check + refresh in final getCart
        verify(cartItemDAO).add(any(CartItem.class));
        verify(cartDAO).saveOrUpdate(any(Cart.class));
        verify(cartDAO, times(2)).getBySessionId(cartSessionId); // Initial fetch + final getCart
    }

    @Test
    void addItemToCart_newProduct_toNewCart_createsCartAndAddsItem() throws SQLException, ResourceNotFoundException, ValidationException, InventoryException {
        String newCartSessionId = UUID.randomUUID().toString();
        
        // This cart will be returned by the final getCart() call within addItemToCart
        Cart cartReturnedByService = new Cart(newCartSessionId, null, LocalDateTime.now());
        cartReturnedByService.setItems(new ArrayList<>());
        // Use the global mockProduct from setUp
        CartItem itemAdded = new CartItem(cartReturnedByService, mockProduct, 1); 
        cartReturnedByService.getItems().add(itemAdded);

        when(productDAO.getById(productId)).thenReturn(mockProduct);
        when(cartDAO.getBySessionId(newCartSessionId))
            .thenReturn(null)                         // First call in addItemToCart: cart doesn't exist
            .thenReturn(cartReturnedByService);       // Second call, from getCart() at the end of addItemToCart

        // Ensure DAO operations that modify state do nothing to the mocks,
        // as we control the state through what getBySessionId returns.
        doNothing().when(cartDAO).saveOrUpdate(any(Cart.class));
        doNothing().when(cartItemDAO).add(any(CartItem.class));


        Cart updatedCart = cartService.addItemToCart(newCartSessionId, productId, 1);

        assertNotNull(updatedCart);
        assertSame(cartReturnedByService, updatedCart); // Verify the prepared cart is returned
        assertEquals(1, updatedCart.getItems().size());
        assertEquals(productId, updatedCart.getItems().get(0).getProduct().getProductId());
        assertEquals(1, updatedCart.getItems().get(0).getQuantity());

        verify(productDAO, times(2)).getById(productId); // Called by addItemToCart and by refreshCartCalculations in final getCart
        verify(cartDAO, times(2)).getBySessionId(newCartSessionId); // Once for check, once for return getCart
        verify(cartItemDAO).add(any(CartItem.class));
        verify(cartDAO, times(2)).saveOrUpdate(any(Cart.class)); // Once for new cart, once for item update
    }


    @Test
    void addItemToCart_existingProduct_updatesQuantity() throws SQLException, ResourceNotFoundException, ValidationException, InventoryException {
        CartItem existingItem = new CartItem(mockCart, mockProduct, 1);
        mockCart.getItems().add(existingItem);

        when(productDAO.getById(productId)).thenReturn(mockProduct);
        when(cartDAO.getBySessionId(cartSessionId)).thenReturn(mockCart);
         // Mock getCart for the return call
        when(cartDAO.getBySessionId(mockCart.getCartSessionId())).thenReturn(mockCart);


        Cart updatedCart = cartService.addItemToCart(cartSessionId, productId, 2);

        assertNotNull(updatedCart);
        assertEquals(1, updatedCart.getItems().size());
        assertEquals(3, updatedCart.getItems().get(0).getQuantity()); // 1 + 2
        verify(cartItemDAO).update(existingItem);
        verify(cartDAO).saveOrUpdate(mockCart);
    }

    @Test
    void addItemToCart_productNotFound_throwsResourceNotFoundException() throws SQLException {
        when(productDAO.getById(productId)).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> {
            cartService.addItemToCart(cartSessionId, productId, 1);
        });
        verify(productDAO).getById(productId);
    }

    @Test
    void addItemToCart_insufficientStock_throwsInventoryException() throws SQLException {
        mockProduct.setQuantityInStock(0);
        when(productDAO.getById(productId)).thenReturn(mockProduct);
        // when(cartDAO.getBySessionId(cartSessionId)).thenReturn(mockCart); // Unnecessary stubbing


        assertThrows(InventoryException.class, () -> {
            cartService.addItemToCart(cartSessionId, productId, 1);
        });
    }
    
    @Test
    void addItemToCart_addMoreThanStock_throwsInventoryException() throws SQLException {
        mockProduct.setQuantityInStock(5);
        CartItem existingItem = new CartItem(mockCart, mockProduct, 3); // Already 3 in cart
        mockCart.getItems().add(existingItem);

        when(productDAO.getById(productId)).thenReturn(mockProduct);
        when(cartDAO.getBySessionId(cartSessionId)).thenReturn(mockCart);

        assertThrows(InventoryException.class, () -> {
            cartService.addItemToCart(cartSessionId, productId, 3); // Requesting 3 more, total 6, stock 5
        });
    }


    @Test
    void addItemToCart_invalidQuantity_throwsValidationException() {
        assertThrows(ValidationException.class, () -> {
            cartService.addItemToCart(cartSessionId, productId, 0);
        });
        assertThrows(ValidationException.class, () -> {
            cartService.addItemToCart(cartSessionId, productId, -1);
        });
    }

    @Test
    void removeItemFromCart_itemExists_removesItem() throws SQLException, ResourceNotFoundException {
        CartItem itemToRemove = new CartItem(mockCart, mockProduct, 1);
        mockCart.getItems().add(itemToRemove);

        when(cartDAO.getBySessionId(cartSessionId)).thenReturn(mockCart);
        // Mock productDAO for refreshCartCalculationsAndStockStatus
        when(productDAO.getById(productId)).thenReturn(mockProduct);


        Cart updatedCart = cartService.removeItemFromCart(cartSessionId, productId);

        assertNotNull(updatedCart);
        assertTrue(updatedCart.getItems().isEmpty());
        verify(cartItemDAO).delete(cartSessionId, productId);
        verify(cartDAO).saveOrUpdate(mockCart);
    }

    @Test
    void removeItemFromCart_cartNotFound_throwsResourceNotFoundException() throws SQLException {
        when(cartDAO.getBySessionId(cartSessionId)).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> {
            cartService.removeItemFromCart(cartSessionId, productId);
        });
    }

    @Test
    void removeItemFromCart_itemNotFoundInCart_throwsResourceNotFoundException() throws SQLException {
        when(cartDAO.getBySessionId(cartSessionId)).thenReturn(mockCart); // Cart exists but is empty
        // Mock productDAO for refreshCartCalculationsAndStockStatus
        // No items, so no productDAO calls expected from refreshCartCalculationsAndStockStatus

        assertThrows(ResourceNotFoundException.class, () -> {
            cartService.removeItemFromCart(cartSessionId, productId);
        });
    }

    @Test
    void updateItemQuantity_itemExists_updatesQuantity() throws SQLException, ResourceNotFoundException, ValidationException, InventoryException {
        CartItem itemToUpdate = new CartItem(mockCart, mockProduct, 1);
        mockCart.getItems().add(itemToUpdate);

        when(cartDAO.getBySessionId(cartSessionId)).thenReturn(mockCart);
        when(productDAO.getById(productId)).thenReturn(mockProduct); // For stock check and refresh

        Cart updatedCart = cartService.updateItemQuantity(cartSessionId, productId, 5);

        assertNotNull(updatedCart);
        assertEquals(1, updatedCart.getItems().size());
        assertEquals(5, updatedCart.getItems().get(0).getQuantity());
        verify(cartItemDAO).update(itemToUpdate);
        verify(cartDAO).saveOrUpdate(mockCart);
    }
    
    @Test
    void updateItemQuantity_toZero_removesItem() throws SQLException, ResourceNotFoundException, ValidationException, InventoryException {
        CartItem itemToUpdate = new CartItem(mockCart, mockProduct, 1);
        mockCart.getItems().add(itemToUpdate);

        // Ensure getBySessionId returns mockCart for all calls within the service method execution
        when(cartDAO.getBySessionId(cartSessionId)).thenReturn(mockCart); 
        when(productDAO.getById(productId)).thenReturn(mockProduct); 
        doNothing().when(cartItemDAO).delete(cartSessionId, productId);
        doNothing().when(cartDAO).saveOrUpdate(mockCart);


        Cart updatedCart = cartService.updateItemQuantity(cartSessionId, productId, 0);

        assertNotNull(updatedCart);
        assertTrue(updatedCart.getItems().isEmpty());
        verify(cartItemDAO).delete(cartSessionId, productId); 
        verify(cartDAO, times(1)).saveOrUpdate(mockCart); // Corrected from times(2)
    }


    @Test
    void updateItemQuantity_insufficientStock_throwsInventoryException() throws SQLException, ResourceNotFoundException {
        CartItem itemToUpdate = new CartItem(mockCart, mockProduct, 1);
        mockCart.getItems().add(itemToUpdate);
        mockProduct.setQuantityInStock(3); // Stock is 3

        when(cartDAO.getBySessionId(cartSessionId)).thenReturn(mockCart);
        when(productDAO.getById(productId)).thenReturn(mockProduct);

        assertThrows(InventoryException.class, () -> {
            cartService.updateItemQuantity(cartSessionId, productId, 5); // Requesting 5
        });
    }
    
    @Test
    void updateItemQuantity_negativeQuantity_throwsValidationException() throws SQLException {
         // when(cartDAO.getBySessionId(cartSessionId)).thenReturn(mockCart); // Unnecessary stubbing
         // No product needed as validation happens first
        assertThrows(ValidationException.class, () -> {
            cartService.updateItemQuantity(cartSessionId, productId, -1);
        });
    }


    @Test
    void clearCart_cartExists_clearsItems() throws SQLException, ResourceNotFoundException {
        mockCart.getItems().add(new CartItem(mockCart, mockProduct, 1));
        when(cartDAO.getBySessionId(cartSessionId)).thenReturn(mockCart);

        Cart clearedCart = cartService.clearCart(cartSessionId);

        assertNotNull(clearedCart);
        assertTrue(clearedCart.getItems().isEmpty());
        verify(cartItemDAO).deleteByCartSessionId(cartSessionId);
        verify(cartDAO).saveOrUpdate(mockCart);
    }

    @Test
    void clearCart_cartNotFound_throwsResourceNotFoundException() throws SQLException {
        when(cartDAO.getBySessionId(cartSessionId)).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> {
            cartService.clearCart(cartSessionId);
        });
    }

    @Test
    void associateCartWithUser_validIds_associatesCart() throws SQLException, ResourceNotFoundException, ValidationException {
        when(cartDAO.getBySessionId(cartSessionId)).thenReturn(mockCart);
        when(userAccountDAO.getById(userId)).thenReturn(mockUser);

        Cart associatedCart = cartService.associateCartWithUser(cartSessionId, userId);

        assertNotNull(associatedCart);
        assertNotNull(associatedCart.getUserAccount());
        assertEquals(userId, associatedCart.getUserAccount().getUserId());
        verify(cartDAO).saveOrUpdate(mockCart);
    }

    @Test
    void associateCartWithUser_cartNotFound_throwsResourceNotFoundException() throws SQLException {
        when(cartDAO.getBySessionId(cartSessionId)).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> {
            cartService.associateCartWithUser(cartSessionId, userId);
        });
    }

    @Test
    void associateCartWithUser_userNotFound_throwsResourceNotFoundException() throws SQLException {
        when(cartDAO.getBySessionId(cartSessionId)).thenReturn(mockCart);
        when(userAccountDAO.getById(userId)).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> {
            cartService.associateCartWithUser(cartSessionId, userId);
        });
    }
    
    @Test
    void associateCartWithUser_cartAlreadyAssociatedWithDifferentUser_throwsValidationException() throws SQLException {
        UserAccount anotherUser = new UserAccount();
        anotherUser.setUserId("otherUser123");
        mockCart.setUserAccount(anotherUser); // Cart already associated with someone else

        when(cartDAO.getBySessionId(cartSessionId)).thenReturn(mockCart);
        // No need to mock userAccountDAO.getById(userId) as the check for existing association comes first

        assertThrows(ValidationException.class, () -> {
            cartService.associateCartWithUser(cartSessionId, userId);
        });
    }
    
    @Test
    void associateCartWithUser_cartAlreadyAssociatedWithSameUser_succeeds() throws SQLException, ResourceNotFoundException, ValidationException {
        mockCart.setUserAccount(mockUser); // Cart already associated with the same user

        when(cartDAO.getBySessionId(cartSessionId)).thenReturn(mockCart);
        when(userAccountDAO.getById(userId)).thenReturn(mockUser); // Still need to mock this for the user fetch

        Cart associatedCart = cartService.associateCartWithUser(cartSessionId, userId);

        assertNotNull(associatedCart);
        assertEquals(mockUser, associatedCart.getUserAccount());
        verify(cartDAO).saveOrUpdate(mockCart); // Should still save (e.g., to update lastUpdated timestamp)
    }


    @Test
    void createNewCart_withUserId_createsCartAssociatedWithUser() throws SQLException {
        when(userAccountDAO.getById(userId)).thenReturn(mockUser);
        // Mock the saveOrUpdate to simulate cart creation
        doAnswer(invocation -> {
            Cart cart = invocation.getArgument(0);
            assertNotNull(cart.getCartSessionId());
            assertEquals(mockUser, cart.getUserAccount());
            return null;
        }).when(cartDAO).saveOrUpdate(any(Cart.class));


        Cart newCart = cartService.createNewCart(userId);

        assertNotNull(newCart);
        assertNotNull(newCart.getCartSessionId());
        assertEquals(mockUser, newCart.getUserAccount());
        verify(cartDAO).saveOrUpdate(any(Cart.class)); // Verifies the cart was persisted
    }

    @Test
    void createNewCart_withoutUserId_createsGuestCart() throws SQLException {
         // Mock the saveOrUpdate to simulate cart creation
        doAnswer(invocation -> {
            Cart cart = invocation.getArgument(0);
            assertNotNull(cart.getCartSessionId());
            assertNull(cart.getUserAccount());
            return null;
        }).when(cartDAO).saveOrUpdate(any(Cart.class));

        Cart newCart = cartService.createNewCart(null);

        assertNotNull(newCart);
        assertNotNull(newCart.getCartSessionId());
        assertNull(newCart.getUserAccount());
        verify(cartDAO).saveOrUpdate(any(Cart.class));
    }
}
