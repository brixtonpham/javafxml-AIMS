import React, { useState, useEffect, useRef, useCallback } from 'react';
import { ChevronLeftIcon, ChevronRightIcon } from '@heroicons/react/24/outline';
import { Button } from '../ui';

interface PaginationInfo {
  page: number;
  limit: number;
  total: number;
  pages: number;
  hasNext: boolean;
  hasPrev: boolean;
}

interface PaginationProps {
  pagination: PaginationInfo;
  onPageChange: (page: number) => void;
  onLoadMore?: () => void;
  loading?: boolean;
  infiniteScroll?: boolean;
  loadMoreThreshold?: number;
  showLoadMoreButton?: boolean;
  className?: string;
}

const Pagination: React.FC<PaginationProps> = ({
  pagination,
  onPageChange,
  onLoadMore,
  loading = false,
  infiniteScroll = true,
  loadMoreThreshold = 200,
  showLoadMoreButton = true,
  className = '',
}) => {
  const [isNearBottom, setIsNearBottom] = useState(false);
  const sentinelRef = useRef<HTMLDivElement>(null);
  const observerRef = useRef<IntersectionObserver | null>(null);

  // Handle infinite scroll
  const handleScroll = useCallback(() => {
    if (!infiniteScroll || loading || !pagination.hasNext) return;

    const scrollTop = window.pageYOffset || document.documentElement.scrollTop;
    const scrollHeight = document.documentElement.scrollHeight;
    const clientHeight = window.innerHeight;
    const scrolledToBottom = scrollTop + clientHeight >= scrollHeight - loadMoreThreshold;

    setIsNearBottom(scrolledToBottom);

    if (scrolledToBottom && onLoadMore) {
      onLoadMore();
    }
  }, [infiniteScroll, loading, pagination.hasNext, loadMoreThreshold, onLoadMore]);

  // Set up scroll listener
  useEffect(() => {
    if (infiniteScroll) {
      window.addEventListener('scroll', handleScroll, { passive: true });
      return () => window.removeEventListener('scroll', handleScroll);
    }
  }, [handleScroll, infiniteScroll]);

  // Set up intersection observer for better performance
  useEffect(() => {
    if (!infiniteScroll || !sentinelRef.current) return;

    observerRef.current = new IntersectionObserver(
      (entries) => {
        const [entry] = entries;
        if (entry.isIntersecting && !loading && pagination.hasNext && onLoadMore) {
          onLoadMore();
        }
      },
      { threshold: 0.1 }
    );

    observerRef.current.observe(sentinelRef.current);

    return () => {
      if (observerRef.current) {
        observerRef.current.disconnect();
      }
    };
  }, [infiniteScroll, loading, pagination.hasNext, onLoadMore]);

  // Generate page numbers for pagination
  const getPageNumbers = () => {
    const { page, pages } = pagination;
    const delta = 2; // Number of pages to show around current page
    const range = [];
    const rangeWithDots = [];

    for (
      let i = Math.max(2, page - delta);
      i <= Math.min(pages - 1, page + delta);
      i++
    ) {
      range.push(i);
    }

    if (page - delta > 2) {
      rangeWithDots.push(1, '...');
    } else {
      rangeWithDots.push(1);
    }

    rangeWithDots.push(...range);

    if (page + delta < pages - 1) {
      rangeWithDots.push('...', pages);
    } else if (pages > 1) {
      rangeWithDots.push(pages);
    }

    return rangeWithDots;
  };

  const handlePageClick = (page: number | string) => {
    if (typeof page === 'number' && page !== pagination.page && !loading) {
      onPageChange(page);
      // Scroll to top when changing pages
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  };

  const handlePrevious = () => {
    if (pagination.hasPrev && !loading) {
      onPageChange(pagination.page - 1);
    }
  };

  const handleNext = () => {
    if (pagination.hasNext && !loading) {
      onPageChange(pagination.page + 1);
    }
  };

  const handleLoadMore = () => {
    if (onLoadMore && !loading && pagination.hasNext) {
      onLoadMore();
    }
  };

  // Results info
  const startItem = (pagination.page - 1) * pagination.limit + 1;
  const endItem = Math.min(pagination.page * pagination.limit, pagination.total);

  return (
    <div className={`space-y-6 ${className}`}>
      {/* Results Info */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 text-sm text-gray-700">
        <div>
          Showing <span className="font-medium">{startItem}</span> to{' '}
          <span className="font-medium">{endItem}</span> of{' '}
          <span className="font-medium">{pagination.total}</span> results
        </div>
        
        <div className="text-gray-500">
          Page {pagination.page} of {pagination.pages}
        </div>
      </div>

      {/* Load More Button for Infinite Scroll */}
      {infiniteScroll && showLoadMoreButton && pagination.hasNext && (
        <div className="flex justify-center">
          <Button
            onClick={handleLoadMore}
            disabled={loading}
            isLoading={loading}
            variant="outline"
            size="lg"
            className="min-w-[200px]"
          >
            {loading ? 'Loading...' : 'Load More Products'}
          </Button>
        </div>
      )}

      {/* Traditional Pagination */}
      {!infiniteScroll && pagination.pages > 1 && (
        <nav className="flex items-center justify-between border-t border-gray-200 pt-6">
          {/* Previous Button */}
          <Button
            onClick={handlePrevious}
            disabled={!pagination.hasPrev || loading}
            variant="outline"
            size="sm"
            className="flex items-center"
          >
            <ChevronLeftIcon className="w-4 h-4 mr-1" />
            Previous
          </Button>

          {/* Page Numbers */}
          <div className="hidden md:flex space-x-1">
            {getPageNumbers().map((page, index) => (
              <button
                key={index}
                onClick={() => handlePageClick(page)}
                disabled={loading || page === '...'}
                className={`
                  px-3 py-2 text-sm font-medium rounded transition-colors
                  ${page === pagination.page
                    ? 'bg-primary-500 text-white'
                    : page === '...'
                    ? 'text-gray-400 cursor-not-allowed'
                    : 'text-gray-700 hover:bg-gray-100'
                  }
                  ${loading ? 'opacity-50 cursor-not-allowed' : ''}
                `}
              >
                {page}
              </button>
            ))}
          </div>

          {/* Mobile Page Info */}
          <div className="md:hidden text-sm text-gray-500">
            {pagination.page} / {pagination.pages}
          </div>

          {/* Next Button */}
          <Button
            onClick={handleNext}
            disabled={!pagination.hasNext || loading}
            variant="outline"
            size="sm"
            className="flex items-center"
          >
            Next
            <ChevronRightIcon className="w-4 h-4 ml-1" />
          </Button>
        </nav>
      )}

      {/* Jump to Page (Traditional Pagination) */}
      {!infiniteScroll && pagination.pages > 10 && (
        <div className="flex items-center justify-center gap-2 text-sm">
          <span className="text-gray-500">Jump to page:</span>
          <input
            type="number"
            min={1}
            max={pagination.pages}
            defaultValue={pagination.page}
            onKeyDown={(e) => {
              if (e.key === 'Enter') {
                const value = parseInt((e.target as HTMLInputElement).value);
                if (value >= 1 && value <= pagination.pages) {
                  handlePageClick(value);
                }
              }
            }}
            className="w-16 px-2 py-1 border border-gray-300 rounded text-center focus:outline-none focus:ring-1 focus:ring-primary-500"
          />
        </div>
      )}

      {/* Infinite Scroll Sentinel */}
      {infiniteScroll && (
        <div
          ref={sentinelRef}
          className={`h-20 flex items-center justify-center ${
            isNearBottom && loading ? 'opacity-100' : 'opacity-0'
          } transition-opacity`}
        >
          {loading && (
            <div className="flex items-center space-x-2 text-gray-600">
              <svg className="animate-spin w-5 h-5" viewBox="0 0 24 24">
                <circle
                  className="opacity-25"
                  cx="12"
                  cy="12"
                  r="10"
                  stroke="currentColor"
                  strokeWidth="4"
                  fill="none"
                />
                <path
                  className="opacity-75"
                  fill="currentColor"
                  d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                />
              </svg>
              <span className="text-sm">Loading more products...</span>
            </div>
          )}
        </div>
      )}

      {/* End of Results Message */}
      {!pagination.hasNext && pagination.total > 0 && (
        <div className="text-center py-8 text-gray-500 border-t border-gray-100">
          <p className="text-sm">You've reached the end of the results</p>
        </div>
      )}
    </div>
  );
};

export default Pagination;