import { doc, updateDoc } from 'firebase/firestore';
import { db } from '../lib/firebase';
import { useState } from 'react';

export const useOrderActions = () => {
    const [updating, setUpdating] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const updateOrderStatus = async (orderId: string, newStatus: string) => {
        setUpdating(true);
        setError(null);
        try {
            await updateDoc(doc(db, "orders", orderId), {
                status: newStatus.toUpperCase()
            });
            return true;
        } catch (err: any) {
            console.error("Error updating order status:", err);
            setError(err.message);
            return false;
        } finally {
            setUpdating(false);
        }
    };

    return { updateOrderStatus, updating, error };
};
