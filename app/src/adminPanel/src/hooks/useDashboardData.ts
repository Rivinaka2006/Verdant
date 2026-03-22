import { useState, useEffect } from 'react';
import {
  collection,
  query,
  where,
  getCountFromServer,
  getAggregateFromServer,
  sum,
  orderBy,
  getDocs,
  doc,
  getDoc,
  Timestamp,
} from 'firebase/firestore';
import { db, auth } from '../lib/firebase';
import { User, Order, DashboardMetrics } from '../types';

export const useDashboardData = () => {
  const [admin, setAdmin] = useState<User | null>(null);
  const [metrics, setMetrics] = useState<DashboardMetrics>({
    totalRevenue: 0,
    totalOrders: 0,
    productsListed: 0,
    activeUsers: 0,
    monthlySalesTrend: [],
    topPerformingNurseries: [],
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchAdminData = async () => {
      const user = auth.currentUser;
      if (user) {
        try {
          const userDoc = await getDoc(doc(db, "users", user.uid));
          if (userDoc.exists()) {
            setAdmin({ ...userDoc.data(), userId: user.uid } as User);
          } else {
            console.error("Admin user document not found");
          }
        } catch (err) {
          console.error("Error fetching admin data:", err);
        }
      }
    };

    const fetchAllMetrics = async () => {
      try {
        setLoading(true);
        setError(null);

        // 1. Basic counts
        const productsQuery = query(collection(db, "products"));
        const productsCountRef = await getCountFromServer(productsQuery);
        const productsListedCount = productsCountRef.data().count;

        const ordersQuery = query(collection(db, "orders"));
        const ordersCountRef = await getCountFromServer(ordersQuery);
        const totalOrdersCount = ordersCountRef.data().count;

        // 2. Total Revenue (sum(totalAmount) where status != 'CANCELLED')
        const revenueQuery = query(
          collection(db, "orders"),
          where("status", "!=", "CANCELLED")
        );
        const revenueSumRef = await getAggregateFromServer(revenueQuery, {
          totalRevenue: sum('totalAmount')
        });
        const totalRevenueSum = revenueSumRef.data().totalRevenue || 0;

        // 3. Active Users (role != 'admin' and createdAt within last 30 days)
        const thirtyDaysAgo = new Date();
        thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);

        const activeUsersQuery = query(
          collection(db, "users"),
          where("role", "!=", "admin"), // Define admin role appropriately
          where("createdAt", ">=", Timestamp.fromDate(thirtyDaysAgo))
        );
        const activeUsersCountRef = await getCountFromServer(activeUsersQuery);
        const activeUsersCount = activeUsersCountRef.data().count;

        // 4 & 5. Monthly Sales Trend & Top Nurseries
        // We fetch the last 12 months' orders to calculate both
        const twelveMonthsAgo = new Date();
        twelveMonthsAgo.setMonth(twelveMonthsAgo.getMonth() - 12);

        const recentOrdersQuery = query(
          collection(db, "orders"),
          where("createdAt", ">=", Timestamp.fromDate(twelveMonthsAgo)),
          orderBy("createdAt", "desc")
        );

        const ordersSnapshot = await getDocs(recentOrdersQuery);
        const orders: Order[] = ordersSnapshot.docs.map(doc => {
          const data = doc.data();
          const rawStatus = typeof data.status === 'string' ? data.status : '';
          const cleanStatus = rawStatus.replace(/[\s\n\r]+/g, '').toUpperCase();

          return {
            ...data,
            status: cleanStatus || 'NEW',
            orderId: doc.id
          } as Order;
        });

        // Aggregate Monthly Sales Trend
        const monthlyTrendMap: Record<string, { sales: number; orders: number }> = {};
        const monthNames = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

        // Initialize last 12 months with 0
        const now = new Date();
        for (let i = 11; i >= 0; i--) {
          const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
          const monthKey = `${monthNames[d.getMonth()]} ${d.getFullYear()}`;
          monthlyTrendMap[monthKey] = { sales: 0, orders: 0 };
        }

        orders.forEach(order => {
          if (order.status === 'CANCELLED') return;

          const date = order.createdAt.toDate();
          const monthKey = `${monthNames[date.getMonth()]} ${date.getFullYear()}`;

          if (monthlyTrendMap[monthKey]) {
            monthlyTrendMap[monthKey].sales += order.totalAmount;
            monthlyTrendMap[monthKey].orders += 1;
          }
        });

        const monthlySalesTrend = Object.entries(monthlyTrendMap).map(([month, data]) => ({
          month: month.split(' ')[0], // UI might only want 'Jan'
          ...data
        }));

        // Aggregate Top Performing Nurseries
        // Note: Products store userId in the nurseryId field, so we aggregate by sellerId
        const nurseryRevenue: Record<string, number> = {};
        const productCache: Record<string, string> = {}; // productId -> sellerId map

        // For each successful order
        for (const order of orders) {
          if (order.status === 'CANCELLED') continue;

          for (const item of order.items) {
            let sellerId = productCache[item.productId];

            if (!sellerId) {
              const productDoc = await getDoc(doc(db, "products", item.productId));
              if (productDoc.exists()) {
                const productData = productDoc.data() as any;
                // Note: nurseryId field actually contains userId
                sellerId = productData.nurseryId;

                if (!sellerId) {
                  console.warn(`Product ${item.productId} has no nurseryId field`, productData);
                  sellerId = 'Unknown';
                }

                productCache[item.productId] = sellerId;
              } else {
                console.warn(`Product document not found for productId=${item.productId}`);
                productCache[item.productId] = 'Unknown';
                sellerId = 'Unknown';
              }
            }

            if (sellerId && sellerId !== 'Unknown') {
              const revenue = item.productPrice * item.quantity;
              nurseryRevenue[sellerId] = (nurseryRevenue[sellerId] || 0) + revenue;
            }
          }
        }

        const colorPalette = ['#98FF98', '#6bb85e', '#4d9a41', '#3d7a34', '#2D5A27'];

        const topNurseriesList = await Promise.all(
          Object.entries(nurseryRevenue)
            .sort((a, b) => b[1] - a[1])
            .slice(0, 5) // Top 5 as per UI
            .map(async ([sellerId, revenue], index) => {
              let name = 'Unknown';
              try {
                // sellerId is actually a userId, so look up in users collection
                const userDoc = await getDoc(doc(db, "users", sellerId));
                if (userDoc.exists()) {
                  const userData = userDoc.data() as any;

                  if (userData.fullName && typeof userData.fullName === 'string') {
                    name = userData.fullName;
                  } else {
                    console.warn(`User document ${sellerId} missing fullName field`, userData);
                    name = 'Unnamed Seller';
                  }
                } else {
                  console.warn(`User document not found for sellerId=${sellerId}`);
                }
              } catch (innerErr) {
                console.error(`Error loading user document for sellerId=${sellerId}`, innerErr);
              }

              return {
                name,
                revenue,
                color: colorPalette[index % colorPalette.length],
              };
            })
        );

        setMetrics({
          totalRevenue: totalRevenueSum,
          totalOrders: totalOrdersCount,
          productsListed: productsListedCount,
          activeUsers: activeUsersCount,
          monthlySalesTrend,
          topPerformingNurseries: topNurseriesList,
        });

      } catch (err: any) {
        console.error("Error calculating dashboard metrics:", err);
        setError(err.message || "Failed to load dashboard data");
      } finally {
        setLoading(false);
      }
    };

    const run = async () => {
      await fetchAdminData();
      await fetchAllMetrics();
    };

    run();
  }, []);

  return { admin, metrics, loading, error };
};
