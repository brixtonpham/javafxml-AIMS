import React from 'react';

interface SkeletonProps {
  className?: string;
  animate?: boolean;
}

const Skeleton: React.FC<SkeletonProps> = ({ className = '', animate = true }) => (
  <div 
    className={`bg-gray-200 rounded ${animate ? 'animate-pulse' : ''} ${className}`}
    aria-hidden="true"
  />
);

// Product Card Skeleton
export const ProductCardSkeleton: React.FC<{ count?: number }> = ({ count = 1 }) => (
  <>
    {Array.from({ length: count }).map((_, index) => (
      <div key={index} className="bg-white rounded-lg shadow-md overflow-hidden">
        {/* Image skeleton */}
        <Skeleton className="w-full h-48" />
        
        <div className="p-4 space-y-3">
          {/* Title skeleton */}
          <Skeleton className="h-5 w-3/4" />
          
          {/* Category skeleton */}
          <Skeleton className="h-4 w-1/2" />
          
          {/* Price skeleton */}
          <Skeleton className="h-6 w-1/3" />
          
          {/* Button skeleton */}
          <Skeleton className="h-10 w-full mt-4" />
        </div>
      </div>
    ))}
  </>
);

// Order Card Skeleton
export const OrderCardSkeleton: React.FC<{ count?: number }> = ({ count = 3 }) => (
  <>
    {Array.from({ length: count }).map((_, index) => (
      <div key={index} className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
        <div className="flex justify-between items-start mb-4">
          <div className="space-y-2">
            {/* Order number skeleton */}
            <Skeleton className="h-5 w-32" />
            {/* Date skeleton */}
            <Skeleton className="h-4 w-24" />
          </div>
          {/* Status badge skeleton */}
          <Skeleton className="h-6 w-20 rounded-full" />
        </div>
        
        {/* Items skeleton */}
        <div className="space-y-3 mb-4">
          {Array.from({ length: 2 }).map((_, itemIndex) => (
            <div key={itemIndex} className="flex items-center space-x-3">
              <Skeleton className="h-12 w-12 rounded" />
              <div className="flex-1 space-y-1">
                <Skeleton className="h-4 w-3/4" />
                <Skeleton className="h-3 w-1/2" />
              </div>
              <Skeleton className="h-4 w-16" />
            </div>
          ))}
        </div>
        
        {/* Total skeleton */}
        <div className="border-t pt-4">
          <div className="flex justify-between items-center">
            <Skeleton className="h-4 w-20" />
            <Skeleton className="h-5 w-24" />
          </div>
        </div>
      </div>
    ))}
  </>
);

// Cart Item Skeleton
export const CartItemSkeleton: React.FC<{ count?: number }> = ({ count = 3 }) => (
  <>
    {Array.from({ length: count }).map((_, index) => (
      <div key={index} className="flex items-center space-x-4 py-4 border-b border-gray-200">
        {/* Product image skeleton */}
        <Skeleton className="h-16 w-16 rounded" />
        
        <div className="flex-1 space-y-2">
          {/* Product title skeleton */}
          <Skeleton className="h-5 w-3/4" />
          {/* Product details skeleton */}
          <Skeleton className="h-4 w-1/2" />
        </div>
        
        {/* Quantity controls skeleton */}
        <div className="flex items-center space-x-2">
          <Skeleton className="h-8 w-8 rounded" />
          <Skeleton className="h-8 w-12" />
          <Skeleton className="h-8 w-8 rounded" />
        </div>
        
        {/* Price skeleton */}
        <Skeleton className="h-5 w-20" />
        
        {/* Remove button skeleton */}
        <Skeleton className="h-8 w-8 rounded" />
      </div>
    ))}
  </>
);

// Table Skeleton
export const TableSkeleton: React.FC<{ rows?: number; columns?: number }> = ({ 
  rows = 5, 
  columns = 4 
}) => (
  <div className="bg-white shadow-sm rounded-lg overflow-hidden">
    {/* Header skeleton */}
    <div className="bg-gray-50 px-6 py-3 border-b border-gray-200">
      <div className="flex space-x-4">
        {Array.from({ length: columns }).map((_, index) => (
          <Skeleton key={index} className="h-4 flex-1" />
        ))}
      </div>
    </div>
    
    {/* Rows skeleton */}
    <div className="divide-y divide-gray-200">
      {Array.from({ length: rows }).map((_, rowIndex) => (
        <div key={rowIndex} className="px-6 py-4">
          <div className="flex space-x-4">
            {Array.from({ length: columns }).map((_, colIndex) => (
              <Skeleton key={colIndex} className="h-4 flex-1" />
            ))}
          </div>
        </div>
      ))}
    </div>
  </div>
);

// Page Header Skeleton
export const PageHeaderSkeleton: React.FC = () => (
  <div className="mb-8">
    <Skeleton className="h-8 w-1/3 mb-2" />
    <Skeleton className="h-4 w-1/2" />
  </div>
);

// Navigation Skeleton
export const NavigationSkeleton: React.FC = () => (
  <div className="space-y-2">
    {Array.from({ length: 5 }).map((_, index) => (
      <div key={index} className="flex items-center space-x-3 px-3 py-2">
        <Skeleton className="h-5 w-5 rounded" />
        <Skeleton className="h-4 flex-1" />
      </div>
    ))}
  </div>
);

