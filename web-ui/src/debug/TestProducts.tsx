import React from 'react';
import { useProducts } from '../hooks/useProducts';

const TestProducts: React.FC = () => {
  const { products, isLoading, error, data } = useProducts();

  console.log('Debug - Products hook result:', {
    products,
    isLoading,
    error,
    data,
    productsLength: products?.length,
  });

  if (isLoading) {
    return <div>Loading...</div>;
  }

  if (error) {
    console.error('Products error:', error);
    return <div>Error: {error.message}</div>;
  }

  return (
    <div>
      <h2>Products Debug</h2>
      <p>Total products: {products.length}</p>
      <ul>
        {products.slice(0, 3).map((product) => (
          <li key={product.id}>
            {product.title} - ${product.price}
          </li>
        ))}
      </ul>
    </div>
  );
};

export default TestProducts;
