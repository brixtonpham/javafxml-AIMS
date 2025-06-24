import { useQuery, useInfiniteQuery, useQueryClient } from '@tanstack/react-query';
import { useSearchParams } from 'react-router-dom';
import { productService } from '../services/productService';
import type { ProductSearchParams, Product, PaginatedResponse } from '../types';

export interface ProductFilters {
  keyword?: string;
  category?: string;
  productType?: string;
  minPrice?: number;
  maxPrice?: number;
  sortBy?: 'title' | 'price' | 'category' | 'entryDate' | 'quantity';
  sortOrder?: 'ASC' | 'DESC';
}

export interface UseProductsOptions {
  pageSize?: number;
  enabled?: boolean;
  refetchOnWindowFocus?: boolean;
  forceRefresh?: boolean; // New option to force fresh data
}

export const useProducts = (
  filters: ProductFilters = {},
  options: UseProductsOptions = {}
) => {
  const {
    pageSize = 20,
    enabled = true,
    refetchOnWindowFocus = false,
    forceRefresh = false,
  } = options;

  const [searchParams] = useSearchParams();
  const page = parseInt(searchParams.get('page') || '1', 10);

  const searchParamsFromFilters: ProductSearchParams = {
    ...filters,
    page,
    pageSize,
  };

  const query = useQuery<PaginatedResponse<Product>>({
    queryKey: ['products', searchParamsFromFilters],
    queryFn: () => productService.getProducts(searchParamsFromFilters),
    enabled,
    refetchOnWindowFocus,
    staleTime: forceRefresh ? 0 : 2 * 60 * 1000, // Reduced from 5 minutes to 2 minutes, or 0 for force refresh
    gcTime: 5 * 60 * 1000, // Reduced garbage collection time
    refetchOnMount: forceRefresh ? 'always' : undefined,
  });

  return {
    ...query,
    products: query.data?.items || [],
    pagination: query.data?.pagination || {
      page: 1,
      limit: pageSize,
      total: 0,
      pages: 0,
      hasNext: false,
      hasPrev: false,
    },
  };
};

export const useInfiniteProducts = (
  filters: ProductFilters = {},
  options: UseProductsOptions = {}
) => {
  const {
    pageSize = 20,
    enabled = true,
    refetchOnWindowFocus = false,
    forceRefresh = false,
  } = options;

  return useInfiniteQuery<PaginatedResponse<Product>>({
    queryKey: ['products', 'infinite', filters],
    queryFn: ({ pageParam = 1 }) => {
      const searchParams: ProductSearchParams = {
        ...filters,
        page: pageParam as number,
        pageSize,
      };
      return productService.getProducts(searchParams);
    },
    enabled,
    refetchOnWindowFocus,
    staleTime: forceRefresh ? 0 : 2 * 60 * 1000, // Reduced stale time
    gcTime: 5 * 60 * 1000,
    getNextPageParam: (lastPage) => {
      if (lastPage.pagination.hasNext) {
        return lastPage.pagination.page + 1;
      }
      return undefined;
    },
    initialPageParam: 1,
    refetchOnMount: forceRefresh ? 'always' : undefined,
  });
};

export const useProductCategories = () => {
  return useQuery<string[]>({
    queryKey: ['categories'],
    queryFn: () => productService.getCategories(),
    staleTime: 10 * 60 * 1000, // Categories don't change often
    gcTime: 60 * 60 * 1000, // 1 hour
  });
};

export const useProductTypes = () => {
  return useQuery<string[]>({
    queryKey: ['productTypes'],
    queryFn: () => productService.getProductTypes(),
    staleTime: 10 * 60 * 1000, // Product types don't change often
    gcTime: 60 * 60 * 1000, // 1 hour
  });
};

export const useProduct = (id: string, options: { forceRefresh?: boolean } = {}) => {
  const { forceRefresh = false } = options;
  
  return useQuery<Product>({
    queryKey: ['product', id],
    queryFn: () => productService.getProductById(id),
    enabled: !!id,
    staleTime: forceRefresh ? 0 : 30 * 1000, // Much more aggressive: 30 seconds or 0 for force refresh
    gcTime: 5 * 60 * 1000, // 5 minutes garbage collection
    refetchOnWindowFocus: true, // Always refetch when tab gains focus
    refetchOnMount: forceRefresh ? 'always' : true, // Refetch on mount
  });
};

// Hook to invalidate product-related caches
export const useProductCacheInvalidation = () => {
  const queryClient = useQueryClient();

  const invalidateProduct = (productId: string) => {
    queryClient.invalidateQueries({ queryKey: ['product', productId] });
  };

  const invalidateProductList = () => {
    queryClient.invalidateQueries({ queryKey: ['products'] });
  };

  const invalidateAllProductCaches = () => {
    queryClient.invalidateQueries({ queryKey: ['products'] });
    queryClient.invalidateQueries({ 
      predicate: (query) => query.queryKey[0] === 'product' 
    });
  };

  const forceRefreshProduct = (productId: string) => {
    queryClient.refetchQueries({ queryKey: ['product', productId] });
  };

  return {
    invalidateProduct,
    invalidateProductList,
    invalidateAllProductCaches,
    forceRefreshProduct,
  };
};