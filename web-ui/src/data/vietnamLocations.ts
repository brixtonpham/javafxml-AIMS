import type { VietnamProvince, VietnamCity, DeliveryZone } from '../types/checkout';
import { DELIVERY_BUSINESS_RULES } from '../types/checkout';

// Define delivery zones based on AIMS business rules
const DELIVERY_ZONES: Record<string, DeliveryZone> = {
  HANOI_INNER: {
    id: 'hanoi_inner',
    name: 'Hanoi Inner City',
    type: 'inner_city',
    baseFee: DELIVERY_BUSINESS_RULES.INNER_CITY.BASE_FEE,
    baseWeight: DELIVERY_BUSINESS_RULES.INNER_CITY.BASE_WEIGHT,
    additionalFeePerKg: DELIVERY_BUSINESS_RULES.INNER_CITY.ADDITIONAL_FEE_PER_INCREMENT,
    rushDeliveryAvailable: DELIVERY_BUSINESS_RULES.INNER_CITY.RUSH_DELIVERY_AVAILABLE,
    rushDeliveryFee: DELIVERY_BUSINESS_RULES.INNER_CITY.RUSH_FEE_PER_ITEM,
  },
  
  HCMC_INNER: {
    id: 'hcmc_inner',
    name: 'Ho Chi Minh City Inner City',
    type: 'inner_city',
    baseFee: DELIVERY_BUSINESS_RULES.INNER_CITY.BASE_FEE,
    baseWeight: DELIVERY_BUSINESS_RULES.INNER_CITY.BASE_WEIGHT,
    additionalFeePerKg: DELIVERY_BUSINESS_RULES.INNER_CITY.ADDITIONAL_FEE_PER_INCREMENT,
    rushDeliveryAvailable: DELIVERY_BUSINESS_RULES.INNER_CITY.RUSH_DELIVERY_AVAILABLE,
    rushDeliveryFee: DELIVERY_BUSINESS_RULES.INNER_CITY.RUSH_FEE_PER_ITEM,
  },
  
  OTHER_LOCATIONS: {
    id: 'other_locations',
    name: 'Other Locations',
    type: 'province',
    baseFee: DELIVERY_BUSINESS_RULES.OUTER_LOCATIONS.BASE_FEE,
    baseWeight: DELIVERY_BUSINESS_RULES.OUTER_LOCATIONS.BASE_WEIGHT,
    additionalFeePerKg: DELIVERY_BUSINESS_RULES.OUTER_LOCATIONS.ADDITIONAL_FEE_PER_INCREMENT,
    rushDeliveryAvailable: DELIVERY_BUSINESS_RULES.OUTER_LOCATIONS.RUSH_DELIVERY_AVAILABLE,
    rushDeliveryFee: DELIVERY_BUSINESS_RULES.OUTER_LOCATIONS.RUSH_FEE_PER_ITEM,
  },
};

