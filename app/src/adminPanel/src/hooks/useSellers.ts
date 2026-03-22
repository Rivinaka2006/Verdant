import { useState, useEffect } from 'react';
import { 
  collection, 
  query, 
  where, 
  getDocs, 
  orderBy, 
  limit, 
  startAfter, 
  getCountFromServer,
  QueryDocumentSnapshot,
  DocumentData,
  doc,
  updateDoc
} from 'firebase/firestore';
import { db } from '../lib/firebase';
import { User, Nursery } from '../types';

export interface Seller extends User {
  nursery?: Nursery;
}

export const useSellers = (pageSize: number = 10) => {
  const [sellers, setSellers] = useState<Seller[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [lastVisible, setLastVisible] = useState<QueryDocumentSnapshot<DocumentData> | null>(null);
  const [cursorStack, setCursorStack] = useState<(QueryDocumentSnapshot<DocumentData> | null)[]>([]);
  const [totalCount, setTotalCount] = useState(0);
  const [page, setPage] = useState(1);
  const [hasMore, setHasMore] = useState(false);

  const fetchSellers = async (direction: 'next' | 'prev' | 'first' = 'first') => {
    try {
      setLoading(true);
      setError(null);

      // 1. Get total count of sellers if not already done
      if (totalCount === 0) {
        const countQuery = query(
          collection(db, "users"),
          where("role", "==", "seller")
        );
        const countSnapshot = await getCountFromServer(countQuery);
        setTotalCount(countSnapshot.data().count);
      }

      let usersQuery;
      let newPage = page;
      let newStack = [...cursorStack];

      if (direction === 'first') {
        usersQuery = query(
          collection(db, "users"),
          where("role", "==", "seller"),
          orderBy("fullName"),
          limit(pageSize)
        );
        newPage = 1;
        newStack = [];
      } else if (direction === 'next' && lastVisible) {
        usersQuery = query(
          collection(db, "users"),
          where("role", "==", "seller"),
          orderBy("fullName"),
          startAfter(lastVisible),
          limit(pageSize)
        );
        newStack.push(lastVisible);
        newPage = page + 1;
      } else if (direction === 'prev' && cursorStack.length > 0) {
        const prevCursor = cursorStack.length > 1 ? cursorStack[cursorStack.length - 2] : null;
        
        if (prevCursor) {
            usersQuery = query(
              collection(db, "users"),
              where("role", "==", "seller"),
              orderBy("fullName"),
              startAfter(prevCursor),
              limit(pageSize)
            );
        } else {
            usersQuery = query(
              collection(db, "users"),
              where("role", "==", "seller"),
              orderBy("fullName"),
              limit(pageSize)
            );
        }
        
        newStack.pop();
        newPage = page - 1;
      } else {
        setLoading(false);
        return;
      }

      const snapshot = await getDocs(usersQuery);
      const userDocs = snapshot.docs;
      
      if (userDocs.length === 0) {
        setSellers([]);
        setLoading(false);
        return;
      }

      const sellersData = userDocs.map(doc => ({
        ...doc.data(),
        userId: doc.id
      })) as User[];

      // 2. Fetch corresponding Nursery data for these sellers
      const sellerIds = sellersData.map(s => s.userId);
      
      // Firestore 'in' query supports up to 30 values, which is enough for our pageSize (10)
      const nurseriesQuery = query(
        collection(db, "nurseries"),
        where("ownerId", "in", sellerIds)
      );
      
      const nurseriesSnapshot = await getDocs(nurseriesQuery);
      const nurseriesData = nurseriesSnapshot.docs.map(doc => ({
        ...doc.data(),
        nurseryId: doc.id
      })) as Nursery[];

      // Map nurseries back to sellers
      const fullSellersData: Seller[] = sellersData.map(user => {
        const nursery = nurseriesData.find(n => n.ownerId === user.userId);
        return {
          ...user,
          nursery
        };
      });

      setSellers(fullSellersData);
      setLastVisible(userDocs[userDocs.length - 1] || null);
      setCursorStack(newStack);
      setPage(newPage);
      setHasMore(userDocs.length === pageSize);
    } catch (err: any) {
      console.error("Error fetching sellers:", err);
      setError(err.message || "Failed to load sellers");
    } finally {
      setLoading(false);
    }
  };

  const toggleSellerStatus = async (userId: string, currentStatus: string) => {
    try {
      const userRef = doc(db, "users", userId);
      const newStatus = currentStatus === 'Active' ? 'Deactivated' : 'Active';
      await updateDoc(userRef, {
        status: newStatus,
        isActive: newStatus === 'Active'
      });
      
      setSellers(prev => prev.map(s => 
        s.userId === userId ? { ...s, status: newStatus, isActive: newStatus === 'Active' } : s
      ));
      return true;
    } catch (err: any) {
      console.error("Error toggling seller status:", err);
      setError(err.message || "Failed to update status");
      return false;
    }
  };

  useEffect(() => {
    fetchSellers('first');
  }, [pageSize]);

  return { sellers, loading, error, hasMore, fetchSellers, toggleSellerStatus, totalCount, page };
};
