import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { 
  ArrowLeftIcon, 
  HeartIcon, 
  ShareIcon,
  StarIcon,
  CheckCircleIcon,
  ExclamationTriangleIcon
} from '@heroicons/react/24/outline';
import { HeartIcon as HeartSolidIcon } from '@heroicons/react/24/solid';
import AppLayout from '../components/layout/AppLayout';
import { Card, Button } from '../components/ui';
import AddToCartButton from '../components/cart/AddToCartButton';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { productService } from '../services/productService';

const ProductDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [selectedImageIndex, setSelectedImageIndex] = useState(0);
  const [isFavorite, setIsFavorite] = useState(false);
  const [isZoomed, setIsZoomed] = useState(false);

  // Fetch product data using React Query with aggressive refresh strategy
  const {
    data: product,
    isLoading: loading,
    error,
    refetch: refetchProduct
  } = useQuery({
    queryKey: ['product', id],
    queryFn: () => productService.getProductById(id!),
    enabled: !!id,
    staleTime: 0, // Always fetch fresh data for detail page
    gcTime: 2 * 60 * 1000, // 2 minutes garbage collection
    retry: 1,
    refetchOnWindowFocus: true, // Refetch when user returns to tab
    refetchOnMount: 'always', // Always refetch on mount
  });

  // Force invalidate product list cache when this page loads to ensure consistency
  React.useEffect(() => {
    if (id) {
      // Invalidate all related product queries to ensure fresh data
      queryClient.invalidateQueries({ queryKey: ['products'] });
      queryClient.invalidateQueries({ queryKey: ['product', id] });
    }
  }, [id, queryClient]);

  // Force refetch when navigating back from cart or other pages
  React.useEffect(() => {
    const handleVisibilityChange = () => {
      if (!document.hidden && id) {
        refetchProduct();
      }
    };

    document.addEventListener('visibilitychange', handleVisibilityChange);
    return () => document.removeEventListener('visibilitychange', handleVisibilityChange);
  }, [id, refetchProduct]);

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND',
    }).format(price);
  };

  const getProductTypeIcon = (type: string) => {
    switch (type) {
      case 'BOOK': return '📚';
      case 'CD': return '💿';
      case 'DVD': return '📀';
      case 'LP': return '🎵';
      default: return '📦';
    }
  };

  const renderTypeSpecificInfo = () => {
    if (!product) return null;

    switch (product.productType) {
      case 'BOOK':
        return (
          <div className="space-y-2">
            {product.author && (
              <div className="flex justify-between">
                <span className="text-gray-600">Author:</span>
                <span className="font-medium">{product.author}</span>
              </div>
            )}
            {product.publisher && (
              <div className="flex justify-between">
                <span className="text-gray-600">Publisher:</span>
                <span className="font-medium">{product.publisher}</span>
              </div>
            )}
            {product.language && (
              <div className="flex justify-between">
                <span className="text-gray-600">Language:</span>
                <span className="font-medium">{product.language}</span>
              </div>
            )}
          </div>
        );
      case 'CD':
        return (
          <div className="space-y-2">
            {product.artists && (
              <div className="flex justify-between">
                <span className="text-gray-600">Artists:</span>
                <span className="font-medium">{product.artists}</span>
              </div>
            )}
            {product.genre && (
              <div className="flex justify-between">
                <span className="text-gray-600">Genre:</span>
                <span className="font-medium">{product.genre}</span>
              </div>
            )}
          </div>
        );
      case 'DVD':
        return (
          <div className="space-y-2">
            {product.director && (
              <div className="flex justify-between">
                <span className="text-gray-600">Director:</span>
                <span className="font-medium">{product.director}</span>
              </div>
            )}
            {product.genre && (
              <div className="flex justify-between">
                <span className="text-gray-600">Genre:</span>
                <span className="font-medium">{product.genre}</span>
              </div>
            )}
            {product.runtime && (
              <div className="flex justify-between">
                <span className="text-gray-600">Runtime:</span>
                <span className="font-medium">{product.runtime} minutes</span>
              </div>
            )}
            {product.language && (
              <div className="flex justify-between">
                <span className="text-gray-600">Language:</span>
                <span className="font-medium">{product.language}</span>
              </div>
            )}
          </div>
        );
      case 'LP':
        return (
          <div className="space-y-2">
            {product.artists && (
              <div className="flex justify-between">
                <span className="text-gray-600">Artists:</span>
                <span className="font-medium">{product.artists}</span>
              </div>
            )}
            {product.genre && (
              <div className="flex justify-between">
                <span className="text-gray-600">Genre:</span>
                <span className="font-medium">{product.genre}</span>
              </div>
            )}
            {product.tracklist && (
              <div className="col-span-2">
                <span className="text-gray-600">Tracklist:</span>
                <p className="mt-1 text-sm">{product.tracklist}</p>
              </div>
            )}
          </div>
        );
      default:
        return null;
    }
  };

  const handleImageZoom = () => {
    setIsZoomed(!isZoomed);
  };

  if (loading) {
    return (
      <AppLayout title="Loading...">
        <div className="animate-pulse">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
            <div className="space-y-4">
              <div className="aspect-square bg-gray-200 rounded-lg"></div>
              <div className="flex space-x-2">
                {[1, 2, 3, 4].map((i) => (
                  <div key={`skeleton-${i}`} className="w-16 h-16 bg-gray-200 rounded"></div>
                ))}
              </div>
            </div>
            <div className="space-y-4">
              <div className="h-8 bg-gray-200 rounded"></div>
              <div className="h-4 bg-gray-200 rounded w-3/4"></div>
              <div className="h-6 bg-gray-200 rounded w-1/2"></div>
              <div className="space-y-2">
                {[1, 2, 3].map((i) => (
                  <div key={`skeleton-detail-${i}`} className="h-4 bg-gray-200 rounded"></div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </AppLayout>
    );
  }

  if (error) {
    return (
      <AppLayout title="Error">
        <div className="text-center py-12">
          <h1 className="text-2xl font-bold text-gray-900 mb-4">Failed to Load Product</h1>
          <p className="text-gray-600 mb-6">
            {error instanceof Error ? error.message : 'Unable to load product details.'}
          </p>
          <div className="flex justify-center gap-4">
            <Button onClick={() => navigate('/products')}>
              Browse Products
            </Button>
            <Button variant="outline" onClick={() => window.location.reload()}>
              Try Again
            </Button>
          </div>
        </div>
      </AppLayout>
    );
  }

  if (!product) {
    return (
      <AppLayout title="Product Not Found">
        <div className="text-center py-12">
          <h1 className="text-2xl font-bold text-gray-900 mb-4">Product Not Found</h1>
          <p className="text-gray-600 mb-6">The product you're looking for doesn't exist.</p>
          <Button onClick={() => navigate('/products')}>
            Browse Products
          </Button>
        </div>
      </AppLayout>
    );
  }

  return (
    <AppLayout title={product.title}>
      <div className="max-w-7xl mx-auto">
        {/* Navigation */}
        <div className="mb-6">
          <Button
            variant="outline"
            size="sm"
            onClick={() => navigate(-1)}
            className="mb-4"
          >
            <ArrowLeftIcon className="w-4 h-4 mr-2" />
            Back
          </Button>
          
          {/* Breadcrumb */}
          <nav className="text-sm text-gray-600">
            <span>Home</span>
            <span className="mx-2">/</span>
            <span>Products</span>
            <span className="mx-2">/</span>
            <span>{product.category}</span>
            <span className="mx-2">/</span>
            <span className="text-gray-900 font-medium">{product.title}</span>
          </nav>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 mb-12">
          {/* Product Images */}
          <div className="space-y-4">
            {/* Main Image */}
            <motion.div 
              className="relative aspect-square bg-gray-100 rounded-lg overflow-hidden cursor-zoom-in"
              onClick={handleImageZoom}
              whileHover={{ scale: 1.02 }}
              transition={{ duration: 0.2 }}
            >
              <img
                src={product.imageUrl || '/api/placeholder/600/600'}
                alt={product.title}
                className="w-full h-full object-cover"
              />
              <div className="absolute top-4 right-4 flex space-x-2">
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    setIsFavorite(!isFavorite);
                  }}
                  className="p-2 bg-white rounded-full shadow-md hover:bg-gray-50 transition-colors"
                >
                  {isFavorite ? (
                    <HeartSolidIcon className="w-5 h-5 text-red-500" />
                  ) : (
                    <HeartIcon className="w-5 h-5 text-gray-600" />
                  )}
                </button>
                <button className="p-2 bg-white rounded-full shadow-md hover:bg-gray-50 transition-colors">
                  <ShareIcon className="w-5 h-5 text-gray-600" />
                </button>
              </div>
            </motion.div>

            {/* Thumbnail Images */}
            <div className="flex space-x-2 overflow-x-auto">
              {[product.imageUrl, product.imageUrl, product.imageUrl, product.imageUrl].map((img, index) => (
                <button
                  key={index}
                  onClick={() => setSelectedImageIndex(index)}
                  className={`flex-shrink-0 w-16 h-16 rounded overflow-hidden border-2 transition-colors ${
                    selectedImageIndex === index 
                      ? 'border-primary-500' 
                      : 'border-gray-200 hover:border-gray-300'
                  }`}
                >
                  <img
                    src={img || '/api/placeholder/80/80'}
                    alt={`${product.title} view ${index + 1}`}
                    className="w-full h-full object-cover"
                  />
                </button>
              ))}
            </div>
          </div>

          {/* Product Info */}
          <div className="space-y-6">
            {/* Header */}
            <div>
              <div className="flex items-center space-x-2 mb-2">
                <span className="text-2xl">{getProductTypeIcon(product.productType)}</span>
                <span className="bg-gray-100 text-gray-700 px-2 py-1 rounded text-sm font-medium">
                  {product.productType}
                </span>
                <span className="bg-blue-100 text-blue-700 px-2 py-1 rounded text-sm font-medium">
                  {product.category}
                </span>
              </div>
              <h1 className="text-3xl font-bold text-gray-900 mb-2">{product.title}</h1>
              <div className="flex items-center space-x-4">
                <div className="flex items-center">
                  {[...Array(5)].map((_, i) => (
                    <StarIcon
                      key={i}
                      className={`w-5 h-5 ${
                        i < 4 ? 'text-yellow-400 fill-current' : 'text-gray-300'
                      }`}
                    />
                  ))}
                  <span className="ml-2 text-sm text-gray-600">(4.2 out of 5)</span>
                </div>
              </div>
            </div>

            {/* Price */}
            <div className="border-t border-b py-4">
              <div className="flex items-center justify-between mb-2">
                <span className="text-3xl font-bold text-primary-600">
                  {formatPrice(product.price)}
                </span>
                <div className="text-right">
                  <div className="text-sm text-gray-500">VAT Included</div>
                  <div className="text-sm text-gray-500">
                    Base Price: {formatPrice(product.valueAmount)}
                  </div>
                </div>
              </div>
            </div>

            {/* Stock Status */}
            <div className="flex items-center space-x-2">
              {(product.quantityInStock ?? product.quantity ?? 0) > 0 ? (
                <>
                  <CheckCircleIcon className="w-5 h-5 text-green-500" />
                  <span className="text-green-700 font-medium">In Stock</span>
                  <span className="text-gray-500">({product.quantityInStock ?? product.quantity ?? 0} available)</span>
                </>
              ) : (
                <>
                  <ExclamationTriangleIcon className="w-5 h-5 text-red-500" />
                  <span className="text-red-700 font-medium">Out of Stock</span>
                </>
              )}
            </div>

            {/* Add to Cart */}
            <div className="space-y-3">
              <AddToCartButton
                product={product}
                disabled={(product.quantityInStock ?? product.quantity ?? 0) === 0}
                className="w-full"
                size="lg"
              />
              <div className="flex space-x-3">
                <Button variant="outline" fullWidth>
                  Add to Wishlist
                </Button>
                <Button variant="outline" fullWidth>
                  Compare
                </Button>
              </div>
            </div>

            {/* Product Specifications */}
            <Card className="p-4">
              <h3 className="font-semibold text-gray-900 mb-3">Product Details</h3>
              <div className="space-y-2">
                <div className="flex justify-between">
                  <span className="text-gray-600">Category:</span>
                  <span className="font-medium">{product.category}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-600">Type:</span>
                  <span className="font-medium">{product.productType}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-600">Entry Date:</span>
                  <span className="font-medium">
                    {new Date(product.entryDate).toLocaleDateString()}
                  </span>
                </div>
                {renderTypeSpecificInfo()}
              </div>
            </Card>
          </div>
        </div>

        {/* Product Description */}
        <Card className="p-6 mb-8">
          <h2 className="text-xl font-semibold text-gray-900 mb-4">Description</h2>
          <p className="text-gray-700 leading-relaxed">
            {product.description}
          </p>
        </Card>

        {/* Related Products */}
        <div className="mb-8">
          <h2 className="text-xl font-semibold text-gray-900 mb-6">You Might Also Like</h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
            {/* Placeholder for related products */}
            {[1, 2, 3, 4].map((i) => (
              <Card key={i} className="p-4">
                <div className="aspect-square bg-gray-200 rounded mb-3"></div>
                <div className="h-4 bg-gray-200 rounded mb-2"></div>
                <div className="h-4 bg-gray-200 rounded w-3/4 mb-2"></div>
                <div className="h-6 bg-gray-200 rounded w-1/2"></div>
              </Card>
            ))}
          </div>
        </div>
      </div>

      {/* Image Zoom Modal */}
      {isZoomed && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          className="fixed inset-0 bg-black bg-opacity-90 z-50 flex items-center justify-center p-4"
          onClick={() => setIsZoomed(false)}
        >
          <motion.img
            initial={{ scale: 0.8 }}
            animate={{ scale: 1 }}
            src={product.imageUrl || '/api/placeholder/800/800'}
            alt={product.title}
            className="max-w-full max-h-full object-contain"
          />
        </motion.div>
      )}
    </AppLayout>
  );
};

export default ProductDetailPage;