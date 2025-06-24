import React from 'react';
import Button from '../ui/Button';
import Card from '../ui/Card';
import { AlertTriangle, ShoppingCart, Minus, Plus, CheckCircle, X } from 'lucide-react';
import type { CartItem, StockValidationResult } from '../../types';

interface StockValidationDialogProps {
  isOpen: boolean;
  onClose: () => void;
  stockValidationResults: StockValidationResult[];
  onUpdateQuantity: (productId: string, newQuantity: number) => void;
  onRemoveItem: (productId: string) => void;
  onProceedWithAdjustments: () => void;
  cartItems: CartItem[];
}

export const StockValidationDialog: React.FC<StockValidationDialogProps> = ({
  isOpen,
  onClose,
  stockValidationResults,
  onUpdateQuantity,
  onRemoveItem,
  onProceedWithAdjustments,
  cartItems,
}) => {
  const failedValidations = stockValidationResults.filter(result => !result.isValid);
  const hasOutOfStockItems = failedValidations.some(result => result.availableStock === 0);

  const getCartItem = (productId: string) => {
    return cartItems.find(item => item.product.id === productId);
  };

  const handleQuantityChange = (productId: string, change: number) => {
    const cartItem = getCartItem(productId);
    if (!cartItem) return;

    const newQuantity = Math.max(0, cartItem.quantity + change);
    const validation = stockValidationResults.find(r => r.productId === productId);
    const maxQuantity = validation?.availableStock || 0;
    
    if (newQuantity <= maxQuantity) {
      onUpdateQuantity(productId, newQuantity);
    }
  };

  const canProceedWithCurrentState = () => {
    return failedValidations.every(validation => {
      const cartItem = getCartItem(validation.productId);
      return cartItem && cartItem.quantity <= validation.availableStock;
    });
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      {/* Backdrop */}
      <div 
        className="absolute inset-0 bg-black bg-opacity-50"
        onClick={onClose}
      />
      
      {/* Modal Content */}
      <div className="relative bg-white rounded-lg shadow-xl max-w-2xl w-full mx-4 max-h-[80vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b">
          <div className="flex items-center gap-2 text-orange-600">
            <AlertTriangle className="w-5 h-5" />
            <h2 className="text-xl font-semibold">Stock Availability Issues</h2>
          </div>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 transition-colors"
          >
            <X className="w-6 h-6" />
          </button>
        </div>

        {/* Content */}
        <div className="p-6 space-y-4">
          <div className="bg-orange-50 border border-orange-200 rounded-lg p-4">
            <p className="text-sm text-orange-800">
              {failedValidations.length} item(s) in your cart have stock availability issues. 
              Please review and adjust quantities or remove items to continue.
            </p>
          </div>

          <div className="space-y-3">
            {failedValidations.map((validation) => {
              const cartItem = getCartItem(validation.productId);
              if (!cartItem) return null;

              return (
                <Card key={validation.productId} className="p-4">
                  <div className="flex items-start justify-between">
                    <div className="flex-1">
                      <h4 className="font-medium text-gray-900">{validation.productTitle}</h4>
                      <div className="mt-2 space-y-1">
                        <p className="text-sm text-gray-600">
                          Requested: <span className="font-medium">{validation.requestedQuantity}</span>
                        </p>
                        <p className="text-sm text-gray-600">
                          Available: <span className={`font-medium ${validation.availableStock === 0 ? 'text-red-600' : 'text-green-600'}`}>
                            {validation.availableStock}
                          </span>
                        </p>
                        {validation.availableStock === 0 && (
                          <p className="text-sm text-red-600 font-medium">Out of Stock</p>
                        )}
                        {validation.reasonCode && (
                          <p className="text-xs text-gray-500">
                            Reason: {validation.reasonCode}
                          </p>
                        )}
                      </div>
                    </div>

                    <div className="flex flex-col items-end gap-2">
                      {validation.availableStock > 0 ? (
                        <div className="flex items-center gap-2">
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => handleQuantityChange(validation.productId, -1)}
                            disabled={cartItem.quantity <= 0}
                          >
                            <Minus className="w-4 h-4" />
                          </Button>
                          <span className="w-12 text-center font-medium">{cartItem.quantity}</span>
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => handleQuantityChange(validation.productId, 1)}
                            disabled={cartItem.quantity >= validation.availableStock}
                          >
                            <Plus className="w-4 h-4" />
                          </Button>
                        </div>
                      ) : (
                        <Button
                          variant="danger"
                          size="sm"
                          onClick={() => onRemoveItem(validation.productId)}
                        >
                          Remove Item
                        </Button>
                      )}
                    </div>
                  </div>

                  {validation.availableStock > 0 && cartItem.quantity > validation.availableStock && (
                    <div className="mt-3 p-3 bg-yellow-50 border border-yellow-200 rounded">
                      <p className="text-sm text-yellow-800">
                        Suggested: Reduce quantity to {validation.availableStock} or fewer
                      </p>
                    </div>
                  )}

                  {validation.shortfallQuantity && validation.shortfallQuantity > 0 && (
                    <div className="mt-3 p-3 bg-red-50 border border-red-200 rounded">
                      <p className="text-sm text-red-800">
                        Short by {validation.shortfallQuantity} units
                      </p>
                    </div>
                  )}
                </Card>
              );
            })}
          </div>

          {/* Summary of cart items that pass validation */}
          {stockValidationResults.some(result => result.isValid) && (
            <div className="border-t pt-4">
              <h4 className="font-medium text-gray-900 mb-2 flex items-center gap-2">
                <CheckCircle className="w-4 h-4 text-green-600" />
                Items Ready for Checkout:
              </h4>
              <div className="space-y-2">
                {stockValidationResults
                  .filter(result => result.isValid)
                  .map((validation) => {
                    const cartItem = getCartItem(validation.productId);
                    if (!cartItem) return null;

                    return (
                      <div key={validation.productId} className="flex justify-between items-center py-2 px-3 bg-green-50 rounded">
                        <span className="text-sm text-gray-700">{validation.productTitle}</span>
                        <span className="text-sm font-medium text-green-700">
                          Qty: {cartItem.quantity} ✓
                        </span>
                      </div>
                    );
                  })
                }
              </div>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="flex justify-between items-center gap-2 p-6 border-t bg-gray-50">
          <Button variant="outline" onClick={onClose}>
            Continue Shopping
          </Button>
          <Button
            onClick={onProceedWithAdjustments}
            disabled={!canProceedWithCurrentState()}
            className="bg-blue-600 hover:bg-blue-700"
          >
            <ShoppingCart className="w-4 h-4 mr-2" />
            Proceed to Checkout
          </Button>
        </div>
      </div>
    </div>
  );
};

export default StockValidationDialog;