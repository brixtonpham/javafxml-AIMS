import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { XMarkIcon, MagnifyingGlassIcon, FunnelIcon, Bars3Icon, Squares2X2Icon } from '@heroicons/react/24/outline';
import AppLayout from '../components/layout/AppLayout';
import { Card, Button, Input } from '../components/ui';
import {
  ProductGrid,
  ProductTypeFilter,
  CategoryFilter,
  PriceRangeFilter,
  SortingControls,
  Pagination
} from '../components/products';
import { useProducts, useInfiniteProducts, useProductCacheInvalidation } from '../hooks/useProducts';
import { useCart } from '../hooks/useCart';
import { useQuery } from '@tanstack/react-query';
import { productService } from '../services/productService';
import { showToast } from '../components/common/Toast';
import type { ProductFilters as Filters } from '../hooks/useProducts';

interface ProductListPageProps {
  enableInfiniteScroll?: boolean;
}

const ProductListPage: React.FC<ProductListPageProps> = ({ 
  enableInfiniteScroll = false 
}) => {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const { addToCart } = useCart();
  const { invalidateAllProductCaches, forceRefreshProduct } = useProductCacheInvalidation();
  
  // UI State
  const [loadingProducts, setLoadingProducts] = useState<string[]>([]);
  const [showMobileFilters, setShowMobileFilters] = useState(false);
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');

  // Parse filters from URL including page
  const filters: Filters = {
    keyword: searchParams.get('keyword') ?? undefined,
    category: searchParams.get('category') ?? undefined,
    productType: searchParams.get('productType') ?? undefined,
    minPrice: searchParams.get('minPrice') ? Number(searchParams.get('minPrice')) : undefined,
    maxPrice: searchParams.get('maxPrice') ? Number(searchParams.get('maxPrice')) : undefined,
    sortBy: (searchParams.get('sortBy') as Filters['sortBy']) ?? 'entryDate',
    sortOrder: (searchParams.get('sortOrder') as Filters['sortOrder']) ?? 'DESC',
  };

  // Page state is handled through pagination object from query

  // Fetch categories and price range
  const { data: categoriesData } = useQuery({
    queryKey: ['categories'],
    queryFn: () => productService.getCategories(),
    staleTime: 10 * 60 * 1000, // 10 minutes
  });

  const { data: priceRangeData } = useQuery({
    queryKey: ['price-range'],
    queryFn: async () => {
      // Mock price range - in real app, this would come from API
      return { min: 0, max: 1000000 };
    },
    staleTime: 10 * 60 * 1000,
  });

  // Force refresh state
  const [shouldForceRefresh, setShouldForceRefresh] = useState(false);

  useEffect(() => {
    const handleVisibilityChange = () => {
      if (!document.hidden) {
        setShouldForceRefresh(true);
        invalidateAllProductCaches();
      }
    };

    document.addEventListener('visibilitychange', handleVisibilityChange);
    return () => document.removeEventListener('visibilitychange', handleVisibilityChange);
  }, [invalidateAllProductCaches]);

  // Product queries
  const infiniteQuery = useInfiniteProducts(filters, {
    enabled: enableInfiniteScroll,
    forceRefresh: shouldForceRefresh,
  });

  const paginatedQuery = useProducts(filters, {
    enabled: !enableInfiniteScroll,
    forceRefresh: shouldForceRefresh,
  });

  // Reset force refresh flag after queries are executed
  useEffect(() => {
    if (shouldForceRefresh) {
      setShouldForceRefresh(false);
    }
  }, [shouldForceRefresh]);

  // Choose the appropriate query result
  const query = enableInfiniteScroll ? infiniteQuery : paginatedQuery;
  const products = enableInfiniteScroll ? 
    (infiniteQuery.data?.pages.flatMap(page => page.items) || []) : 
    (paginatedQuery.data?.items || []);
  const isLoading = query.isLoading;
  const error = query.error;
  const pagination = enableInfiniteScroll ? null : paginatedQuery.data?.pagination;

  // Get result count text
  const getResultCountText = () => {
    if (isLoading) return 'Loading products...';
    if (enableInfiniteScroll) return `Showing ${products.length} products`;
    return `Showing ${products.length} of ${pagination?.total ?? 0} products`;
  };

  // Render products content
  const renderProductsContent = () => {
    if (isLoading) {
      return <ProductGrid products={[]} loading={true} />;
    }
    
    if (products.length === 0) {
      return (
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
      );
    }

    return (
      <div className={viewMode === 'grid' ? '' : 'space-y-4'}>
        <ProductGrid 
          products={products}
          onAddToCart={handleAddToCart}
          onViewDetails={handleViewDetails}
          loadingProducts={loadingProducts}
          className={viewMode === 'list' ? 'grid-cols-1' : ''}
        />
      </div>
    );
  };

  // Update URL parameters
  const updateURLParams = useCallback((updates: Partial<Filters & { page?: number }>) => {
    const newParams = new URLSearchParams(searchParams);
    
    Object.entries(updates).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        newParams.set(key, value.toString());
      } else {
        newParams.delete(key);
      }
    });

    // Reset page when filters change (except when explicitly setting page)
    if (!updates.page && Object.keys(updates).some(key => key !== 'page')) {
      newParams.delete('page');
    }

    setSearchParams(newParams);
    setShouldForceRefresh(true);
  }, [searchParams, setSearchParams]);

  // Filter handlers
  const handleSearchChange = useCallback((search: string) => {
    updateURLParams({ keyword: search });
  }, [updateURLParams]);

  const handleProductTypeChange = useCallback((type: string | undefined) => {
    updateURLParams({ productType: type });
  }, [updateURLParams]);

  const handleCategoryChange = useCallback((categories: string[]) => {
    updateURLParams({ category: categories.length > 0 ? categories[0] : undefined });
  }, [updateURLParams]);

  const handlePriceRangeChange = useCallback((range: { min: number; max: number }) => {
    updateURLParams({ 
      minPrice: range.min > 0 ? range.min : undefined,
      maxPrice: range.max < (priceRangeData?.max ?? 1000000) ? range.max : undefined 
    });
  }, [updateURLParams, priceRangeData]);

  const handleSortingChange = useCallback((sort: { field: string; order: string }) => {
    updateURLParams({ sortBy: sort.field as Filters['sortBy'], sortOrder: sort.order as Filters['sortOrder'] });
  }, [updateURLParams]);

  const handlePageChange = useCallback((page: number) => {
    updateURLParams({ page });
    // Force refresh immediately when page changes
    setShouldForceRefresh(true);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }, [updateURLParams]);

  const clearFilters = useCallback(() => {
    setSearchParams({});
    setShouldForceRefresh(true);
  }, [setSearchParams]);

  // Check if any filters are active
  const hasActiveFilters = !!(
    filters.keyword || 
    filters.category || 
    filters.productType || 
    filters.minPrice || 
    filters.maxPrice ||
    (filters.sortBy && filters.sortBy !== 'entryDate') ||
    (filters.sortOrder && filters.sortOrder !== 'DESC')
  );

  const activeFilterCount = [
    filters.keyword,
    filters.category,
    filters.productType,
    filters.minPrice,
    filters.maxPrice,
  ].filter(Boolean).length;

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

  // Action handlers
  const handleAddToCart = async (productId: string) => {
    setLoadingProducts(prev => [...prev, productId]);
    try {
      // Note: We don't have the full product object here, so optimistic updates
      // will be skipped and we'll rely on the server response
      await addToCart(productId, 1);
      forceRefreshProduct(productId);
      showToast.success('Success', 'Product added to cart successfully!');
    } catch (error) {
      console.error('Failed to add product to cart:', error);
      showToast.error('Error', 'Failed to add product to cart. Please try again.');
    } finally {
      setLoadingProducts(prev => prev.filter(id => id !== productId));
    }
  };

  const handleViewDetails = (productId: string) => {
    navigate(`/products/${productId}`);
  };

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
                <div className="relative">
                  <MagnifyingGlassIcon className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-gray-400" />
                  <Input
                    placeholder="Search products..."
                    value={filters.keyword ?? ''}
                    onChange={(e) => handleSearchChange(e.target.value)}
                    className="pl-10 rounded-full"
                  />
                  {filters.keyword && (
                    <button
                      onClick={() => handleSearchChange('')}
                      className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-gray-600"
                    >
                      <XMarkIcon className="h-5 w-5" />
                    </button>
                  )}
                </div>
              </div>
            </div>
          </div>
        </div>

        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
          <div className="flex flex-col lg:flex-row gap-6">
            {/* Desktop Sidebar Filters */}
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
                    onTypeChange={handleProductTypeChange}
                  />

                  {/* Category Filter */}
                  <CategoryFilter
                    selectedCategories={filters.category ? [filters.category] : []}
                    onCategoryChange={handleCategoryChange}
                    categories={categoriesData || []}
                  />

                  {/* Price Range Filter */}
                  {priceRangeData && (
                    <PriceRangeFilter
                      range={priceRangeData}
                      value={{
                        min: filters.minPrice ?? priceRangeData.min,
                        max: filters.maxPrice ?? priceRangeData.max,
                      }}
                      onChange={handlePriceRangeChange}
                    />
                  )}
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
                    onClick={() => setShowMobileFilters(!showMobileFilters)}
                    className="flex items-center gap-2"
                  >
                    <FunnelIcon className="h-4 w-4" />
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
                      <Squares2X2Icon className="h-4 w-4" />
                    </Button>
                    <Button
                      variant={viewMode === 'list' ? 'primary' : 'outline'}
                      size="sm"
                      onClick={() => setViewMode('list')}
                    >
                      <Bars3Icon className="h-4 w-4" />
                    </Button>
                  </div>
                </div>

                {/* Mobile Filters Panel */}
                {showMobileFilters && (
                  <div className="bg-white rounded-lg shadow-sm p-4 mt-4">
                    <div className="space-y-4">
                      <ProductTypeFilter
                        selectedType={filters.productType}
                        onTypeChange={handleProductTypeChange}
                      />
                      <CategoryFilter
                        selectedCategories={filters.category ? [filters.category] : []}
                        onCategoryChange={handleCategoryChange}
                        categories={categoriesData || []}
                      />
                      {priceRangeData && (
                        <PriceRangeFilter
                          range={priceRangeData}
                          value={{
                            min: filters.minPrice ?? priceRangeData.min,
                            max: filters.maxPrice ?? priceRangeData.max,
                          }}
                          onChange={handlePriceRangeChange}
                        />
                      )}
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

              {/* Results Header */}
              <div className="bg-white rounded-lg shadow-sm p-4 mb-6">
                <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                  <div className="text-sm text-gray-600">
                    {getResultCountText()}
                  </div>

                  <SortingControls
                    value={{
                      field: filters.sortBy ?? 'entryDate',
                      order: filters.sortOrder ?? 'DESC'
                    }}
                    onChange={handleSortingChange}
                  />
                </div>
              </div>

              {/* Products Grid/List */}
              {renderProductsContent()}

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
                    pagination && (
                      <Pagination
                        pagination={pagination}
                        onPageChange={handlePageChange}
                        loading={isLoading}
                      />
                    )
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
