import React, { useState, useRef, useEffect } from 'react';
import { ChevronDownIcon, CheckIcon } from '@heroicons/react/24/outline';
import { motion, AnimatePresence } from 'framer-motion';

interface CategoryFilterProps {
  categories: string[];
  selectedCategories: string[];
  onCategoryChange: (categories: string[]) => void;
  loading?: boolean;
  placeholder?: string;
  maxHeight?: number;
  className?: string;
}

const CategoryFilter: React.FC<CategoryFilterProps> = ({
  categories,
  selectedCategories,
  onCategoryChange,
  loading = false,
  placeholder = 'Select categories',
  maxHeight = 300,
  className = '',
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
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

  // Filter categories based on search query
  const filteredCategories = categories.filter(category =>
    category.toLowerCase().includes(searchQuery.toLowerCase())
  );

  // Handle category selection
  const handleCategoryToggle = (category: string) => {
    const isSelected = selectedCategories.includes(category);
    let newSelection: string[];

    if (isSelected) {
      newSelection = selectedCategories.filter(c => c !== category);
    } else {
      newSelection = [...selectedCategories, category];
    }

    onCategoryChange(newSelection);
  };

  // Handle select all
  const handleSelectAll = () => {
    if (selectedCategories.length === filteredCategories.length) {
      // Deselect all filtered categories
      const newSelection = selectedCategories.filter(
        selected => !filteredCategories.includes(selected)
      );
      onCategoryChange(newSelection);
    } else {
      // Select all filtered categories
      const newSelection = [
        ...selectedCategories,
        ...filteredCategories.filter(category => !selectedCategories.includes(category))
      ];
      onCategoryChange(newSelection);
    }
  };

  // Clear all selections
  const handleClearAll = () => {
    onCategoryChange([]);
  };

  const displayText = selectedCategories.length === 0 
    ? placeholder
    : selectedCategories.length === 1
    ? selectedCategories[0]
    : `${selectedCategories.length} categories selected`;

  const allFilteredSelected = filteredCategories.length > 0 && 
    filteredCategories.every(category => selectedCategories.includes(category));

  return (
    <div className={`relative ${className}`} ref={dropdownRef}>
      {/* Trigger Button */}
      <button
        onClick={() => setIsOpen(!isOpen)}
        disabled={loading}
        className={`
          w-full flex items-center justify-between px-3 py-2 
          border border-gray-300 rounded-lg shadow-sm bg-white
          text-left focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent
          transition-colors duration-200
          ${loading ? 'opacity-50 cursor-not-allowed' : 'hover:border-gray-400'}
          ${isOpen ? 'ring-2 ring-primary-500 border-transparent' : ''}
        `}
      >
        <span className={`block truncate ${selectedCategories.length === 0 ? 'text-gray-500' : 'text-gray-900'}`}>
          {loading ? 'Loading categories...' : displayText}
        </span>
        <ChevronDownIcon 
          className={`w-5 h-5 text-gray-400 transition-transform duration-200 ${
            isOpen ? 'transform rotate-180' : ''
          }`} 
        />
      </button>

      {/* Dropdown Menu */}
      <AnimatePresence>
        {isOpen && (
          <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            transition={{ duration: 0.15 }}
            className="absolute z-50 w-full mt-1 bg-white border border-gray-200 rounded-lg shadow-lg"
            style={{ maxHeight: maxHeight + 100 }}
          >
            {/* Search Input */}
            {categories.length > 5 && (
              <div className="p-2 border-b border-gray-100">
                <input
                  type="text"
                  placeholder="Search categories..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="w-full px-3 py-2 text-sm border border-gray-200 rounded focus:outline-none focus:ring-1 focus:ring-primary-500 focus:border-transparent"
                />
              </div>
            )}

            {/* Action Buttons */}
            {filteredCategories.length > 0 && (
              <div className="p-2 border-b border-gray-100 flex gap-2">
                <button
                  onClick={handleSelectAll}
                  className="text-xs text-primary-600 hover:text-primary-700 font-medium"
                >
                  {allFilteredSelected ? 'Deselect All' : 'Select All'}
                </button>
                {selectedCategories.length > 0 && (
                  <button
                    onClick={handleClearAll}
                    className="text-xs text-gray-500 hover:text-gray-700 font-medium"
                  >
                    Clear All
                  </button>
                )}
              </div>
            )}

            {/* Categories List */}
            <div className="max-h-64 overflow-y-auto">
              {filteredCategories.length === 0 ? (
                <div className="px-3 py-4 text-sm text-gray-500 text-center">
                  {searchQuery ? 'No categories found' : 'No categories available'}
                </div>
              ) : (
                <div className="py-1">
                  {filteredCategories.map((category) => {
                    const isSelected = selectedCategories.includes(category);
                    return (
                      <button
                        key={category}
                        onClick={() => handleCategoryToggle(category)}
                        className={`
                          w-full flex items-center px-3 py-2 text-sm text-left
                          hover:bg-gray-50 focus:bg-gray-50 focus:outline-none
                          transition-colors duration-150
                          ${isSelected ? 'bg-primary-50 text-primary-700' : 'text-gray-900'}
                        `}
                      >
                        <div className={`
                          flex-shrink-0 w-4 h-4 mr-3 border rounded border-gray-300
                          flex items-center justify-center
                          ${isSelected ? 'bg-primary-500 border-primary-500' : 'bg-white'}
                        `}>
                          {isSelected && (
                            <CheckIcon className="w-3 h-3 text-white" />
                          )}
                        </div>
                        <span className="truncate">{category}</span>
                      </button>
                    );
                  })}
                </div>
              )}
            </div>

            {/* Footer */}
            {selectedCategories.length > 0 && (
              <div className="px-3 py-2 border-t border-gray-100 bg-gray-50 text-xs text-gray-600">
                {selectedCategories.length} of {categories.length} categories selected
              </div>
            )}
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};

export default CategoryFilter;