import { orderService } from './orderService';
import type { 
  DeliveryCalculationInput,
  DeliveryCalculationResult,
  CheckoutDeliveryInfo,
  DeliveryOptions
} from '../types/checkout';
import type { OrderItem, DeliveryFormData } from '../types';
import { 
  calculateDeliveryDetails, 
  validateDeliveryInfo,
  supportsRushDelivery,
  getFreeShippingThreshold,
  calculateRemainingForFreeShipping
} from '../utils/deliveryCalculations';
import { locationHelpers } from '../data/vietnamLocations';

/**
 * DeliveryService - Core service for handling delivery calculations and validation
 * Integrates with existing orderService for backend communication
 */
export class DeliveryService {
  /**
   * Calculate delivery fee and details for given items and delivery info
   */
  async calculateDeliveryFee(
    items: OrderItem[],
    deliveryInfo: CheckoutDeliveryInfo,
    isRushOrder: boolean = false
  ): Promise<DeliveryCalculationResult> {
    // Validate delivery info first
    const validationErrors = validateDeliveryInfo(deliveryInfo);
    if (validationErrors.length > 0) {
      throw new Error(`Invalid delivery information: ${validationErrors.join(', ')}`);
    }

    // Check if rush delivery is available for the location
    if (isRushOrder && !supportsRushDelivery(deliveryInfo.province, deliveryInfo.city)) {
      throw new Error('Rush delivery is not available for this location');
    }

    // Calculate delivery details using our business logic
    const calculationInput: DeliveryCalculationInput = {
      items,
      deliveryInfo,
      isRushOrder,
    };

    const result = calculateDeliveryDetails(calculationInput);

    // Optionally, call backend for validation/confirmation
    try {
      const backendResult = await orderService.calculateShippingFee({
        ...deliveryInfo,
        isRushOrder,
      } as DeliveryFormData);
      
      // Compare with our calculation for consistency
      if (Math.abs(backendResult.fee - result.baseShippingFee) > 1) {
        console.warn('Delivery fee calculation mismatch between frontend and backend');
      }
    } catch (error) {
      console.warn('Backend delivery calculation failed, using frontend calculation:', error);
    }

    return result;
  }

  /**
   * Validate delivery information
   */
  validateDeliveryInfo(deliveryInfo: CheckoutDeliveryInfo): string[] {
    return validateDeliveryInfo(deliveryInfo);
  }

  /**
   * Check if delivery info is valid
   */
  isDeliveryInfoValid(deliveryInfo: CheckoutDeliveryInfo): boolean {
    return this.validateDeliveryInfo(deliveryInfo).length === 0;
  }

  /**
   * Get delivery zones for a province
   */
  getDeliveryZone(provinceCode: string, cityCode?: string) {
    return locationHelpers.getDeliveryZone(provinceCode, cityCode);
  }

  /**
   * Check if rush delivery is available for a location
   */
  isRushDeliveryAvailable(provinceCode: string, cityCode?: string): boolean {
    return supportsRushDelivery(provinceCode, cityCode);
  }

  /**
   * Get free shipping threshold
   */
  getFreeShippingThreshold(): number {
    return getFreeShippingThreshold();
  }

  /**
   * Calculate remaining amount needed for free shipping
   */
  calculateRemainingForFreeShipping(currentSubtotal: number): number {
    return calculateRemainingForFreeShipping(currentSubtotal);
  }

  /**
   * Get all provinces for dropdown
   */
  getAllProvinces() {
    return locationHelpers.getAllProvinces();
  }

  /**
   * Get cities for a province
   */
  getCitiesForProvince(provinceCode: string) {
    return locationHelpers.getCitiesForProvince(provinceCode);
  }

  /**
   * Search locations by name
   */
  searchLocations(query: string) {
    return locationHelpers.searchLocations(query);
  }

  /**
   * Convert DeliveryOptions to DeliveryFormData for backend
   */
  convertToDeliveryFormData(
    deliveryInfo: CheckoutDeliveryInfo,
    deliveryOptions: DeliveryOptions
  ): DeliveryFormData {
    return {
      ...deliveryInfo,
      isRushOrder: deliveryOptions.isRushOrder,
      deliveryInstructions: deliveryOptions.deliveryInstructions || deliveryInfo.deliveryInstructions,
    };
  }

  /**
   * Estimate delivery time based on location and rush order
   */
  estimateDeliveryTime(
    provinceCode: string,
    cityCode?: string,
    isRushOrder: boolean = false
  ): { days: number; description: string } {
    const zone = this.getDeliveryZone(provinceCode, cityCode);
    
    let days: number;
    let description: string;

    if (isRushOrder && zone.rushDeliveryAvailable) {
      days = 0.5;
      description = 'Same day delivery';
    } else {
      switch (zone.type) {
        case 'inner_city':
          days = 1;
          description = '1 business day';
          break;
        case 'outer_city':
          days = 2;
          description = '2 business days';
          break;
        case 'province':
          days = 3;
          description = '3 business days';
          break;
        default:
          days = 3;
          description = '3 business days';
      }
    }

    return { days, description };
  }

