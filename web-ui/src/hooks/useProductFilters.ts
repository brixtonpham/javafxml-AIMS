import { useSearchParams } from 'react-router-dom';
import { useCallback, useMemo } from 'react';
import type { ProductFilters } from './useProducts';

export const useProductFilters = () => {
  const [searchParams, setSearchParams] = useSearchParams();

  // Parse current filters from URL
  const filters: ProductFilters = useMemo(() => {
    const keyword = searchParams.get('q') || undefined;
    const category = searchParams.get('category') || undefined;
    const productType = searchParams.get('type') || undefined;
    const minPrice = searchParams.get('minPrice') ? parseFloat(searchParams.get('minPrice')!) : undefined;
    const maxPrice = searchParams.get('maxPrice') ? parseFloat(searchParams.get('maxPrice')!) : undefined;
    const sortBy = (searchParams.get('sortBy') as ProductFilters['sortBy']) || 'entryDate';
    const sortOrder = (searchParams.get('sortOrder') as ProductFilters['sortOrder']) || 'DESC';

    return {
      keyword,
      category,
      productType,
      minPrice,
      maxPrice,
      sortBy,
      sortOrder,
    };
  }, [searchParams]);

  // Update filters in URL
  const updateFilters = useCallback((newFilters: Partial<ProductFilters>) => {
    setSearchParams(currentParams => {
      const updatedParams = new URLSearchParams(currentParams);
      
      // Update each filter parameter
      Object.entries(newFilters).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') {
          // Map internal filter names to URL parameter names
          const paramName = key === 'keyword' ? 'q' : 
                           key === 'productType' ? 'type' : key;
          updatedParams.set(paramName, String(value));
        } else {
          // Remove parameter if value is undefined/null/empty
          const paramName = key === 'keyword' ? 'q' : 
                           key === 'productType' ? 'type' : key;
          updatedParams.delete(paramName);
        }
      });

      // Reset page to 1 when filters change (except for page updates)
      if (!('page' in newFilters)) {
        updatedParams.delete('page');
      }

      return updatedParams;
    });
  }, [setSearchParams]);

  // Clear all filters
  const clearFilters = useCallback(() => {
    setSearchParams(currentParams => {
      const updatedParams = new URLSearchParams(currentParams);
      
      // Remove all filter-related parameters but keep page if it exists
      const page = updatedParams.get('page');
      updatedParams.delete('q');
      updatedParams.delete('category');
      updatedParams.delete('type');
      updatedParams.delete('minPrice');
      updatedParams.delete('maxPrice');
      updatedParams.delete('sortBy');
      updatedParams.delete('sortOrder');
      
      // Reset to page 1
      if (page) {
        updatedParams.set('page', '1');
      }
      
      return updatedParams;
    });
  }, [setSearchParams]);

  // Update specific filter
  const updateFilter = useCallback((key: keyof ProductFilters, value: any) => {
    updateFilters({ [key]: value });
  }, [updateFilters]);

  // Update search keyword
  const updateSearch = useCallback((keyword: string) => {
    updateFilters({ keyword });
  }, [updateFilters]);

  // Update category filter
  const updateCategory = useCallback((category: string) => {
    updateFilters({ category });
  }, [updateFilters]);

  // Update product type filter
  const updateProductType = useCallback((productType: string | undefined) => {
    updateFilters({ productType });
  }, [updateFilters]);

  // Update price range
  const updatePriceRange = useCallback((minPrice?: number, maxPrice?: number) => {
    updateFilters({ minPrice, maxPrice });
  }, [updateFilters]);

  // Update sorting
  const updateSorting = useCallback((sortBy: ProductFilters['sortBy'], sortOrder: ProductFilters['sortOrder'] = 'DESC') => {
    updateFilters({ sortBy, sortOrder });
  }, [updateFilters]);

  // Check if any filters are active
  const hasActiveFilters = useMemo(() => {
    return !!(filters.keyword || filters.category || filters.productType || 
              filters.minPrice !== undefined || filters.maxPrice !== undefined ||
              (filters.sortBy && filters.sortBy !== 'entryDate') ||
              (filters.sortOrder && filters.sortOrder !== 'DESC'));
  }, [filters]);

  // Get filter count for display
  const activeFilterCount = useMemo(() => {
    let count = 0;
    if (filters.keyword) count++;
    if (filters.category) count++;
    if (filters.productType) count++;
    if (filters.minPrice !== undefined || filters.maxPrice !== undefined) count++;
    if (filters.sortBy && filters.sortBy !== 'entryDate') count++;
    return count;
  }, [filters]);

  return {
    filters,
    updateFilters,
    updateFilter,
    updateSearch,
    updateCategory,
    updateProductType,
    updatePriceRange,
    updateSorting,
    clearFilters,
    hasActiveFilters,
    activeFilterCount,
  };
};