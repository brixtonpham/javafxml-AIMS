import React, { useState, useEffect, useCallback } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Input, Button, Card } from '../ui';
import type { CheckoutDeliveryInfo, DeliveryInfoFormErrors } from '../../types/checkout';
import { deliveryService } from '../../services/DeliveryService';
import { locationHelpers } from '../../data/vietnamLocations';

// Zod validation schema
const deliveryInfoSchema = z.object({
  recipientName: z.string()
    .min(2, 'Recipient name must be at least 2 characters')
    .max(100, 'Recipient name must be less than 100 characters')
    .regex(/^[a-zA-ZÀ-ÿ\s]+$/, 'Please enter a valid name'),
  
  phone: z.string()
    .min(10, 'Phone number is required')
    .regex(/^(\+84|84|0)(3[2-9]|5[689]|7[06-9]|8[1-689]|9[0-46-9])[0-9]{7}$/, 'Please enter a valid Vietnamese phone number'),
  
  email: z.string()
    .email('Please enter a valid email address'),
  
  address: z.string()
    .min(10, 'Address must be at least 10 characters')
    .max(200, 'Address must be less than 200 characters'),
  
  province: z.string()
    .min(1, 'Province is required'),
  
  city: z.string()
    .min(1, 'City/District is required'),
  
  postalCode: z.string()
    .optional()
    .refine((val) => !val || /^\d{5,6}$/.test(val), 'Postal code must be 5-6 digits'),
  
  deliveryInstructions: z.string()
    .optional()
    .refine((val) => !val || val.length <= 500, 'Delivery instructions must be less than 500 characters'),
});

type DeliveryInfoFormData = z.infer<typeof deliveryInfoSchema>;

export interface DeliveryInfoFormProps {
  initialData?: Partial<CheckoutDeliveryInfo>;
  onSubmit: (data: CheckoutDeliveryInfo) => void;
  onValidationChange?: (isValid: boolean) => void;
  className?: string;
  isLoading?: boolean;
}

