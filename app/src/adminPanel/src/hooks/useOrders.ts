import { useState, useEffect } from 'react';
import { collection, query, onSnapshot, orderBy } from 'firebase/firestore';
import { db } from '../lib/firebase';
import { Order } from '../types';

export const useOrders = () => {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const q = query(collection(db, "orders"), orderBy("createdAt", "desc"));
    
    const unsubscribe = onSnapshot(q, (snapshot) => {
      const orderList: Order[] = snapshot.docs.map(doc => {
        const data = doc.data();
        
        // Normalize status: remove all whitespace and newlines, convert to UPPERCASE
        const rawStatus = typeof data.status === 'string' ? data.status : '';
        const cleanStatus = rawStatus.replace(/[\s\n\r]+/g, '').toUpperCase();

        // Handle createdAt safely (ensure it has toDate() for the UI)
        let createdAt = data.createdAt;
        if (!createdAt || typeof createdAt.toDate !== 'function') {
          // If it's a date string or JS date, wrap it; if missing, use current date
          const dateValue = createdAt ? (createdAt.toDate ? createdAt.toDate() : new Date(createdAt)) : new Date();
          createdAt = { toDate: () => isNaN(dateValue.getTime()) ? new Date() : dateValue };
        }

        return {
          ...data,
          orderId: doc.id,
          status: cleanStatus || 'NEW',
          createdAt,
          address: data.address || 'No Address Provided',
          items: data.items || [],
          totalAmount: data.totalAmount || 0,
          shippingFee: data.shippingFee || 0,
          paymentMethod: data.paymentMethod || 'UNKNOWN'
        } as Order;
      });
      
      setOrders(orderList);
      setLoading(false);
    }, (err) => {
      console.error("Error fetching orders:", err);
      setError(err.message);
      setLoading(false);
    });

    return () => unsubscribe();
  }, []);

  return { orders, loading, error };
};
