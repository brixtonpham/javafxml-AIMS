import React, { useState, useRef, useEffect } from 'react';
import { ChevronDownIcon, ArrowUpIcon, ArrowDownIcon } from '@heroicons/react/24/outline';
import { motion, AnimatePresence } from 'framer-motion';

type SortField = 'title' | 'price' | 'category' | 'entryDate' | 'quantity';
type SortOrder = 'ASC' | 'DESC';

interface SortOption {
  field: SortField;
  order: SortOrder;
  label: string;
  icon?: React.ReactNode;
}

interface SortingControlsProps {
  value: { field: SortField; order: SortOrder };
  onChange: (sort: { field: SortField; order: SortOrder }) => void;
  options?: SortOption[];
  className?: string;
}

const defaultSortOptions: SortOption[] = [
  {
    field: 'entryDate',
    order: 'DESC',
    label: 'Newest First',
    icon: <ArrowDownIcon className="w-4 h-4" />,
  },
  {
    field: 'entryDate',
    order: 'ASC',
    label: 'Oldest First',
    icon: <ArrowUpIcon className="w-4 h-4" />,
  },
  {
    field: 'title',
    order: 'ASC',
    label: 'Name A-Z',
    icon: <ArrowUpIcon className="w-4 h-4" />,
  },
  {
    field: 'title',
    order: 'DESC',
    label: 'Name Z-A',
    icon: <ArrowDownIcon className="w-4 h-4" />,
  },
  {
    field: 'price',
    order: 'ASC',
    label: 'Price: Low to High',
    icon: <ArrowUpIcon className="w-4 h-4" />,
  },
  {
    field: 'price',
    order: 'DESC',
    label: 'Price: High to Low',
    icon: <ArrowDownIcon className="w-4 h-4" />,
  },
  {
    field: 'quantity',
    order: 'DESC',
    label: 'Stock: High to Low',
    icon: <ArrowDownIcon className="w-4 h-4" />,
  },
  {
    field: 'category',
    order: 'ASC',
    label: 'Category A-Z',
    icon: <ArrowUpIcon className="w-4 h-4" />,
  },
];

const SortingControls: React.FC<SortingControlsProps> = ({
  value,
  onChange,
  options = defaultSortOptions,
  className = '',
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  // Find current selected option
  const selectedOption = options.find(
    option => option.field === value.field && option.order === value.order
  ) || options[0];

  // Handle option selection
  const handleOptionSelect = (option: SortOption) => {
    onChange({ field: option.field, order: option.order });
    setIsOpen(false);
  };

  // Quick sort toggle for same field
  const handleQuickToggle = (field: SortField) => {
    if (value.field === field) {
      // Toggle order for same field
      const newOrder = value.order === 'ASC' ? 'DESC' : 'ASC';
      onChange({ field, order: newOrder });
    } else {
      // Set new field with default order
      const defaultOrder = field === 'price' || field === 'quantity' ? 'DESC' : 'ASC';
      onChange({ field, order: defaultOrder });
    }
  };

  // Group options by field for better organization
  const groupedOptions = options.reduce((acc, option) => {
    if (!acc[option.field]) {
      acc[option.field] = [];
    }
    acc[option.field].push(option);
    return acc;
  }, {} as Record<SortField, SortOption[]>);

  const getFieldDisplayName = (field: SortField) => {
    const fieldNames: Record<SortField, string> = {
      title: 'Name',
      price: 'Price',
      category: 'Category',
      entryDate: 'Date',
      quantity: 'Stock',
    };
    return fieldNames[field];
  };

  return (
    <div className={`relative ${className}`} ref={dropdownRef}>
      {/* Main Dropdown */}
      <div className="flex items-center space-x-2">
        {/* Sort Label */}
        <span className="text-sm font-medium text-gray-700 hidden sm:block">
          Sort by:
        </span>

        {/* Dropdown Button */}
        <button
          onClick={() => setIsOpen(!isOpen)}
          className="flex items-center justify-between px-3 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent transition-colors min-w-[150px]"
        >
          <div className="flex items-center space-x-2">
            {selectedOption.icon}
            <span className="truncate">{selectedOption.label}</span>
          </div>
          <ChevronDownIcon
            className={`w-4 h-4 text-gray-400 transition-transform duration-200 ${
              isOpen ? 'transform rotate-180' : ''
            }`}
          />
        </button>

        {/* Quick Sort Buttons (Desktop) */}
        <div className="hidden lg:flex items-center space-x-1 ml-4">
          {(['title', 'price', 'entryDate'] as SortField[]).map((field) => (
            <button
              key={field}
              onClick={() => handleQuickToggle(field)}
              className={`
                flex items-center space-x-1 px-2 py-1 text-xs font-medium rounded transition-colors
                ${value.field === field 
                  ? 'bg-primary-100 text-primary-700 border border-primary-200' 
                  : 'text-gray-600 hover:bg-gray-100 border border-transparent'
                }
              `}
            >
              <span>{getFieldDisplayName(field)}</span>
              {value.field === field && (
                value.order === 'ASC' ? (
                  <ArrowUpIcon className="w-3 h-3" />
                ) : (
                  <ArrowDownIcon className="w-3 h-3" />
                )
              )}
            </button>
          ))}
        </div>
      </div>

      {/* Dropdown Menu */}
      <AnimatePresence>
        {isOpen && (
          <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            transition={{ duration: 0.15 }}
            className="absolute z-50 mt-1 w-full sm:w-64 bg-white border border-gray-200 rounded-lg shadow-lg max-h-80 overflow-y-auto"
          >
            <div className="py-1">
              {Object.entries(groupedOptions).map(([field, fieldOptions]) => (
                <div key={field}>
                  {/* Field Group Header */}
                  <div className="px-3 py-2 text-xs font-semibold text-gray-500 uppercase tracking-wider bg-gray-50 border-b border-gray-100">
                    {getFieldDisplayName(field as SortField)}
                  </div>
                  
                  {/* Field Options */}
                  {fieldOptions.map((option, index) => {
                    const isSelected = option.field === value.field && option.order === value.order;
                    return (
                      <button
                        key={`${option.field}-${option.order}`}
                        onClick={() => handleOptionSelect(option)}
                        className={`
                          w-full flex items-center justify-between px-3 py-2 text-sm text-left
                          hover:bg-gray-50 focus:bg-gray-50 focus:outline-none transition-colors
                          ${isSelected ? 'bg-primary-50 text-primary-700' : 'text-gray-900'}
                        `}
                      >
                        <div className="flex items-center space-x-2">
                          {option.icon}
                          <span>{option.label}</span>
                        </div>
                        {isSelected && (
                          <div className="w-2 h-2 bg-primary-500 rounded-full" />
                        )}
                      </button>
                    );
                  })}
                </div>
              ))}
            </div>

            {/* Footer */}
            <div className="px-3 py-2 border-t border-gray-100 bg-gray-50">
              <div className="text-xs text-gray-500">
                Click field names above for quick sorting
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};

export default SortingControls;