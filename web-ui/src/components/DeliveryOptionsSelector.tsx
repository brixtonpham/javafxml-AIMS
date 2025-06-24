import React, { useState, useEffect } from 'react';
import Card from './ui/Card';
import Button from './ui/Button';
import { useCartContext } from '../contexts/CartContext';
import { deliveryService } from '../services/DeliveryService';
import { stockValidationService } from '../services/stockValidationService';
import { Clock, MapPin, AlertCircle, CheckCircle, Info, Truck } from 'lucide-react';
import type {
  DeliveryOptions,
  CheckoutDeliveryInfo,
  DeliveryCalculationResult
} from '../types/checkout';
import type { OrderItem, RushDeliveryEligibilityResult } from '../types';

interface DeliveryOptionsSelectorProps {
  deliveryInfo: CheckoutDeliveryInfo;
  onOptionsChange: (options: DeliveryOptions) => void;
  onCalculationComplete: (result: DeliveryCalculationResult) => void;
  onNext: () => void;
  onBack: () => void;
  initialOptions?: DeliveryOptions;
}

const DeliveryOptionsSelector: React.FC<DeliveryOptionsSelectorProps> = ({
  deliveryInfo,
  onOptionsChange,
  onCalculationComplete,
  onNext,
  onBack,
  initialOptions
}) => {
  const { items, totalPrice } = useCartContext();
  
  const [options, setOptions] = useState<DeliveryOptions>(
    initialOptions || {
      isRushOrder: false,
      deliveryInstructions: '',
      preferredDeliveryTime: 'any'
    }
  );
  
  const [isCalculating, setIsCalculating] = useState(false);
  const [calculationResult, setCalculationResult] = useState<DeliveryCalculationResult | null>(null);
  const [errors, setErrors] = useState<string[]>([]);
  const [rushAvailable, setRushAvailable] = useState(false);
  const [rushEligibilityResult, setRushEligibilityResult] = useState<RushDeliveryEligibilityResult | null>(null);
  const [isCheckingEligibility, setIsCheckingEligibility] = useState(false);

  // Check if rush delivery is available for the location
  useEffect(() => {
    if (deliveryInfo.province && deliveryInfo.city) {
      const available = deliveryService.isRushDeliveryAvailable(
        deliveryInfo.province,
        deliveryInfo.city
      );
      setRushAvailable(available);
      
      // Reset rush order if not available
      if (!available && options.isRushOrder) {
        const newOptions = { ...options, isRushOrder: false };
        setOptions(newOptions);
        onOptionsChange(newOptions);
      }
    }
  }, [deliveryInfo.province, deliveryInfo.city]);

  // Check rush delivery eligibility for cart items
  useEffect(() => {
    if (items.length > 0 && rushAvailable) {
      checkRushDeliveryEligibility();
    }
  }, [items, rushAvailable]);

  // Calculate delivery fee whenever options change
  useEffect(() => {
    if (deliveryInfo.province && deliveryInfo.city && items.length > 0) {
      calculateDeliveryFee();
    }
  }, [options.isRushOrder, deliveryInfo, items]);

  const checkRushDeliveryEligibility = async () => {
    if (!items.length || !rushAvailable) return;

    setIsCheckingEligibility(true);
    try {
      const productIds = items.map(item => item.productId);
      const serviceResult = await stockValidationService.checkRushDeliveryEligibility(productIds);
      
      // Map service result to expected RushDeliveryEligibilityResult format
      const eligibilityResult: RushDeliveryEligibilityResult = {
        isEligible: serviceResult.isEligible,
        message: serviceResult.isEligible
          ? "All items are eligible for rush delivery"
          : `${serviceResult.ineligibleProducts.length} item(s) not eligible for rush delivery`,
        reasonCode: serviceResult.isEligible ? 'ELIGIBLE' : 'INELIGIBLE_PRODUCTS',
        eligibleDistricts: rushAvailable && deliveryInfo.province === 'Hanoi'
          ? ['Ba Đình', 'Hoàn Kiếm', 'Hai Bà Trưng', 'Đống Đa', 'Tây Hồ', 'Cầu Giấy', 'Thanh Xuân']
          : rushAvailable && deliveryInfo.province === 'Ho Chi Minh City'
          ? ['District 1', 'District 3', 'District 5', 'Bình Thạnh', 'Phú Nhuận']
          : []
      };
      
      setRushEligibilityResult(eligibilityResult);

      // If rush delivery is not eligible for some products, show warnings
      if (!serviceResult.isEligible && serviceResult.ineligibleProducts.length > 0) {
        const warnings = serviceResult.ineligibleProducts.map(productId => {
          const item = items.find(i => i.productId === productId);
          const reason = serviceResult.reasons[productId];
          return `${item?.product.title || productId}: ${reason}`;
        });
        setErrors(prev => [...prev, ...warnings]);
      }
    } catch (error) {
      console.error('Failed to check rush delivery eligibility:', error);
      setErrors(prev => [...prev, 'Failed to check rush delivery eligibility']);
    } finally {
      setIsCheckingEligibility(false);
    }
  };

  const calculateDeliveryFee = async () => {
    if (!deliveryInfo.province || !deliveryInfo.city || items.length === 0) {
      return;
    }

    setIsCalculating(true);
    setErrors([]);

    try {
      // Convert cart items to OrderItem format
      const orderItems: OrderItem[] = items.map(item => ({
        productId: item.productId,
        productTitle: item.product.title,
        productType: item.product.productType,
        quantity: item.quantity,
        unitPrice: item.product.price,
        subtotal: item.subtotal,
        productMetadata: {
          author: item.product.author,
          artists: item.product.artists,
          director: item.product.director,
          category: item.product.category,
        }
      }));

      const result = await deliveryService.calculateDeliveryFee(
        orderItems,
        deliveryInfo,
        options.isRushOrder
      );

      setCalculationResult(result);
      onCalculationComplete(result);

      // Show warnings if any
      if (result.warnings && result.warnings.length > 0) {
        setErrors(result.warnings);
      }
    } catch (error) {
      setErrors([error instanceof Error ? error.message : 'Failed to calculate delivery fee']);
    } finally {
      setIsCalculating(false);
    }
  };

  const handleOptionChange = (field: keyof DeliveryOptions, value: any) => {
    const newOptions = { ...options, [field]: value };
    setOptions(newOptions);
    onOptionsChange(newOptions);
  };

  const handleNext = () => {
    // Validate delivery instructions length
    if (options.deliveryInstructions && options.deliveryInstructions.length > 500) {
      setErrors(['Delivery instructions must be less than 500 characters']);
      return;
    }

    onNext();
  };

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-bold text-gray-900 mb-2">Delivery Options</h2>
        <p className="text-gray-600">
          Choose your preferred delivery options for {deliveryInfo.city}, {deliveryInfo.province}
        </p>
      </div>

      {/* Rush Delivery Option */}
      <Card>
        <div className="p-6">
          <div className="flex items-start space-x-3">
            <input
              type="checkbox"
              id="rushDelivery"
              checked={options.isRushOrder}
              onChange={(e) => handleOptionChange('isRushOrder', e.target.checked)}
              disabled={!rushAvailable || (rushEligibilityResult?.isEligible === false)}
              className="mt-1 h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded disabled:opacity-50"
            />
            <div className="flex-1">
              <div className="flex items-center">
                <label
                  htmlFor="rushDelivery"
                  className={`text-sm font-medium ${rushAvailable ? 'text-gray-900' : 'text-gray-400'}`}
                >
                  <Truck className="inline w-4 h-4 mr-2" />
                  Rush Delivery - Same Day
                </label>
                {rushAvailable && (
                  <span className="ml-2 inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">
                    <CheckCircle className="w-3 h-3 mr-1" />
                    Available
                  </span>
                )}
                {isCheckingEligibility && (
                  <span className="ml-2 inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                    <Clock className="w-3 h-3 mr-1 animate-spin" />
                    Checking...
                  </span>
                )}
              </div>
              
              <div className="mt-2 space-y-2">
                <p className={`text-sm ${rushAvailable ? 'text-gray-600' : 'text-gray-400'}`}>
                  {rushAvailable
                    ? 'Get your order delivered on the same day for an additional fee'
                    : 'Only available for inner districts of Hanoi and Ho Chi Minh City'
                  }
                </p>

                {/* District Information for Hanoi */}
                {deliveryInfo.province === 'Hanoi' && rushAvailable && (
                  <div className="bg-blue-50 p-3 rounded-lg">
                    <div className="flex items-center mb-2">
                      <MapPin className="w-4 h-4 text-blue-600 mr-2" />
                      <span className="text-sm font-medium text-blue-900">
                        Hanoi Rush Delivery Districts
                      </span>
                    </div>
                    <div className="text-xs text-blue-700 space-y-1">
                      <p><strong>Inner Districts:</strong> Ba Đình, Hoàn Kiếm, Hai Bà Trưng, Đống Đa</p>
                      <p><strong>Extended Areas:</strong> Tây Hồ, Cầu Giấy, Thanh Xuân</p>
                      <p className="text-blue-600 mt-2">
                        <Info className="inline w-3 h-3 mr-1" />
                        Current location: {deliveryInfo.city} -
                        {rushEligibilityResult?.eligibleDistricts.includes(deliveryInfo.city)
                          ? ' ✅ Eligible'
                          : ' ⚠️ Please verify eligibility'}
                      </p>
                    </div>
                  </div>
                )}

                {/* District Information for Ho Chi Minh City */}
                {deliveryInfo.province === 'Ho Chi Minh City' && rushAvailable && (
                  <div className="bg-blue-50 p-3 rounded-lg">
                    <div className="flex items-center mb-2">
                      <MapPin className="w-4 h-4 text-blue-600 mr-2" />
                      <span className="text-sm font-medium text-blue-900">
                        Ho Chi Minh City Rush Delivery Districts
                      </span>
                    </div>
                    <div className="text-xs text-blue-700 space-y-1">
                      <p><strong>Central Districts:</strong> District 1, District 3, District 5</p>
                      <p><strong>Extended Areas:</strong> Bình Thạnh, Phú Nhuận</p>
                      <p className="text-blue-600 mt-2">
                        <Info className="inline w-3 h-3 mr-1" />
                        Current location: {deliveryInfo.city} -
                        {rushEligibilityResult?.eligibleDistricts.includes(deliveryInfo.city)
                          ? ' ✅ Eligible'
                          : ' ⚠️ Please verify eligibility'}
                      </p>
                    </div>
                  </div>
                )}

                {/* Rush Eligibility Status */}
                {rushEligibilityResult && (
                  <div className={`p-3 rounded-lg ${rushEligibilityResult.isEligible ? 'bg-green-50' : 'bg-orange-50'}`}>
                    <div className="flex items-center">
                      {rushEligibilityResult.isEligible ? (
                        <CheckCircle className="w-4 h-4 text-green-600 mr-2" />
                      ) : (
                        <AlertCircle className="w-4 h-4 text-orange-600 mr-2" />
                      )}
                      <span className={`text-sm font-medium ${rushEligibilityResult.isEligible ? 'text-green-900' : 'text-orange-900'}`}>
                        {rushEligibilityResult.message}
                      </span>
                    </div>
                  </div>
                )}

                {options.isRushOrder && calculationResult && (
                  <p className="text-sm text-blue-600 mt-1">
                    Rush delivery fee: +{deliveryService.formatDeliveryFee(calculationResult.rushDeliveryFee)}
                  </p>
                )}
              </div>
            </div>
          </div>
        </div>
      </Card>

      {/* Preferred Delivery Time */}
      <Card>
        <div className="p-6">
          <label className="block text-sm font-medium text-gray-900 mb-3">
            Preferred Delivery Time
          </label>
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
            {[
              { value: 'morning', label: 'Morning (8AM-12PM)' },
              { value: 'afternoon', label: 'Afternoon (12PM-5PM)' },
              { value: 'evening', label: 'Evening (5PM-8PM)' },
              { value: 'any', label: 'Any Time' }
            ].map((time) => (
              <label key={time.value} className="relative">
                <input
                  type="radio"
                  name="preferredTime"
                  value={time.value}
                  checked={options.preferredDeliveryTime === time.value}
                  onChange={(e) => handleOptionChange('preferredDeliveryTime', e.target.value)}
                  className="sr-only peer"
                />
                <div className="border-2 border-gray-200 rounded-lg p-3 cursor-pointer peer-checked:border-blue-500 peer-checked:bg-blue-50 hover:border-gray-300 transition-colors">
                  <div className="text-sm font-medium text-gray-900">{time.label}</div>
                </div>
              </label>
            ))}
          </div>
        </div>
      </Card>

      {/* Delivery Instructions */}
      <Card>
        <div className="p-6">
          <label htmlFor="deliveryInstructions" className="block text-sm font-medium text-gray-900 mb-2">
            Delivery Instructions (Optional)
          </label>
          <textarea
            id="deliveryInstructions"
            rows={3}
            value={options.deliveryInstructions || ''}
            onChange={(e) => handleOptionChange('deliveryInstructions', e.target.value)}
            placeholder="e.g., Leave at front door, Ring doorbell twice, Call when arrived..."
            className="w-full border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none"
            maxLength={500}
          />
          <p className="text-xs text-gray-500 mt-1">
            {(options.deliveryInstructions || '').length}/500 characters
          </p>
        </div>
      </Card>

      {/* Delivery Summary */}
      {calculationResult && (
        <Card>
          <div className="p-6">
            <h3 className="text-lg font-medium text-gray-900 mb-4">Delivery Summary</h3>
            <div className="space-y-3">
              <div className="flex justify-between text-sm">
                <span className="text-gray-600">Delivery to:</span>
                <span className="text-gray-900">{calculationResult.breakdown.location}</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-gray-600">Delivery zone:</span>
                <span className="text-gray-900">{calculationResult.breakdown.deliveryZone}</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-gray-600">Estimated delivery:</span>
                <span className="text-gray-900">
                  {options.isRushOrder && rushAvailable
                    ? 'Same day'
                    : `${calculationResult.estimatedDeliveryDays} business day${calculationResult.estimatedDeliveryDays > 1 ? 's' : ''}`
                  }
                </span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-gray-600">Total weight:</span>
                <span className="text-gray-900">{calculationResult.breakdown.totalWeight.toFixed(1)} kg</span>
              </div>
              <div className="border-t pt-3">
                <div className="flex justify-between font-medium">
                  <span className="text-gray-900">Shipping Fee:</span>
                  <span className="text-gray-900">
                    {deliveryService.formatDeliveryFee(calculationResult.totalShippingFee)}
                  </span>
                </div>
                {calculationResult.freeShippingDiscount > 0 && (
                  <div className="flex justify-between text-sm text-green-600">
                    <span>Free shipping discount:</span>
                    <span>-{deliveryService.formatDeliveryFee(calculationResult.freeShippingDiscount)}</span>
                  </div>
                )}
              </div>
            </div>
          </div>
        </Card>
      )}

      {/* Free Shipping Progress */}
      {totalPrice < deliveryService.getFreeShippingThreshold() && (
        <Card>
          <div className="p-6">
            <div className="flex items-center justify-between mb-2">
              <h3 className="text-sm font-medium text-gray-900">Free Shipping Progress</h3>
              <span className="text-sm text-gray-600">
                {deliveryService.formatDeliveryFee(deliveryService.getFreeShippingThreshold())} for free shipping
              </span>
            </div>
            <div className="w-full bg-gray-200 rounded-full h-2">
              <div
                className="bg-blue-600 h-2 rounded-full transition-all duration-300"
                style={{
                  width: `${Math.min((totalPrice / deliveryService.getFreeShippingThreshold()) * 100, 100)}%`
                }}
              />
            </div>
            <p className="text-sm text-gray-600 mt-2">
              Add {deliveryService.formatDeliveryFee(deliveryService.calculateRemainingForFreeShipping(totalPrice))} more to get free shipping!
            </p>
          </div>
        </Card>
      )}

      {/* Error Messages */}
      {errors.length > 0 && (
        <Card>
          <div className="p-6">
            <div className="rounded-md bg-yellow-50 p-4">
              <div className="flex">
                <div className="flex-shrink-0">
                  <svg className="h-5 w-5 text-yellow-400" viewBox="0 0 20 20" fill="currentColor">
                    <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
                  </svg>
                </div>
                <div className="ml-3">
                  <h3 className="text-sm font-medium text-yellow-800">Notice</h3>
                  <div className="mt-2 text-sm text-yellow-700">
                    <ul className="list-disc space-y-1 pl-5">
                      {errors.map((error, index) => (
                        <li key={index}>{error}</li>
                      ))}
                    </ul>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </Card>
      )}

      {/* Navigation Buttons */}
      <div className="flex justify-between pt-6">
        <Button
          variant="outline"
          onClick={onBack}
          className="px-6"
        >
          Back
        </Button>
        <Button
          onClick={handleNext}
          disabled={isCalculating || !calculationResult}
          isLoading={isCalculating}
          className="px-6"
        >
          {isCalculating ? 'Calculating...' : 'Continue to Order Summary'}
        </Button>
      </div>
    </div>
  );
};

export default DeliveryOptionsSelector;