export const DeliveryInfoForm: React.FC<DeliveryInfoFormProps> = ({
  initialData,
  onSubmit,
  onValidationChange,
  className = '',
  isLoading = false,
}) => {
  const [selectedProvince, setSelectedProvince] = useState<string>(initialData?.province || '');
  const [availableCities, setAvailableCities] = useState<Array<{ code: string; name: string }>>([]);
  const [rushDeliveryAvailable, setRushDeliveryAvailable] = useState<boolean>(false);

  const {
    control,
    handleSubmit,
    formState: { errors, isValid },
    watch,
    setValue,
    trigger,
  } = useForm<DeliveryInfoFormData>({
    resolver: zodResolver(deliveryInfoSchema),
    defaultValues: {
      recipientName: initialData?.recipientName || '',
      phone: initialData?.phone || '',
      email: initialData?.email || '',
      address: initialData?.address || '',
      province: initialData?.province || '',
      city: initialData?.city || '',
      postalCode: initialData?.postalCode || '',
      deliveryInstructions: initialData?.deliveryInstructions || '',
    },
    mode: 'onChange',
  });

  const watchedProvince = watch('province');
  const watchedCity = watch('city');

  // Update validation state
  useEffect(() => {
    onValidationChange?.(isValid);
  }, [isValid, onValidationChange]);

  // Load cities when province changes
  useEffect(() => {
    if (watchedProvince && watchedProvince !== selectedProvince) {
      setSelectedProvince(watchedProvince);
      const cities = deliveryService.getCitiesForProvince(watchedProvince);
      setAvailableCities(cities);
      
      // Reset city selection if current city is not in new province
      if (watchedCity) {
        const isCityValid = cities.some(city => city.code === watchedCity);
        if (!isCityValid) {
          setValue('city', '');
          trigger('city');
        }
      }
    }
  }, [watchedProvince, selectedProvince, watchedCity, setValue, trigger]);

  // Check rush delivery availability
  useEffect(() => {
    if (watchedProvince && watchedCity) {
      const isAvailable = deliveryService.isRushDeliveryAvailable(watchedProvince, watchedCity);
      setRushDeliveryAvailable(isAvailable);
    } else {
      setRushDeliveryAvailable(false);
    }
  }, [watchedProvince, watchedCity]);

  // Initialize cities on mount
  useEffect(() => {
    if (initialData?.province) {
      const cities = deliveryService.getCitiesForProvince(initialData.province);
      setAvailableCities(cities);
    }
  }, [initialData?.province]);

  const handleFormSubmit = useCallback((data: DeliveryInfoFormData) => {
    const formattedData: CheckoutDeliveryInfo = {
      ...data,
      postalCode: data.postalCode || undefined,
      deliveryInstructions: data.deliveryInstructions || undefined,
      isValidated: true,
    };

    onSubmit(formattedData);
  }, [onSubmit]);

  const provinces = deliveryService.getAllProvinces();

  return (
    <Card className={`p-6 ${className}`}>
      <div className="mb-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-2">
          Delivery Information
        </h3>
        <p className="text-sm text-gray-600">
          Please provide accurate delivery details for your order.
        </p>
      </div>

      <form onSubmit={handleSubmit(handleFormSubmit)} className="space-y-4">
        {/* Personal Information */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Recipient Name *
            </label>
            <Controller
              name="recipientName"
              control={control}
              render={({ field }) => (
                <Input
                  {...field}
                  placeholder="Enter recipient's full name"
                  error={errors.recipientName?.message}
                  className="w-full"
                />
              )}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Phone Number *
            </label>
            <Controller
              name="phone"
              control={control}
              render={({ field }) => (
                <Input
                  {...field}
                  type="tel"
                  placeholder="0123456789"
                  error={errors.phone?.message}
                  className="w-full"
                />
              )}
            />
          </div>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Email Address *
          </label>
          <Controller
            name="email"
            control={control}
            render={({ field }) => (
              <Input
                {...field}
                type="email"
                placeholder="example@email.com"
                error={errors.email?.message}
                className="w-full"
              />
            )}
          />
        </div>

        {/* Address Information */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Street Address *
          </label>
          <Controller
            name="address"
            control={control}
            render={({ field }) => (
              <Input
                {...field}
                placeholder="Enter detailed street address"
                error={errors.address?.message}
                className="w-full"
              />
            )}
          />
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Province/City *
            </label>
            <Controller
              name="province"
              control={control}
              render={({ field }) => (
                <select
                  {...field}
                  className={`w-full px-3 py-2 border rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 ${
                    errors.province ? 'border-red-500' : 'border-gray-300'
                  }`}
                >
                  <option value="">Select Province/City</option>
                  {provinces.map((province) => (
                    <option key={province.code} value={province.code}>
                      {province.name}
                    </option>
                  ))}
                </select>
              )}
            />
            {errors.province && (
              <p className="mt-1 text-sm text-red-600">{errors.province.message}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              District *
            </label>
            <Controller
              name="city"
              control={control}
              render={({ field }) => (
                <select
                  {...field}
                  disabled={!selectedProvince || availableCities.length === 0}
                  className={`w-full px-3 py-2 border rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:bg-gray-50 disabled:cursor-not-allowed ${
                    errors.city ? 'border-red-500' : 'border-gray-300'
                  }`}
                >
                  <option value="">Select District</option>
                  {availableCities.map((city) => (
                    <option key={city.code} value={city.code}>
                      {city.name}
                    </option>
                  ))}
                </select>
              )}
            />
            {errors.city && (
              <p className="mt-1 text-sm text-red-600">{errors.city.message}</p>
            )}
          </div>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Postal Code (Optional)
          </label>
          <Controller
            name="postalCode"
            control={control}
            render={({ field }) => (
              <Input
                {...field}
                placeholder="12345"
                error={errors.postalCode?.message}
                className="w-full md:w-48"
              />
            )}
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Delivery Instructions (Optional)
          </label>
          <Controller
            name="deliveryInstructions"
            control={control}
            render={({ field }) => (
              <textarea
                {...field}
                rows={3}
                placeholder="Any special delivery instructions..."
                className={`w-full px-3 py-2 border rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 ${
                  errors.deliveryInstructions ? 'border-red-500' : 'border-gray-300'
                }`}
              />
            )}
          />
          {errors.deliveryInstructions && (
            <p className="mt-1 text-sm text-red-600">{errors.deliveryInstructions.message}</p>
          )}
        </div>

        {/* Rush Delivery Notice */}
        {rushDeliveryAvailable && (
          <div className="bg-green-50 border border-green-200 rounded-md p-3">
            <div className="flex items-center">
              <svg className="h-5 w-5 text-green-500 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
              </svg>
              <span className="text-sm text-green-800 font-medium">
                Rush delivery available for this location!
              </span>
            </div>
          </div>
        )}

        {/* Free Shipping Notice */}
        <div className="bg-blue-50 border border-blue-200 rounded-md p-3">
          <div className="flex items-center">
            <svg className="h-5 w-5 text-blue-500 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
            </svg>
            <span className="text-sm text-blue-800">
              Free shipping on orders over {deliveryService.formatDeliveryFee(deliveryService.getFreeShippingThreshold())}
            </span>
          </div>
        </div>

        {/* Submit Button */}
        <div className="pt-4">
          <Button
            type="submit"
            disabled={!isValid || isLoading}
            isLoading={isLoading}
            className="w-full md:w-auto"
          >
            {isLoading ? 'Validating...' : 'Continue to Delivery Options'}
          </Button>
        </div>
      </form>
    </Card>
  );
};

export default DeliveryInfoForm;