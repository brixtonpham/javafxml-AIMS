import React, { useState, useEffect, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import AppLayout from '../components/layout/AppLayout';
import { Button, Card } from '../components/ui';
import {
  ProductCard,
  ProductGrid,
  SearchBar,
  CategoryFilter,
  PriceRangeFilter,
  SortingControls,
  Pagination,
  ProductTypeFilter,
} from '../components/products';
import { useProducts, useProductFilters, useInfiniteProducts, useProductCategories } from '../hooks';

interface ProductListPageProps {
  enableInfiniteScroll?: boolean;
}

const ProductListPage: React.FC<ProductListPageProps> = ({ 
  enableInfiniteScroll = false 
}) => {
  const [searchParams] = useSearchParams();
  const [isMobileFiltersOpen, setIsMobileFiltersOpen] = useState(false);
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');
  
  const {
    filters,
    updateSearch,
    updateCategory,
    updateProductType,
    updatePriceRange,
    updateSorting,
    clearFilters,
    hasActiveFilters,
    activeFilterCount,
  } = useProductFilters();

  // Category filter expects array but our hook returns string
  const handleCategoryChange = (categories: string[]) => {
    updateCategory(categories[0] || '');
  };

  // Price range filter needs proper structure
  const priceRange = {
    min: 0,
    max: 10000000, // 10M VND
  };

  const priceValue = {
    min: filters.minPrice || 0,
    max: filters.maxPrice || 10000000,
  };

  const handlePriceChange = (range: { min: number; max: number }) => {
    updatePriceRange(range.min, range.max);
  };

  // Sorting controls need proper structure
  const handleSortingChange = (sort: { field: any; order: any }) => {
    updateSorting(sort.field, sort.order);
  };

  // Use either infinite scroll or pagination based on prop
  const infiniteQuery = useInfiniteProducts(filters, {
    enabled: enableInfiniteScroll,
  });

  const paginatedQuery = useProducts(filters, {
    enabled: !enableInfiniteScroll,
  });

  const { data: categories } = useProductCategories();

  // Choose the appropriate query result
  const query = enableInfiniteScroll ? infiniteQuery : paginatedQuery;
  const products = enableInfiniteScroll ? infiniteQuery.products : paginatedQuery.products;
  const isLoading = query.isLoading;
  const error = query.error;

  // Infinite scroll observer
  const [loadMoreRef, setLoadMoreRef] = useState<HTMLDivElement | null>(null);

  useEffect(() => {
    if (!enableInfiniteScroll || !loadMoreRef || !infiniteQuery.hasNextPage || infiniteQuery.isFetchingNextPage) {
      return;
    }

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          infiniteQuery.fetchNextPage();
        }
      },
      { threshold: 0.1 }
    );

    observer.observe(loadMoreRef);
    return () => observer.disconnect();
  }, [enableInfiniteScroll, loadMoreRef, infiniteQuery.hasNextPage, infiniteQuery.isFetchingNextPage, infiniteQuery]);

  // Handle search with debouncing
  const handleSearch = useCallback((keyword: string) => {
    updateSearch(keyword);
  }, [updateSearch]);

  // Handle price range changes
  const handlePriceRangeChange = useCallback((min?: number, max?: number) => {
    updatePriceRange(min, max);
  }, [updatePriceRange]);

  // Mobile filter toggle
  const toggleMobileFilters = () => {
    setIsMobileFiltersOpen(!isMobileFiltersOpen);
  };

  // Error boundary for product loading
  if (error) {
    return (
      <AppLayout title="AIMS - Products">
        <div className="min-h-screen flex items-center justify-center">
          <Card className="text-center p-8">
            <h2 className="text-xl font-semibold text-gray-900 mb-4">
              Failed to Load Products
            </h2>
            <p className="text-gray-600 mb-6">
              We're having trouble loading the product catalog. Please try again later.
            </p>
            <Button onClick={() => window.location.reload()}>
              Retry
            </Button>
          </Card>
        </div>
      </AppLayout>
    );
  }

  return (
    <AppLayout title="AIMS - Browse Products">
      <div className="min-h-screen bg-gray-50">
        {/* Header Section */}
        <div className="bg-white shadow-sm border-b">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
            <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4">
              <div>
                <h1 className="text-2xl font-bold text-gray-900">
                  Browse Products
                </h1>
                <p className="text-gray-600 mt-1">
                  Discover books, CDs, DVDs, and LPs in our collection
                </p>
              </div>
              
              {/* Search Bar */}
              <div className="w-full lg:w-96">
                <SearchBar
                  onSearch={handleSearch}
                  placeholder="Search products..."
                  initialValue={filters.keyword || ''}
                />
              </div>
            </div>
          </div>
        </div>

        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
          <div className="flex flex-col lg:flex-row gap-6">
            {/* Sidebar Filters - Desktop */}
            <div className="hidden lg:block w-80 flex-shrink-0">
              <div className="bg-white rounded-lg shadow-sm p-6 sticky top-6">
                <div className="flex items-center justify-between mb-6">
                  <h3 className="text-lg font-semibold text-gray-900">Filters</h3>
                  {hasActiveFilters && (
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={clearFilters}
                      className="text-sm"
                    >
                      Clear All
                    </Button>
                  )}
                </div>

                <div className="space-y-6">
                  {/* Product Type Filter */}
                  <ProductTypeFilter
                    selectedType={filters.productType}
                    onTypeChange={updateProductType}
                  />

                  {/* Category Filter */}
                  <CategoryFilter
                    selectedCategories={filters.category ? [filters.category] : []}
                    onCategoryChange={handleCategoryChange}
                    categories={categories || []}
                  />

                  {/* Price Range Filter */}
                  <PriceRangeFilter
                    range={priceRange}
                    value={priceValue}
                    onChange={handlePriceChange}
                  />
                </div>
              </div>
            </div>

            {/* Main Content */}
            <div className="flex-1 min-w-0">
              {/* Mobile Filter Bar */}
              <div className="lg:hidden mb-4">
                <div className="bg-white rounded-lg shadow-sm p-4 flex items-center justify-between">
                  <Button
                    variant="outline"
                    onClick={toggleMobileFilters}
                    className="flex items-center gap-2"
                  >
                    <span>🔍</span>
                    <span>Filters</span>
                    {activeFilterCount > 0 && (
                      <span className="bg-blue-100 text-blue-800 text-xs px-2 py-1 rounded-full">
                        {activeFilterCount}
                      </span>
                    )}
                  </Button>

                  <div className="flex items-center gap-2">
                    <Button
                      variant={viewMode === 'grid' ? 'primary' : 'outline'}
                      size="sm"
                      onClick={() => setViewMode('grid')}
                    >
                      ⊞
                    </Button>
                    <Button
                      variant={viewMode === 'list' ? 'primary' : 'outline'}
                      size="sm"
                      onClick={() => setViewMode('list')}
                    >
                      ☰
                    </Button>
                  </div>
                </div>

                {/* Mobile Filters Panel */}
                {isMobileFiltersOpen && (
                  <div className="bg-white rounded-lg shadow-sm p-4 mt-4">
                    <div className="space-y-4">
                      <ProductTypeFilter
                        selectedType={filters.productType}
                        onTypeChange={updateProductType}
                      />
                      <CategoryFilter
                        selectedCategories={filters.category ? [filters.category] : []}
                        onCategoryChange={handleCategoryChange}
                        categories={categories || []}
                      />
                      <PriceRangeFilter
                        range={priceRange}
                        value={priceValue}
                        onChange={handlePriceChange}
                      />
                      {hasActiveFilters && (
                        <Button
                          variant="outline"
                          onClick={clearFilters}
                          className="w-full"
                        >
                          Clear All Filters
                        </Button>
                      )}
                    </div>
                  </div>
                )}
              </div>

              {/* Sort and Results Header */}
              <div className="bg-white rounded-lg shadow-sm p-4 mb-6">
                <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                  <div className="text-sm text-gray-600">
                    {isLoading ? (
                      'Loading products...'
                    ) : enableInfiniteScroll ? (
                      `Showing ${products.length} of ${infiniteQuery.totalProducts} products`
                    ) : (
                      `Showing ${products.length} of ${paginatedQuery.pagination.total} products`
                    )}
                  </div>

                  <SortingControls
                    value={{
                      field: (filters.sortBy || 'entryDate') as any,
                      order: (filters.sortOrder || 'DESC') as any
                    }}
                    onChange={handleSortingChange}
                  />
                </div>
              </div>

              {/* Products Grid/List */}
              {isLoading ? (
                <ProductGrid products={[]} loading={true} />
              ) : products.length === 0 ? (
                <Card className="text-center p-12">
                  <div className="text-gray-400 text-4xl mb-4">🔍</div>
                  <h3 className="text-lg font-semibold text-gray-900 mb-2">
                    No products found
                  </h3>
                  <p className="text-gray-600 mb-4">
                    Try adjusting your search or filter criteria
                  </p>
                  {hasActiveFilters && (
                    <Button onClick={clearFilters}>
                      Clear Filters
                    </Button>
                  )}
                </Card>
              ) : (
                <div className={viewMode === 'grid' ? '' : 'space-y-4'}>
                  <ProductGrid 
                    products={products}
                    className={viewMode === 'list' ? 'grid-cols-1' : ''}
                  />
                </div>
              )}

              {/* Pagination or Load More */}
              {!isLoading && products.length > 0 && (
                <div className="mt-8">
                  {enableInfiniteScroll ? (
                    <>
                      {infiniteQuery.hasNextPage && (
                        <div
                          ref={setLoadMoreRef}
                          className="flex justify-center py-4"
                        >
                          {infiniteQuery.isFetchingNextPage ? (
                            <div className="flex items-center gap-2 text-gray-600">
                              <div className="animate-spin h-4 w-4 border-2 border-blue-500 border-t-transparent rounded-full" />
                              <span>Loading more products...</span>
                            </div>
                          ) : (
                            <Button onClick={() => infiniteQuery.fetchNextPage()}>
                              Load More Products
                            </Button>
                          )}
                        </div>
                      )}
                    </>
                  ) : (
                    <Pagination
                      pagination={paginatedQuery.pagination}
                      onPageChange={(page) => {
                        window.scrollTo({ top: 0, behavior: 'smooth' });
                      }}
                      infiniteScroll={false}
                    />
                  )}
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </AppLayout>
  );
};

export default ProductListPage;