// Vietnam Provinces and Cities with delivery zone mapping
export const VIETNAM_PROVINCES: VietnamProvince[] = [
  {
    code: 'HN',
    name: 'Hà Nội',
    deliveryZone: DELIVERY_ZONES.HANOI_INNER,
    cities: [
      { code: 'HN_BA_DINH', name: 'Ba Đình', provinceCode: 'HN', deliveryZone: DELIVERY_ZONES.HANOI_INNER },
      { code: 'HN_HOAN_KIEM', name: 'Hoàn Kiếm', provinceCode: 'HN', deliveryZone: DELIVERY_ZONES.HANOI_INNER },
      { code: 'HN_TAY_HO', name: 'Tây Hồ', provinceCode: 'HN', deliveryZone: DELIVERY_ZONES.HANOI_INNER },
      { code: 'HN_LONG_BIEN', name: 'Long Biên', provinceCode: 'HN', deliveryZone: DELIVERY_ZONES.HANOI_INNER },
      { code: 'HN_CAU_GIAY', name: 'Cầu Giấy', provinceCode: 'HN', deliveryZone: DELIVERY_ZONES.HANOI_INNER },
      { code: 'HN_DONG_DA', name: 'Đống Đa', provinceCode: 'HN', deliveryZone: DELIVERY_ZONES.HANOI_INNER },
      { code: 'HN_HAI_BA_TRUNG', name: 'Hai Bà Trưng', provinceCode: 'HN', deliveryZone: DELIVERY_ZONES.HANOI_INNER },
      { code: 'HN_HOANG_MAI', name: 'Hoàng Mai', provinceCode: 'HN', deliveryZone: DELIVERY_ZONES.HANOI_INNER },
      { code: 'HN_THANH_XUAN', name: 'Thanh Xuân', provinceCode: 'HN', deliveryZone: DELIVERY_ZONES.HANOI_INNER },
      { code: 'HN_SOC_SON', name: 'Sóc Sơn', provinceCode: 'HN', deliveryZone: DELIVERY_ZONES.OTHER_LOCATIONS },
      { code: 'HN_DONG_ANH', name: 'Đông Anh', provinceCode: 'HN', deliveryZone: DELIVERY_ZONES.OTHER_LOCATIONS },
      { code: 'HN_GIA_LAM', name: 'Gia Lâm', provinceCode: 'HN', deliveryZone: DELIVERY_ZONES.OTHER_LOCATIONS },
      { code: 'HN_NAM_TU_LIEM', name: 'Nam Từ Liêm', provinceCode: 'HN', deliveryZone: DELIVERY_ZONES.HANOI_INNER },
      { code: 'HN_BAC_TU_LIEM', name: 'Bắc Từ Liêm', provinceCode: 'HN', deliveryZone: DELIVERY_ZONES.HANOI_INNER },
      { code: 'HN_ME_LINH', name: 'Mê Linh', provinceCode: 'HN', deliveryZone: DELIVERY_ZONES.OTHER_LOCATIONS },
      { code: 'HN_HA_DONG', name: 'Hà Đông', provinceCode: 'HN', deliveryZone: DELIVERY_ZONES.HANOI_INNER },
    ],
  },
  
  {
    code: 'HCM',
    name: 'TP. Hồ Chí Minh',
    deliveryZone: DELIVERY_ZONES.HCMC_INNER,
    cities: [
      { code: 'HCM_QUAN_1', name: 'Quận 1', provinceCode: 'HCM', deliveryZone: DELIVERY_ZONES.HCMC_INNER },
      { code: 'HCM_QUAN_2', name: 'Quận 2', provinceCode: 'HCM', deliveryZone: DELIVERY_ZONES.HCMC_INNER },
      { code: 'HCM_QUAN_3', name: 'Quận 3', provinceCode: 'HCM', deliveryZone: DELIVERY_ZONES.HCMC_INNER },
      { code: 'HCM_QUAN_4', name: 'Quận 4', provinceCode: 'HCM', deliveryZone: DELIVERY_ZONES.HCMC_INNER },
      { code: 'HCM_QUAN_5', name: 'Quận 5', provinceCode: 'HCM', deliveryZone: DELIVERY_ZONES.HCMC_INNER },
      { code: 'HCM_QUAN_6', name: 'Quận 6', provinceCode: 'HCM', deliveryZone: DELIVERY_ZONES.HCMC_INNER },
      { code: 'HCM_QUAN_7', name: 'Quận 7', provinceCode: 'HCM', deliveryZone: DELIVERY_ZONES.HCMC_INNER },
      { code: 'HCM_QUAN_8', name: 'Quận 8', provinceCode: 'HCM', deliveryZone: DELIVERY_ZONES.HCMC_INNER },
      { code: 'HCM_QUAN_9', name: 'Quận 9', provinceCode: 'HCM', deliveryZone: DELIVERY_ZONES.HCMC_INNER },
      { code: 'HCM_QUAN_10', name: 'Quận 10', provinceCode: 'HCM', deliveryZone: DELIVERY_ZONES.HCMC_INNER },
      { code: 'HCM_QUAN_11', name: 'Quận 11', provinceCode: 'HCM', deliveryZone: DELIVERY_ZONES.HCMC_INNER },
      { code: 'HCM_QUAN_12', name: 'Quận 12', provinceCode: 'HCM', deliveryZone: DELIVERY_ZONES.HCMC_INNER },
      { code: 'HCM_BINH_THANH', name: 'Bình Thạnh', provinceCode: 'HCM', deliveryZone: DELIVERY_ZONES.HCMC_INNER },
      { code: 'HCM_GO_VAP', name: 'Gò Vấp', provinceCode: 'HCM', deliveryZone: DELIVERY_ZONES.HCMC_INNER },
      { code: 'HCM_PHU_NHUAN', name: 'Phú Nhuận', provinceCode: 'HCM', deliveryZone: DELIVERY_ZONES.HCMC_INNER },
      { code: 'HCM_TAN_BINH', name: 'Tân Bình', provinceCode: 'HCM', deliveryZone: DELIVERY_ZONES.HCMC_INNER },
      { code: 'HCM_TAN_PHU', name: 'Tân Phú', provinceCode: 'HCM', deliveryZone: DELIVERY_ZONES.HCMC_INNER },
      { code: 'HCM_THU_DUC', name: 'Thủ Đức', provinceCode: 'HCM', deliveryZone: DELIVERY_ZONES.HCMC_INNER },
      { code: 'HCM_BINH_TAN', name: 'Bình Tân', provinceCode: 'HCM', deliveryZone: DELIVERY_ZONES.HCMC_INNER },
      { code: 'HCM_HOC_MON', name: 'Hóc Môn', provinceCode: 'HCM', deliveryZone: DELIVERY_ZONES.OTHER_LOCATIONS },
      { code: 'HCM_CU_CHI', name: 'Củ Chi', provinceCode: 'HCM', deliveryZone: DELIVERY_ZONES.OTHER_LOCATIONS },
      { code: 'HCM_BINH_CHANH', name: 'Bình Chánh', provinceCode: 'HCM', deliveryZone: DELIVERY_ZONES.OTHER_LOCATIONS },
      { code: 'HCM_NHA_BE', name: 'Nhà Bè', provinceCode: 'HCM', deliveryZone: DELIVERY_ZONES.OTHER_LOCATIONS },
      { code: 'HCM_CAN_GIO', name: 'Cần Giờ', provinceCode: 'HCM', deliveryZone: DELIVERY_ZONES.OTHER_LOCATIONS },
    ],
  },
  
  {
    code: 'DNA',
    name: 'Đà Nẵng',
    deliveryZone: DELIVERY_ZONES.OTHER_LOCATIONS,
    cities: [
      { code: 'DNA_HAI_CHAU', name: 'Hải Châu', provinceCode: 'DNA', deliveryZone: DELIVERY_ZONES.OTHER_LOCATIONS },
      { code: 'DNA_CAM_LE', name: 'Cẩm Lệ', provinceCode: 'DNA', deliveryZone: DELIVERY_ZONES.OTHER_LOCATIONS },
      { code: 'DNA_THANH_KHE', name: 'Thanh Khê', provinceCode: 'DNA', deliveryZone: DELIVERY_ZONES.OTHER_LOCATIONS },
      { code: 'DNA_LIEN_CHIEU', name: 'Liên Chiểu', provinceCode: 'DNA', deliveryZone: DELIVERY_ZONES.OTHER_LOCATIONS },
      { code: 'DNA_NGU_HANH_SON', name: 'Ngũ Hành Sơn', provinceCode: 'DNA', deliveryZone: DELIVERY_ZONES.OTHER_LOCATIONS },
      { code: 'DNA_SON_TRA', name: 'Sơn Trà', provinceCode: 'DNA', deliveryZone: DELIVERY_ZONES.OTHER_LOCATIONS },
      { code: 'DNA_HOA_VANG', name: 'Hòa Vang', provinceCode: 'DNA', deliveryZone: DELIVERY_ZONES.OTHER_LOCATIONS },
    ],
  },
  
  // Major provinces
  {
    code: 'AN_GIANG',
    name: 'An Giang',
    deliveryZone: DELIVERY_ZONES.OTHER_LOCATIONS,
    cities: [
      { code: 'AG_LONG_XUYEN', name: 'Long Xuyên', provinceCode: 'AN_GIANG', deliveryZone: DELIVERY_ZONES.OTHER_LOCATIONS },
      { code: 'AG_CHAU_DOC', name: 'Châu Đốc', provinceCode: 'AN_GIANG', deliveryZone: DELIVERY_ZONES.OTHER_LOCATIONS },
      { code: 'AG_AN_PHU', name: 'An Phú', provinceCode: 'AN_GIANG', deliveryZone: DELIVERY_ZONES.OTHER_LOCATIONS },
      { code: 'AG_TAN_CHAU', name: 'Tân Châu', provinceCode: 'AN_GIANG', deliveryZone: DELIVERY_ZONES.OTHER_LOCATIONS },
      { code: 'AG_PHU_TAN', name: 'Phú Tân', provinceCode: 'AN_GIANG', deliveryZone: DELIVERY_ZONES.OTHER_LOCATIONS },
    ],
  },
  
  {
    code: 'BA_RIA_VUNG_TAU',
    name: 'Bà Rịa - Vũng Tàu',
    deliveryZone: DELIVERY_ZONES.OTHER_LOCATIONS,
    cities: [
      { code: 'BRVT_VUNG_TAU', name: 'Vũng Tàu', provinceCode: 'BA_RIA_VUNG_TAU', deliveryZone: DELIVERY_ZONES.OTHER_LOCATIONS },
      { code: 'BRVT_BA_RIA', name: 'Bà Rịa', provinceCode: 'BA_RIA_VUNG_TAU', deliveryZone: DELIVERY_ZONES.OTHER_LOCATIONS },
      { code: 'BRVT_CON_DAO', name: 'Côn Đảo', provinceCode: 'BA_RIA_VUNG_TAU', deliveryZone: DELIVERY_ZONES.OTHER_LOCATIONS },
      { code: 'BRVT_CHAU_DUC', name: 'Châu Đức', provinceCode: 'BA_RIA_VUNG_TAU', deliveryZone: DELIVERY_ZONES.OTHER_LOCATIONS },
    ],
  },
  
  // Add more provinces as needed for comprehensive coverage
  {
    code: 'HAI_PHONG',
    name: 'Hải Phòng',
    deliveryZone: DELIVERY_ZONES.OTHER_LOCATIONS,
    cities: [
      { code: 'HP_HONG_BANG', name: 'Hồng Bàng', provinceCode: 'HAI_PHONG', deliveryZone: DELIVERY_ZONES.OTHER_LOCATIONS },
      { code: 'HP_LE_CHAN', name: 'Lê Chân', provinceCode: 'HAI_PHONG', deliveryZone: DELIVERY_ZONES.OTHER_LOCATIONS },
      { code: 'HP_NGO_QUYEN', name: 'Ngô Quyền', provinceCode: 'HAI_PHONG', deliveryZone: DELIVERY_ZONES.OTHER_LOCATIONS },
      { code: 'HP_KIEN_AN', name: 'Kiến An', provinceCode: 'HAI_PHONG', deliveryZone: DELIVERY_ZONES.OTHER_LOCATIONS },
      { code: 'HP_HAI_AN', name: 'Hải An', provinceCode: 'HAI_PHONG', deliveryZone: DELIVERY_ZONES.OTHER_LOCATIONS },
    ],
  },
  
  {
    code: 'CAN_THO',
    name: 'Cần Thơ',
    deliveryZone: DELIVERY_ZONES.OTHER_LOCATIONS,
    cities: [
      { code: 'CT_NINH_KIEU', name: 'Ninh Kiều', provinceCode: 'CAN_THO', deliveryZone: DELIVERY_ZONES.OTHER_LOCATIONS },
      { code: 'CT_BINH_THUY', name: 'Bình Thủy', provinceCode: 'CAN_THO', deliveryZone: DELIVERY_ZONES.OTHER_LOCATIONS },
      { code: 'CT_CAI_RANG', name: 'Cái Răng', provinceCode: 'CAN_THO', deliveryZone: DELIVERY_ZONES.OTHER_LOCATIONS },
      { code: 'CT_O_MON', name: 'Ô Môn', provinceCode: 'CAN_THO', deliveryZone: DELIVERY_ZONES.OTHER_LOCATIONS },
      { code: 'CT_THOT_NOT', name: 'Thốt Nốt', provinceCode: 'CAN_THO', deliveryZone: DELIVERY_ZONES.OTHER_LOCATIONS },
    ],
  },
];

