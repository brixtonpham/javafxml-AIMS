import React, { useState, useEffect, useRef } from 'react';
import { MagnifyingGlassIcon, XMarkIcon } from '@heroicons/react/24/outline';
import { Input } from '../ui';

interface SearchBarProps {
  onSearch: (query: string) => void;
  placeholder?: string;
  initialValue?: string;
  debounceMs?: number;
  showSuggestions?: boolean;
  suggestions?: string[];
  onSuggestionClick?: (suggestion: string) => void;
  className?: string;
}

const SearchBar: React.FC<SearchBarProps> = ({
  onSearch,
  placeholder = 'Search products...',
  initialValue = '',
  debounceMs = 300,
  showSuggestions = false,
  suggestions = [],
  onSuggestionClick,
  className = '',
}) => {
  const [query, setQuery] = useState(initialValue);
  const [isFocused, setIsFocused] = useState(false);
  const [showSuggestionsList, setShowSuggestionsList] = useState(false);
  const debounceRef = useRef<NodeJS.Timeout | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const suggestionRef = useRef<HTMLDivElement>(null);

  // Debounced search effect
  useEffect(() => {
    if (debounceRef.current) {
      clearTimeout(debounceRef.current);
    }

    debounceRef.current = setTimeout(() => {
      onSearch(query.trim());
    }, debounceMs);

    return () => {
      if (debounceRef.current) {
        clearTimeout(debounceRef.current);
      }
    };
  }, [query, onSearch, debounceMs]);

  // Handle input change
  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    setQuery(value);
    
    if (showSuggestions && value.trim()) {
      setShowSuggestionsList(true);
    } else {
      setShowSuggestionsList(false);
    }
  };

  // Handle clear search
  const handleClear = () => {
    setQuery('');
    setShowSuggestionsList(false);
    inputRef.current?.focus();
  };

  // Handle suggestion click
  const handleSuggestionClick = (suggestion: string) => {
    setQuery(suggestion);
    setShowSuggestionsList(false);
    onSuggestionClick?.(suggestion);
    inputRef.current?.blur();
  };

  // Handle input focus
  const handleFocus = () => {
    setIsFocused(true);
    if (showSuggestions && query.trim() && suggestions.length > 0) {
      setShowSuggestionsList(true);
    }
  };

  // Handle input blur
  const handleBlur = () => {
    setIsFocused(false);
    // Delay hiding suggestions to allow clicking
    setTimeout(() => {
      setShowSuggestionsList(false);
    }, 150);
  };

  // Handle keyboard navigation
  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Escape') {
      setShowSuggestionsList(false);
      inputRef.current?.blur();
    } else if (e.key === 'Enter') {
      setShowSuggestionsList(false);
      onSearch(query.trim());
    }
  };

  // Filter suggestions based on query
  const filteredSuggestions = suggestions.filter(suggestion =>
    suggestion.toLowerCase().includes(query.toLowerCase())
  ).slice(0, 8); // Limit to 8 suggestions

  return (
    <div className={`relative ${className}`}>
      <Input
        ref={inputRef}
        type="text"
        value={query}
        onChange={handleInputChange}
        onFocus={handleFocus}
        onBlur={handleBlur}
        onKeyDown={handleKeyDown}
        placeholder={placeholder}
        leftIcon={<MagnifyingGlassIcon className="w-5 h-5" />}
        rightIcon={
          query && (
            <button
              onClick={handleClear}
              className="hover:text-gray-600 transition-colors"
              type="button"
            >
              <XMarkIcon className="w-5 h-5" />
            </button>
          )
        }
        className={`${isFocused ? 'ring-2 ring-primary-500' : ''}`}
        fullWidth
      />

      {/* Search Suggestions */}
      {showSuggestions && showSuggestionsList && filteredSuggestions.length > 0 && (
        <div
          ref={suggestionRef}
          className="absolute top-full left-0 right-0 mt-1 bg-white border border-gray-200 rounded-lg shadow-lg z-50 max-h-64 overflow-y-auto"
        >
          <div className="py-1">
            {filteredSuggestions.map((suggestion, index) => (
              <button
                key={index}
                onClick={() => handleSuggestionClick(suggestion)}
                className="w-full px-3 py-2 text-left hover:bg-gray-50 focus:bg-gray-50 focus:outline-none transition-colors"
              >
                <div className="flex items-center">
                  <MagnifyingGlassIcon className="w-4 h-4 text-gray-400 mr-2 flex-shrink-0" />
                  <span className="text-sm text-gray-900 truncate">
                    {suggestion}
                  </span>
                </div>
              </button>
            ))}
          </div>
          
          {/* Search History or Popular Searches Footer */}
          <div className="border-t border-gray-100 px-3 py-2">
            <div className="text-xs text-gray-500 flex items-center justify-between">
              <span>Press Enter to search</span>
              <span>ESC to close</span>
            </div>
          </div>
        </div>
      )}

      {/* Search Results Count or Status */}
      {query.trim() && (
        <div className="absolute -bottom-5 left-0 text-xs text-gray-500">
          Searching for "{query}"...
        </div>
      )}
    </div>
  );
};

export default SearchBar;