// Form Skeleton
export const FormSkeleton: React.FC<{ fields?: number }> = ({ fields = 4 }) => (
  <div className="space-y-6">
    {Array.from({ length: fields }).map((_, index) => (
      <div key={index} className="space-y-2">
        <Skeleton className="h-4 w-1/4" />
        <Skeleton className="h-10 w-full rounded-md" />
      </div>
    ))}
    <div className="flex space-x-3 pt-4">
      <Skeleton className="h-10 w-24 rounded-md" />
      <Skeleton className="h-10 w-20 rounded-md" />
    </div>
  </div>
);

// Stats Card Skeleton
export const StatsCardSkeleton: React.FC<{ count?: number }> = ({ count = 4 }) => (
  <>
    {Array.from({ length: count }).map((_, index) => (
      <div key={index} className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
        <div className="flex items-center justify-between">
          <div className="space-y-2">
            <Skeleton className="h-4 w-24" />
            <Skeleton className="h-8 w-16" />
          </div>
          <Skeleton className="h-8 w-8 rounded" />
        </div>
      </div>
    ))}
  </>
);

// Text Block Skeleton
export const TextBlockSkeleton: React.FC<{ lines?: number }> = ({ lines = 3 }) => (
  <div className="space-y-2">
    {Array.from({ length: lines }).map((_, index) => (
      <Skeleton 
        key={index} 
        className={`h-4 ${
          index === lines - 1 ? 'w-3/4' : 'w-full'
        }`} 
      />
    ))}
  </div>
);

// Image Skeleton
export const ImageSkeleton: React.FC<{ 
  className?: string;
  aspectRatio?: 'square' | 'video' | 'photo';
}> = ({ className = '', aspectRatio = 'photo' }) => {
  const aspectClasses = {
    square: 'aspect-square',
    video: 'aspect-video',
    photo: 'aspect-[4/3]'
  };

  return (
    <Skeleton 
      className={`w-full ${aspectClasses[aspectRatio]} ${className}`} 
    />
  );
};

// Button Skeleton
export const ButtonSkeleton: React.FC<{ 
  size?: 'sm' | 'md' | 'lg';
  variant?: 'primary' | 'secondary' | 'outline';
}> = ({ size = 'md', variant = 'primary' }) => {
  const sizeClasses = {
    sm: 'h-8 w-16',
    md: 'h-10 w-20',
    lg: 'h-12 w-24'
  };

  return (
    <Skeleton 
      className={`${sizeClasses[size]} rounded-md`} 
    />
  );
};

// List Item Skeleton
export const ListItemSkeleton: React.FC<{ count?: number; showIcon?: boolean }> = ({ 
  count = 5, 
  showIcon = true 
}) => (
  <>
    {Array.from({ length: count }).map((_, index) => (
      <div key={index} className="flex items-center space-x-3 py-3">
        {showIcon && <Skeleton className="h-5 w-5 rounded" />}
        <div className="flex-1 space-y-1">
          <Skeleton className="h-4 w-3/4" />
          <Skeleton className="h-3 w-1/2" />
        </div>
      </div>
    ))}
  </>
);

// Card Skeleton (generic)
export const CardSkeleton: React.FC<{ 
  children?: React.ReactNode;
  className?: string;
}> = ({ children, className = '' }) => (
  <div className={`bg-white rounded-lg shadow-sm border border-gray-200 p-6 ${className}`}>
    {children || (
      <div className="space-y-4">
        <Skeleton className="h-6 w-1/3" />
        <TextBlockSkeleton lines={3} />
        <ButtonSkeleton />
      </div>
    )}
  </div>
);

// Full Page Skeleton
export const FullPageSkeleton: React.FC = () => (
  <div className="min-h-screen bg-gray-50">
    {/* Header skeleton */}
    <div className="bg-white shadow-sm border-b border-gray-200">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center py-4">
          <Skeleton className="h-8 w-32" />
          <div className="flex items-center space-x-4">
            <Skeleton className="h-8 w-8 rounded-full" />
            <Skeleton className="h-8 w-24" />
          </div>
        </div>
      </div>
    </div>

    {/* Main content skeleton */}
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <PageHeaderSkeleton />
      
      <div className="grid grid-cols-1 lg:grid-cols-4 gap-8">
        {/* Sidebar skeleton */}
        <div className="lg:col-span-1">
          <CardSkeleton>
            <NavigationSkeleton />
          </CardSkeleton>
        </div>
        
        {/* Content skeleton */}
        <div className="lg:col-span-3">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            <ProductCardSkeleton count={6} />
          </div>
        </div>
      </div>
    </div>
  </div>
);

export default {
  Skeleton,
  ProductCardSkeleton,
  OrderCardSkeleton,
  CartItemSkeleton,
  TableSkeleton,
  PageHeaderSkeleton,
  NavigationSkeleton,
  FormSkeleton,
  StatsCardSkeleton,
  TextBlockSkeleton,
  ImageSkeleton,
  ButtonSkeleton,
  ListItemSkeleton,
  CardSkeleton,
  FullPageSkeleton,
};