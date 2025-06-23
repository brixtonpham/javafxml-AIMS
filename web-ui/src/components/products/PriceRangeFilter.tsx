import React, { useState, useEffect, useRef } from 'react';
import { motion } from 'framer-motion';

interface PriceRange {
  min: number;
  max: number;
}

interface PriceRangeFilterProps {
  range: PriceRange;
  value: PriceRange;
  onChange: (range: PriceRange) => void;
  step?: number;
  currency?: string;
  debounceMs?: number;
  className?: string;
}

const PriceRangeFilter: React.FC<PriceRangeFilterProps> = ({
  range,
  value,
  onChange,
  step = 10000, // 10k VND steps
  currency = 'VND',
  debounceMs = 300,
  className = '',
}) => {
  const [localValue, setLocalValue] = useState(value);
  const [isDragging, setIsDragging] = useState<'min' | 'max' | null>(null);
  const debounceRef = useRef<NodeJS.Timeout | null>(null);
  const sliderRef = useRef<HTMLDivElement>(null);

  // Debounced onChange
  useEffect(() => {
    if (debounceRef.current) {
      clearTimeout(debounceRef.current);
    }

    debounceRef.current = setTimeout(() => {
      if (localValue.min !== value.min || localValue.max !== value.max) {
        onChange(localValue);
      }
    }, debounceMs);

    return () => {
      if (debounceRef.current) {
        clearTimeout(debounceRef.current);
      }
    };
  }, [localValue, onChange, debounceMs, value]);

  // Update local value when prop value changes
  useEffect(() => {
    setLocalValue(value);
  }, [value]);

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: currency,
    }).format(price);
  };

  const getPercentage = (val: number) => {
    return ((val - range.min) / (range.max - range.min)) * 100;
  };

  const getValueFromPercentage = (percentage: number) => {
    const value = range.min + (percentage / 100) * (range.max - range.min);
    return Math.round(value / step) * step;
  };

  const handleSliderClick = (e: React.MouseEvent<HTMLDivElement>) => {
    if (!sliderRef.current || isDragging) return;

    const rect = sliderRef.current.getBoundingClientRect();
    const percentage = ((e.clientX - rect.left) / rect.width) * 100;
    const newValue = getValueFromPercentage(Math.max(0, Math.min(100, percentage)));

    // Determine which handle to move based on proximity
    const minDistance = Math.abs(newValue - localValue.min);
    const maxDistance = Math.abs(newValue - localValue.max);

    if (minDistance < maxDistance) {
      setLocalValue(prev => ({
        ...prev,
        min: Math.min(newValue, prev.max - step)
      }));
    } else {
      setLocalValue(prev => ({
        ...prev,
        max: Math.max(newValue, prev.min + step)
      }));
    }
  };

  const handleMouseDown = (handle: 'min' | 'max') => (e: React.MouseEvent) => {
    e.preventDefault();
    setIsDragging(handle);
  };

  useEffect(() => {
    const handleMouseMove = (e: MouseEvent) => {
      if (!isDragging || !sliderRef.current) return;

      const rect = sliderRef.current.getBoundingClientRect();
      const percentage = ((e.clientX - rect.left) / rect.width) * 100;
      const newValue = getValueFromPercentage(Math.max(0, Math.min(100, percentage)));

      if (isDragging === 'min') {
        setLocalValue(prev => ({
          ...prev,
          min: Math.min(newValue, prev.max - step)
        }));
      } else {
        setLocalValue(prev => ({
          ...prev,
          max: Math.max(newValue, prev.min + step)
        }));
      }
    };

    const handleMouseUp = () => {
      setIsDragging(null);
    };

    if (isDragging) {
      document.addEventListener('mousemove', handleMouseMove);
      document.addEventListener('mouseup', handleMouseUp);
    }

    return () => {
      document.removeEventListener('mousemove', handleMouseMove);
      document.removeEventListener('mouseup', handleMouseUp);
    };
  }, [isDragging, step]);

  const handleInputChange = (type: 'min' | 'max') => (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = Number(e.target.value.replace(/[^\d]/g, ''));
    
    if (type === 'min') {
      setLocalValue(prev => ({
        ...prev,
        min: Math.min(value, prev.max - step)
      }));
    } else {
      setLocalValue(prev => ({
        ...prev,
        max: Math.max(value, prev.min + step)
      }));
    }
  };

  const minPercentage = getPercentage(localValue.min);
  const maxPercentage = getPercentage(localValue.max);

  return (
    <div className={`space-y-4 ${className}`}>
      {/* Price Input Fields */}
      <div className="flex items-center space-x-4">
        <div className="flex-1">
          <label className="block text-xs font-medium text-gray-700 mb-1">
            Min Price
          </label>
          <input
            type="text"
            value={localValue.min.toLocaleString()}
            onChange={handleInputChange('min')}
            className="w-full px-3 py-2 text-sm border border-gray-300 rounded focus:outline-none focus:ring-1 focus:ring-primary-500 focus:border-transparent"
          />
        </div>
        <div className="pt-6 text-gray-400">-</div>
        <div className="flex-1">
          <label className="block text-xs font-medium text-gray-700 mb-1">
            Max Price
          </label>
          <input
            type="text"
            value={localValue.max.toLocaleString()}
            onChange={handleInputChange('max')}
            className="w-full px-3 py-2 text-sm border border-gray-300 rounded focus:outline-none focus:ring-1 focus:ring-primary-500 focus:border-transparent"
          />
        </div>
      </div>

      {/* Slider */}
      <div className="px-2">
        <div
          ref={sliderRef}
          className="relative h-6 flex items-center cursor-pointer"
          onClick={handleSliderClick}
        >
          {/* Track */}
          <div className="absolute w-full h-2 bg-gray-200 rounded" />
          
          {/* Active Range */}
          <div
            className="absolute h-2 bg-primary-500 rounded"
            style={{
              left: `${minPercentage}%`,
              width: `${maxPercentage - minPercentage}%`,
            }}
          />

          {/* Min Handle */}
          <motion.div
            className={`
              absolute w-5 h-5 bg-white border-2 border-primary-500 rounded-full cursor-grab
              transform -translate-x-1/2 shadow-sm hover:shadow-md transition-shadow
              ${isDragging === 'min' ? 'cursor-grabbing scale-110' : ''}
            `}
            style={{ left: `${minPercentage}%` }}
            onMouseDown={handleMouseDown('min')}
            whileHover={{ scale: 1.1 }}
            whileTap={{ scale: 0.95 }}
          />

          {/* Max Handle */}
          <motion.div
            className={`
              absolute w-5 h-5 bg-white border-2 border-primary-500 rounded-full cursor-grab
              transform -translate-x-1/2 shadow-sm hover:shadow-md transition-shadow
              ${isDragging === 'max' ? 'cursor-grabbing scale-110' : ''}
            `}
            style={{ left: `${maxPercentage}%` }}
            onMouseDown={handleMouseDown('max')}
            whileHover={{ scale: 1.1 }}
            whileTap={{ scale: 0.95 }}
          />
        </div>

        {/* Range Labels */}
        <div className="flex justify-between text-xs text-gray-500 mt-2">
          <span>{formatPrice(range.min)}</span>
          <span>{formatPrice(range.max)}</span>
        </div>
      </div>

      {/* Current Selection Display */}
      <div className="text-center">
        <div className="text-sm font-medium text-gray-900">
          {formatPrice(localValue.min)} - {formatPrice(localValue.max)}
        </div>
        <div className="text-xs text-gray-500">
          Selected price range
        </div>
      </div>

      {/* Quick Preset Buttons */}
      <div className="grid grid-cols-3 gap-2">
        {[
          { label: 'Under 100k', range: { min: range.min, max: 100000 } },
          { label: '100k - 500k', range: { min: 100000, max: 500000 } },
          { label: 'Over 500k', range: { min: 500000, max: range.max } },
        ].map((preset, index) => (
          <button
            key={index}
            onClick={() => setLocalValue(preset.range)}
            className="px-3 py-2 text-xs font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 rounded transition-colors"
          >
            {preset.label}
          </button>
        ))}
      </div>
    </div>
  );
};

export default PriceRangeFilter;