import React from 'react';
import { Button } from '../ui';
import { useProductTypes } from '../../hooks';
import { ProductType } from '../../types';

interface ProductTypeFilterProps {
  selectedType?: string;
  onTypeChange: (type: string | undefined) => void;
  className?: string;
}

const ProductTypeFilter: React.FC<ProductTypeFilterProps> = ({
  selectedType,
  onTypeChange,
  className = '',
}) => {
  const { data: productTypes, isLoading, error } = useProductTypes();

  if (isLoading) {
    return (
      <div className={`flex flex-wrap gap-2 ${className}`}>
        {[...Array(4)].map((_, index) => (
          <div
            key={index}
            className="h-10 w-16 bg-gray-200 animate-pulse rounded-md"
          />
        ))}
      </div>
    );
  }

  if (error) {
    return (
      <div className={`text-sm text-gray-500 ${className}`}>
        Failed to load product types
      </div>
    );
  }

  const types = productTypes || Object.values(ProductType);

  const getTypeDisplayName = (type: string) => {
    switch (type) {
      case 'BOOK':
        return 'Books';
      case 'CD':
        return 'CDs';
      case 'DVD':
        return 'DVDs';
      case 'LP':
        return 'LPs';
      default:
        return type;
    }
  };

  const getTypeIcon = (type: string) => {
    switch (type) {
      case 'BOOK':
        return '📚';
      case 'CD':
        return '💿';
      case 'DVD':
        return '📀';
      case 'LP':
        return '🎵';
      default:
        return '📦';
    }
  };

  return (
    <div className={`space-y-3 ${className}`}>
      <label className="block text-sm font-medium text-gray-700">
        Product Type
      </label>
      
      <div className="flex flex-wrap gap-2">
        {/* All Types Button */}
        <Button
          variant={!selectedType ? 'primary' : 'outline'}
          size="sm"
          onClick={() => onTypeChange(undefined)}
          className="flex items-center gap-2 transition-all duration-200 hover:scale-105"
        >
          <span>🏪</span>
          <span>All</span>
        </Button>

        {/* Individual Type Buttons */}
        {types.map((type) => (
          <Button
            key={type}
            variant={selectedType === type ? 'primary' : 'outline'}
            size="sm"
            onClick={() => onTypeChange(type === selectedType ? undefined : type)}
            className="flex items-center gap-2 transition-all duration-200 hover:scale-105"
          >
            <span className="text-base">{getTypeIcon(type)}</span>
            <span>{getTypeDisplayName(type)}</span>
          </Button>
        ))}
      </div>

      {/* Mobile-optimized vertical layout for small screens */}
      <div className="sm:hidden">
        <div className="grid grid-cols-2 gap-2">
          <Button
            variant={!selectedType ? 'primary' : 'outline'}
            size="sm"
            onClick={() => onTypeChange(undefined)}
            className="flex items-center justify-center gap-2 py-3"
          >
            <span>🏪</span>
            <span>All</span>
          </Button>

          {types.map((type) => (
            <Button
              key={`mobile-${type}`}
              variant={selectedType === type ? 'primary' : 'outline'}
              size="sm"
              onClick={() => onTypeChange(type === selectedType ? undefined : type)}
              className="flex items-center justify-center gap-2 py-3"
            >
              <span className="text-base">{getTypeIcon(type)}</span>
              <span className="text-xs">{getTypeDisplayName(type)}</span>
            </Button>
          ))}
        </div>
      </div>

      {/* Active filter indicator */}
      {selectedType && (
        <div className="flex items-center gap-2 text-sm text-blue-600">
          <span>📍</span>
          <span>Filtered by: {getTypeDisplayName(selectedType)}</span>
          <button
            onClick={() => onTypeChange(undefined)}
            className="text-gray-400 hover:text-gray-600 ml-1"
            aria-label="Clear filter"
          >
            ×
          </button>
        </div>
      )}
    </div>
  );
};

export default ProductTypeFilter;