import { Timestamp } from "firebase/firestore";

export interface User {
  userId: string;
  fullName: string;
  email: string;
  phone: string;
  role: string;
  profileImageUrl: string;
  address: string;
  billing: Address;
  shipping: Address;
  sameAsBilling: boolean;
  defaultAddressId: string;
  fcmToken: string;
  biometricEnabled: boolean;
  isActive: boolean;
  status: string;
  createdAt: Timestamp;
}

export interface Address {
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  address: string;
  city: string;
  country: string;
  postalCode: string;
}

export interface Product {
  productId: string;
  name: string;
  description: string;
  category: string;
  price: number;
  oldPrice: number;
  stock: number;
  available: boolean;
  nurseryId: string;
  imageUrls: string[];
  videoUrl: string;
  lightRequirement: string;
  waterFrequency: string;
  careInstructions: string;
  rating: number;
  soldCount: number;
  createdAt: Timestamp;
}

export interface Order {
  orderId: string;
  userId: string;
  items: OrderItem[];
  totalAmount: number;
  shippingFee: number;
  address: string;
  paymentMethod: string;
  status: string;
  createdAt: Timestamp;
}

export interface OrderItem {
  productId: string;
  productName: string;
  productPrice: number;
  quantity: number;
  productImage: string;
  available: boolean;
}

export interface Nursery {
  nurseryId: string;
  nurseryName: string;
  ownerId: string;
  description: string;
  bannerImageUrl: string;
  phoneNumber: string;
  latitude: number;
  longitude: number;
  businessHours: Record<string, string>;
  ratingAverage: number;
  totalReviews: number;
  time: Timestamp;
  createdAt: Timestamp;
}

export interface DashboardMetrics {
  totalRevenue: number;
  totalOrders: number;
  productsListed: number;
  activeUsers: number;
  monthlySalesTrend: { month: string; sales: number; orders: number }[];
  topPerformingNurseries: { name: string; revenue: number; color: string }[];
}

export interface Banner {
  id: string;
  imageUrl: string;
  title?: string;
  subtitle?: string;
  link?: string;
  active: boolean;
  order: number;
  createdAt: Timestamp;
}
