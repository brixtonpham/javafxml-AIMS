import React from 'react';
import { UserMenu, AuthenticatedOnly, UnauthenticatedOnly } from '../auth';
import { Button } from '../ui';
import type { LayoutProps } from '../../types';
import CartIcon from '../cart/CartIcon';
import CartDrawer from '../cart/CartDrawer';
import OfflineIndicator from '../OfflineIndicator';

const AppLayout: React.FC<LayoutProps> = ({ 
  children, 
  title = 'AIMS - Internet Media Store',
  showHeader = true,
  showSidebar = false 
}) => {
  const [isCartOpen, setCartOpen] = React.useState(false);

  const handleCartToggle = () => setCartOpen((prev) => !prev);
  return (
    <div className="min-h-screen bg-gray-50">
      {showHeader && (
        <header className="bg-white shadow-sm border-b border-gray-200">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="flex justify-between items-center h-16">
              <div className="flex items-center">
                <h1 className="text-xl font-semibold text-gray-900">
                  {title}
                </h1>
              </div>
              <nav className="flex items-center space-x-6">
                {/* Main Navigation Links */}
                  <CartIcon onClick={handleCartToggle} />
                <div className="hidden md:flex space-x-6">
                  <a
                    href="/"
                    className="text-gray-700 hover:text-blue-600 px-3 py-2 text-sm font-medium transition-colors"
                  >
                    Home
                  </a>
                  <AuthenticatedOnly>
                    <a
                      href="/products"
                      className="text-gray-700 hover:text-blue-600 px-3 py-2 text-sm font-medium transition-colors"
                    >
                      Products
                    </a>
                    <a
                      href="/cart"
                      className="text-gray-700 hover:text-blue-600 px-3 py-2 text-sm font-medium transition-colors"
                    >
                      Cart
                    </a>
                    <a
                      href="/orders"
                      className="text-gray-700 hover:text-blue-600 px-3 py-2 text-sm font-medium transition-colors"
                    >
                      Orders
                    </a>
                  </AuthenticatedOnly>
                </div>
                  <CartDrawer isOpen={isCartOpen} onClose={handleCartToggle} />

                {/* Network Status and Authentication Section */}
                <div className="flex items-center space-x-4">
                  {/* Network Status Indicator */}
                  <OfflineIndicator
                    className="hidden md:block"
                    showDetails={true}
                    autoReconnect={true}
                  />
                  
                  <AuthenticatedOnly>
                    <UserMenu />
                  </AuthenticatedOnly>
                  
                  <UnauthenticatedOnly>
                    <Button
                      variant="outline"
                      onClick={() => window.location.href = '/login'}
                      className="text-sm"
                    >
                      Sign In
                    </Button>
                  </UnauthenticatedOnly>
                </div>
              </nav>
            </div>
          </div>
        </header>
      )}
      
      <div className="flex">
        {showSidebar && (
          <aside className="w-64 bg-white shadow-sm min-h-screen">
            <div className="p-4">
              {/* Sidebar content will be added here */}
              <p className="text-gray-600">Sidebar</p>
            </div>
          </aside>
        )}
        
        <main className="flex-1">
          <div className="max-w-7xl mx-auto py-6 px-4 sm:px-6 lg:px-8">
            {children}
          </div>
        </main>
      </div>
    </div>
  );
};

export default AppLayout;