// Helper functions for working with location data
export const locationHelpers = {
  // Find province by code
  findProvinceByCode: (code: string): VietnamProvince | undefined => {
    return VIETNAM_PROVINCES.find(province => province.code === code);
  },
  
  // Find city by code
  findCityByCode: (code: string): VietnamCity | undefined => {
    for (const province of VIETNAM_PROVINCES) {
      const city = province.cities.find(city => city.code === code);
      if (city) return city;
    }
    return undefined;
  },
  
  // Get cities for a province
  getCitiesForProvince: (provinceCode: string): VietnamCity[] => {
    const province = locationHelpers.findProvinceByCode(provinceCode);
    return province?.cities || [];
  },
  
  // Get delivery zone for location
  getDeliveryZone: (provinceCode: string, cityCode?: string): DeliveryZone => {
    if (cityCode) {
      const city = locationHelpers.findCityByCode(cityCode);
      if (city) return city.deliveryZone;
    }
    
    const province = locationHelpers.findProvinceByCode(provinceCode);
    return province?.deliveryZone || DELIVERY_ZONES.OTHER_LOCATIONS;
  },
  
  // Check if rush delivery is available for location
  isRushDeliveryAvailable: (provinceCode: string, cityCode?: string): boolean => {
    const zone = locationHelpers.getDeliveryZone(provinceCode, cityCode);
    return zone.rushDeliveryAvailable;
  },
  
  // Get all provinces sorted by name
  getAllProvinces: (): VietnamProvince[] => {
    return [...VIETNAM_PROVINCES].sort((a, b) => a.name.localeCompare(b.name));
  },
  
  // Search provinces and cities by name
  searchLocations: (query: string): Array<VietnamProvince | VietnamCity> => {
    const results: Array<VietnamProvince | VietnamCity> = [];
    const normalizedQuery = query.toLowerCase().trim();
    
    if (!normalizedQuery) return results;
    
    // Search provinces
    VIETNAM_PROVINCES.forEach(province => {
      if (province.name.toLowerCase().includes(normalizedQuery)) {
        results.push(province);
      }
      
      // Search cities within provinces
      province.cities.forEach(city => {
        if (city.name.toLowerCase().includes(normalizedQuery)) {
          results.push(city);
        }
      });
    });
    
    return results;
  },
};

// Export delivery zones for direct access
export { DELIVERY_ZONES };

// Default exports
export default {
  provinces: VIETNAM_PROVINCES,
  zones: DELIVERY_ZONES,
  helpers: locationHelpers,
};