import React from 'react';
import Card from '../ui/Card';
import { Calculator, Info } from 'lucide-react';
import type { VATCalculationResult } from '../../types';

interface VATCalculationDisplayProps {
  vatResult: VATCalculationResult;
  showBreakdown?: boolean;
  className?: string;
}

export const VATCalculationDisplay: React.FC<VATCalculationDisplayProps> = ({
  vatResult,
  showBreakdown = true,
  className = ''
}) => {
  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND',
    }).format(amount);
  };

  const formatPercentage = (rate: number) => {
    return `${(rate * 100).toFixed(1)}%`;
  };

  if (!vatResult.isValid) {
    return (
      <Card className={`p-4 border-red-200 bg-red-50 ${className}`}>
        <div className="flex items-center gap-2 text-red-600">
          <Info className="w-4 h-4" />
          <span className="font-medium">VAT Calculation Error</span>
        </div>
        <p className="text-sm text-red-700 mt-1">
          Unable to calculate VAT for this order. Please try again.
        </p>
      </Card>
    );
  }

  return (
    <Card className={`p-4 ${className}`}>
      <div className="flex items-center gap-2 mb-3">
        <Calculator className="w-4 h-4 text-blue-600" />
        <h3 className="font-medium text-gray-900">VAT Calculation</h3>
      </div>

      {showBreakdown && (
        <div className="space-y-2 mb-3">
          <div className="flex justify-between text-sm">
            <span className="text-gray-600">Subtotal (excluding VAT):</span>
            <span className="font-medium">{formatCurrency(vatResult.basePrice)}</span>
          </div>
          
          <div className="flex justify-between text-sm">
            <span className="text-gray-600">
              VAT ({formatPercentage(vatResult.vatRate)}):
            </span>
            <span className="font-medium text-blue-600">
              {formatCurrency(vatResult.vatAmount)}
            </span>
          </div>
          
          <div className="border-t pt-2">
            <div className="flex justify-between">
              <span className="font-medium text-gray-900">Total (including VAT):</span>
              <span className="font-bold text-lg text-gray-900">
                {formatCurrency(vatResult.totalPrice)}
              </span>
            </div>
          </div>
        </div>
      )}

      {!showBreakdown && (
        <div className="flex justify-between items-center">
          <span className="text-sm text-gray-600">
            Total (including {formatPercentage(vatResult.vatRate)} VAT):
          </span>
          <span className="font-bold text-lg text-gray-900">
            {formatCurrency(vatResult.totalPrice)}
          </span>
        </div>
      )}

      <div className="mt-2 p-2 bg-blue-50 rounded text-xs text-blue-700">
        <div className="flex items-start gap-1">
          <Info className="w-3 h-3 mt-0.5 flex-shrink-0" />
          <span>
            VAT is automatically calculated according to Vietnamese tax regulations. 
            VAT rate may vary based on product type and customer location.
          </span>
        </div>
      </div>
    </Card>
  );
};

export default VATCalculationDisplay;