  /**
   * Get delivery fee breakdown for display
   */
  getDeliveryFeeBreakdown(result: DeliveryCalculationResult) {
    const { breakdown, baseShippingFee, rushDeliveryFee, freeShippingDiscount, totalShippingFee } = result;
    
    return {
      items: [
        {
          label: 'Base shipping fee',
          amount: breakdown.baseWeightFee,
          description: `First ${breakdown.totalWeight <= 3 ? breakdown.totalWeight : 3}kg`,
        },
        ...(breakdown.additionalWeightFee > 0 ? [{
          label: 'Additional weight fee',
          amount: breakdown.additionalWeightFee,
          description: `Extra ${(breakdown.totalWeight - 3).toFixed(1)}kg`,
        }] : []),
        ...(rushDeliveryFee > 0 ? [{
          label: 'Rush delivery fee',
          amount: rushDeliveryFee,
          description: `${breakdown.applicableItems} items × ${breakdown.rushFeePerItem.toLocaleString('vi-VN')} VND`,
        }] : []),
        ...(freeShippingDiscount > 0 ? [{
          label: 'Free shipping discount',
          amount: -freeShippingDiscount,
          description: 'Order over 100,000 VND',
        }] : []),
      ],
      subtotal: baseShippingFee,
      total: totalShippingFee,
      location: breakdown.location,
      zone: breakdown.deliveryZone,
    };
  }

  /**
   * Format delivery fee for display
   */
  formatDeliveryFee(amount: number): string {
    return amount.toLocaleString('vi-VN') + ' VND';
  }

  /**
   * Generate delivery summary for order confirmation
   */
  generateDeliverySummary(
    deliveryInfo: CheckoutDeliveryInfo,
    deliveryOptions: DeliveryOptions,
    result: DeliveryCalculationResult
  ): string {
    const { breakdown, estimatedDeliveryDays, totalShippingFee } = result;
    
    let summary = `Delivery to: ${deliveryInfo.recipientName}\n`;
    summary += `Address: ${deliveryInfo.address}, ${breakdown.location}\n`;
    summary += `Phone: ${deliveryInfo.phone}\n`;
    summary += `Email: ${deliveryInfo.email}\n`;
    
    if (deliveryOptions.isRushOrder && breakdown.rushFeePerItem > 0) {
      summary += `Rush delivery: Same day\n`;
    } else {
      summary += `Estimated delivery: ${estimatedDeliveryDays} business day${estimatedDeliveryDays > 1 ? 's' : ''}\n`;
    }
    
    summary += `Shipping fee: ${this.formatDeliveryFee(totalShippingFee)}`;
    
    if (result.freeShippingDiscount > 0) {
      summary += ` (${this.formatDeliveryFee(result.freeShippingDiscount)} discount applied)`;
    }
    
    if (deliveryOptions.deliveryInstructions) {
      summary += `\nDelivery instructions: ${deliveryOptions.deliveryInstructions}`;
    }
    
    return summary;
  }

  /**
   * Check if free shipping is applicable
   */
  isFreeShippingApplicable(subtotal: number): boolean {
    return subtotal >= this.getFreeShippingThreshold();
  }

  /**
   * Get delivery warnings for the user
   */
  getDeliveryWarnings(
    deliveryInfo: CheckoutDeliveryInfo,
    deliveryOptions: DeliveryOptions,
    items: OrderItem[]
  ): string[] {
    const warnings: string[] = [];
    
    // Rush delivery warning
    if (deliveryOptions.isRushOrder && !this.isRushDeliveryAvailable(deliveryInfo.province, deliveryInfo.city)) {
      warnings.push('Rush delivery is only available for inner districts of Hanoi and Ho Chi Minh City');
    }
    
    // Heavy order warning
    const totalWeight = items.reduce((total, item) => {
      const weightPerItem = 0.2; // Default weight
      return total + (weightPerItem * item.quantity);
    }, 0);
    
    if (totalWeight > 10) {
      warnings.push('Large orders may require special handling and additional delivery time');
    }
    
    // Free shipping eligibility
    const subtotal = items.reduce((total, item) => total + item.subtotal, 0);
    const remaining = this.calculateRemainingForFreeShipping(subtotal);
    if (remaining > 0 && remaining <= 20000) {
      warnings.push(`Add ${this.formatDeliveryFee(remaining)} more to get free shipping!`);
    }
    
    return warnings;
  }
}

// Create singleton instance
export const deliveryService = new DeliveryService();

// Export default
export default deliveryService;