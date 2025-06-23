import { useQuery, useInfiniteQuery } from '@tanstack/react-query';
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
}

export const useProducts = (
  filters: ProductFilters = {},
  options: UseProductsOptions = {}
) => {
  const {
    pageSize = 20,
    enabled = true,
    refetchOnWindowFocus = false,
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
    staleTime: 5 * 60 * 1000, // 5 minutes
    gcTime: 10 * 60 * 1000, // 10 minutes
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
  } = options;

  const query = useInfiniteQuery<PaginatedResponse<Product>>({
    queryKey: ['products-infinite', filters],
    queryFn: ({ pageParam = 1 }) =>
      productService.getProducts({
        ...filters,
        page: pageParam as number,
        pageSize,
      }),
    enabled,
    refetchOnWindowFocus,
    staleTime: 5 * 60 * 1000,
    gcTime: 10 * 60 * 1000,
    getNextPageParam: (lastPage) => {
      const { pagination } = lastPage;
      return pagination.hasNext ? pagination.page + 1 : undefined;
    },
    initialPageParam: 1,
  });

  const allProducts = query.data?.pages.flatMap(page => page.items) || [];
  const totalProducts = query.data?.pages[0]?.pagination.total || 0;

  return {
    ...query,
    products: allProducts,
    totalProducts,
    hasNextPage: query.hasNextPage,
    fetchNextPage: query.fetchNextPage,
    isFetchingNextPage: query.isFetchingNextPage,
  };
};

export const useProductCategories = () => {
  return useQuery<string[]>({
    queryKey: ['product-categories'],
    queryFn: () => productService.getCategories(),
    staleTime: 30 * 60 * 1000, // 30 minutes
    gcTime: 60 * 60 * 1000, // 1 hour
  });
};

export const useProductTypes = () => {
  return useQuery<string[]>({
    queryKey: ['product-types'],
    queryFn: () => productService.getProductTypes(),
    staleTime: 30 * 60 * 1000, // 30 minutes
    gcTime: 60 * 60 * 1000, // 1 hour
  });
};

export const useProduct = (id: string) => {
  return useQuery<Product>({
    queryKey: ['product', id],
    queryFn: () => productService.getProductById(id),
    enabled: !!id,
    staleTime: 10 * 60 * 1000, // 10 minutes
    gcTime: 30 * 60 * 1000, // 30 minutes
  });
};