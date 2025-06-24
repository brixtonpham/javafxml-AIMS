import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import DeliveryInfoForm from './forms/DeliveryInfoForm';
import DeliveryOptionsSelector from './DeliveryOptionsSelector';
import OrderSummary from './OrderSummary';
import OrderPlacement from './checkout/OrderPlacement';
import CheckoutProgress from './CheckoutProgress';
import Card from './ui/Card';
import Button from './ui/Button';
import { useCartContext } from '../contexts/CartContext';
import { deliveryService } from '../services/DeliveryService';
import type {
  CheckoutStep,
  CheckoutFormData,
  CheckoutDeliveryInfo,
  DeliveryOptions,
  DeliveryCalculationResult,
  CheckoutStepConfig
} from '../types/checkout';

const CheckoutWizard: React.FC = () => {
  const navigate = useNavigate();
  const { totalItems } = useCartContext();
  
  const [currentStep, setCurrentStep] = useState<CheckoutStep>(() => {
    // Restore step from sessionStorage if available
    const savedStep = sessionStorage.getItem('checkout_current_step');
    return savedStep ? savedStep as CheckoutStep : 'delivery_info';
  });
  
  const [formData, setFormData] = useState<Partial<CheckoutFormData>>(() => {
    // Restore form data from sessionStorage if available
    const savedData = sessionStorage.getItem('checkout_form_data');
    return savedData ? JSON.parse(savedData) : {};
  });
  
  const [calculationResult, setCalculationResult] = useState<DeliveryCalculationResult | null>(() => {
    // Restore calculation result from sessionStorage if available
    const savedResult = sessionStorage.getItem('checkout_calculation_result');
    return savedResult ? JSON.parse(savedResult) : null;
  });
  
  const [isLoading, setIsLoading] = useState(false);
  const [errors, setErrors] = useState<string[]>([]);

  // Redirect if cart is empty
  useEffect(() => {
    if (totalItems === 0) {
      navigate('/cart');
    }
  }, [totalItems, navigate]);

  // Save current step to sessionStorage
  useEffect(() => {
    sessionStorage.setItem('checkout_current_step', currentStep);
  }, [currentStep]);

  // Save form data to sessionStorage
  useEffect(() => {
    sessionStorage.setItem('checkout_form_data', JSON.stringify(formData));
  }, [formData]);

  // Save calculation result to sessionStorage
  useEffect(() => {
    if (calculationResult) {
      sessionStorage.setItem('checkout_calculation_result', JSON.stringify(calculationResult));
    }
  }, [calculationResult]);

  // Cleanup sessionStorage when component unmounts or cart changes
  useEffect(() => {
    return () => {
      if (totalItems === 0) {
        sessionStorage.removeItem('checkout_current_step');
        sessionStorage.removeItem('checkout_form_data');
        sessionStorage.removeItem('checkout_calculation_result');
      }
    };
  }, [totalItems]);

  // Define checkout steps configuration
  const steps: CheckoutStepConfig[] = [
    {
      id: 'delivery_info',
      title: 'Delivery Information',
      description: 'Enter your delivery details',
      isComplete: !!formData.deliveryInfo,
      isActive: currentStep === 'delivery_info',
      isValid: !!formData.deliveryInfo && deliveryService.isDeliveryInfoValid(formData.deliveryInfo)
    },
    {
      id: 'delivery_options',
      title: 'Delivery Options',
      description: 'Choose delivery preferences',
      isComplete: !!formData.deliveryOptions && !!calculationResult,
      isActive: currentStep === 'delivery_options',
      isValid: !!formData.deliveryOptions && !!calculationResult
    },
    {
      id: 'order_summary',
      title: 'Order Summary',
      description: 'Review your order',
      isComplete: !!formData.deliveryInfo && !!formData.deliveryOptions && !!calculationResult,
      isActive: currentStep === 'order_summary',
      isValid: !!formData.deliveryInfo && !!formData.deliveryOptions && !!calculationResult
    },
    {
      id: 'place_order',
      title: 'Place Order',
      description: 'Confirm and place your order',
      isComplete: false,
      isActive: currentStep === 'place_order',
      isValid: !!formData.deliveryInfo && !!formData.deliveryOptions && !!calculationResult
    }
  ];

  const handleDeliveryInfoSubmit = (deliveryInfo: CheckoutDeliveryInfo) => {
    setFormData(prev => ({ ...prev, deliveryInfo }));
    setCurrentStep('delivery_options');
    setErrors([]);
  };

  const handleDeliveryOptionsChange = (options: DeliveryOptions) => {
    setFormData(prev => ({ ...prev, deliveryOptions: options }));
  };

  const handleCalculationComplete = (result: DeliveryCalculationResult) => {
    setCalculationResult(result);
    if (result.warnings) {
      setErrors(result.warnings);
    } else {
      setErrors([]);
    }
  };

  const handleStepNavigation = (step: CheckoutStep) => {
    // Only allow navigation to completed steps or the next step
    const stepIndex = steps.findIndex(s => s.id === step);
    const currentStepIndex = steps.findIndex(s => s.id === currentStep);
    
    if (stepIndex <= currentStepIndex || (stepIndex === currentStepIndex + 1 && steps[currentStepIndex].isValid)) {
      setCurrentStep(step);
    }
  };

  const handleEditSection = (section: 'delivery' | 'options') => {
    if (section === 'delivery') {
      setCurrentStep('delivery_info');
    } else if (section === 'options') {
      setCurrentStep('delivery_options');
    }
  };

  const handleProceedToOrderPlacement = () => {
    if (!formData.deliveryInfo || !formData.deliveryOptions || !calculationResult) {
      setErrors(['Please complete all required steps']);
      return;
    }
    setCurrentStep('place_order');
  };

  const handleOrderPlaced = (orderId: string) => {
    // Navigate to order confirmation page
    navigate(`/order-confirmation/${orderId}`);
  };

  const renderStepContent = () => {
    switch (currentStep) {
      case 'delivery_info':
        return (
          <div className="space-y-6">
            <DeliveryInfoForm
              initialData={formData.deliveryInfo}
              onSubmit={handleDeliveryInfoSubmit}
            />
            <div className="flex justify-start">
              <Button
                variant="outline"
                onClick={() => navigate('/cart')}
                className="px-6"
              >
                Back to Cart
              </Button>
            </div>
          </div>
        );

      case 'delivery_options':
        if (!formData.deliveryInfo) {
          return (
            <Card>
              <div className="p-6 text-center">
                <p className="text-gray-600">Please complete delivery information first.</p>
                <Button
                  onClick={() => setCurrentStep('delivery_info')}
                  className="mt-4"
                >
                  Back to Delivery Info
                </Button>
              </div>
            </Card>
          );
        }
        
        return (
          <DeliveryOptionsSelector
            deliveryInfo={formData.deliveryInfo}
            onOptionsChange={handleDeliveryOptionsChange}
            onCalculationComplete={handleCalculationComplete}
            onNext={() => setCurrentStep('order_summary')}
            onBack={() => setCurrentStep('delivery_info')}
            initialOptions={formData.deliveryOptions}
          />
        );

      case 'order_summary':
        if (!formData.deliveryInfo || !formData.deliveryOptions || !calculationResult) {
          return (
            <Card>
              <div className="p-6 text-center">
                <p className="text-gray-600">Please complete all previous steps.</p>
                <Button
                  onClick={() => setCurrentStep('delivery_info')}
                  className="mt-4"
                >
                  Start Over
                </Button>
              </div>
            </Card>
          );
        }
        
        return (
          <OrderSummary
            deliveryInfo={formData.deliveryInfo}
            deliveryOptions={formData.deliveryOptions}
            calculationResult={calculationResult}
            onNext={handleProceedToOrderPlacement}
            onBack={() => setCurrentStep('delivery_options')}
            onEdit={handleEditSection}
          />
        );

      case 'place_order':
        if (!formData.deliveryInfo || !formData.deliveryOptions || !calculationResult) {
          return (
            <Card>
              <div className="p-6 text-center">
                <p className="text-gray-600">Please complete all previous steps.</p>
                <Button
                  onClick={() => setCurrentStep('delivery_info')}
                  className="mt-4"
                >
                  Start Over
                </Button>
              </div>
            </Card>
          );
        }
        
        return (
          <OrderPlacement
            deliveryInfo={formData.deliveryInfo}
            deliveryOptions={formData.deliveryOptions}
            calculationResult={calculationResult}
            onBack={() => setCurrentStep('order_summary')}
            onOrderPlaced={handleOrderPlaced}
          />
        );

      default:
        return null;
    }
  };

  if (totalItems === 0) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <Card>
          <div className="p-8 text-center">
            <svg className="mx-auto h-12 w-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 11V7a4 4 0 00-8 0v4M5 9h14l1 12H4L5 9z" />
            </svg>
            <h3 className="mt-2 text-sm font-medium text-gray-900">Your cart is empty</h3>
            <p className="mt-1 text-sm text-gray-500">Add some items to your cart to proceed with checkout.</p>
            <div className="mt-6">
              <Button onClick={() => navigate('/')}>
                Continue Shopping
              </Button>
            </div>
          </div>
        </Card>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto px-4 py-8">
      {/* Header */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900">Checkout</h1>
        <p className="mt-2 text-gray-600">
          Complete your order in {steps.length} easy steps
        </p>
      </div>

      {/* Progress Indicator */}
      <CheckoutProgress currentStep={steps.findIndex(s => s.id === currentStep) + 1} />

      {/* Step Navigation */}
      <div className="mb-8">
        <nav className="flex space-x-8" aria-label="Progress">
          {steps.map((step, index) => {
            const isCurrentStep = step.id === currentStep;
            const isClickable = step.isComplete || isCurrentStep ||
              (index > 0 && steps[index - 1].isComplete);
            
            return (
              <button
                key={step.id}
                onClick={() => isClickable && handleStepNavigation(step.id)}
                disabled={!isClickable}
                className={`
                  flex-1 text-left px-4 py-3 rounded-lg border transition-colors
                  ${isCurrentStep
                    ? 'border-blue-500 bg-blue-50 text-blue-700'
                    : step.isComplete
                      ? 'border-green-500 bg-green-50 text-green-700 hover:bg-green-100'
                      : 'border-gray-200 bg-gray-50 text-gray-400'
                  }
                  ${isClickable && !isCurrentStep ? 'cursor-pointer' : 'cursor-default'}
                  disabled:opacity-50
                `}
              >
                <div className="flex items-center">
                  <span className={`
                    flex h-6 w-6 items-center justify-center rounded-full text-xs font-medium mr-3
                    ${isCurrentStep
                      ? 'bg-blue-600 text-white'
                      : step.isComplete
                        ? 'bg-green-600 text-white'
                        : 'bg-gray-300 text-gray-600'
                    }
                  `}>
                    {step.isComplete ? (
                      <svg className="h-3 w-3" fill="currentColor" viewBox="0 0 20 20">
                        <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                      </svg>
                    ) : (
                      index + 1
                    )}
                  </span>
                  <div>
                    <div className="text-sm font-medium">{step.title}</div>
                    <div className="text-xs">{step.description}</div>
                  </div>
                </div>
              </button>
            );
          })}
        </nav>
      </div>

      {/* Error Messages */}
      {errors.length > 0 && (
        <Card className="mb-6">
          <div className="p-4 bg-yellow-50 border border-yellow-200 rounded-lg">
            <div className="flex">
              <div className="flex-shrink-0">
                <svg className="h-5 w-5 text-yellow-400" viewBox="0 0 20 20" fill="currentColor">
                  <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
                </svg>
              </div>
              <div className="ml-3">
                <h3 className="text-sm font-medium text-yellow-800">Notice</h3>
                <div className="mt-2 text-sm text-yellow-700">
                  <ul className="list-disc space-y-1 pl-5">
                    {errors.map((error, index) => (
                      <li key={index}>{error}</li>
                    ))}
                  </ul>
                </div>
              </div>
            </div>
          </div>
        </Card>
      )}

      {/* Step Content */}
      <div className="mb-8">
        {renderStepContent()}
      </div>

      {/* Footer */}
      <Card>
        <div className="p-6 bg-gray-50">
          <div className="flex items-center justify-between text-sm text-gray-600">
            <span>Need help? Contact our support team</span>
            <div className="flex space-x-4">
              <a href="mailto:support@aims.com" className="text-blue-600 hover:text-blue-500">
                Email Support
              </a>
              <a href="tel:+84123456789" className="text-blue-600 hover:text-blue-500">
                Call Us
              </a>
            </div>
          </div>
        </div>
      </Card>
    </div>
  );
};

export default CheckoutWizard;