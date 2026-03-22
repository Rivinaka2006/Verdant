import React, { useState, useMemo } from 'react';
import { Clock, Loader2, Truck, CheckCircle2, AlertCircle, Search, X, Package, User, MapPin, CreditCard, ChevronRight, Ban, ArrowRightCircle } from 'lucide-react';
import { useOrders } from '../hooks/useOrders';
import { useOrderActions } from '../hooks/useOrderActions';
import { formatDistanceToNow } from 'date-fns';
import { Order } from '../types';

interface Column {
  id: string;
  title: string;
  icon: React.FC<{ className?: string }>;
  color: string;
  dotColor: string;
  statuses: string[];
}

const COLUMNS_CONFIG: Column[] = [
  {
    id: 'new',
    title: 'New',
    icon: Clock,
    color: 'text-blue-400',
    dotColor: 'bg-blue-400',
    statuses: ['new', 'NEW', 'pending', 'PENDING'],
  },
  {
    id: 'processing',
    title: 'Processing',
    icon: Loader2,
    color: 'text-amber-400',
    dotColor: 'bg-amber-400',
    statuses: ['processing', 'PROCESSING', 'preparing', 'PREPARING'],
  },
  {
    id: 'shipped',
    title: 'Shipped',
    icon: Truck,
    color: 'text-purple-400',
    dotColor: 'bg-purple-400',
    statuses: ['shipping', 'SHIPPING', 'shipped', 'SHIPPED', 'out_for_delivery', 'OUT_FOR_DELIVERY', 'DELIVERING'],
  },
  {
    id: 'delivered',
    title: 'Delivered',
    icon: CheckCircle2,
    color: 'text-emerald-400',
    dotColor: 'bg-emerald-400',
    statuses: ['completed', 'COMPLETED', 'delivered', 'DELIVERED'],
  },
  {
    id: 'cancelled',
    title: 'Cancelled',
    icon: Ban,
    color: 'text-red-400',
    dotColor: 'bg-red-400',
    statuses: ['cancelled', 'CANCELLED'],
  },
];

const OrderBoard: React.FC = () => {
  const { orders, loading, error } = useOrders();
  const { updateOrderStatus, updating } = useOrderActions();
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null);

  const filteredOrders = useMemo(() => {
    if (!searchQuery.trim()) return orders;
    const query = searchQuery.toLowerCase();
    return orders.filter(order => 
      order.orderId.toLowerCase().includes(query) || 
      order.address.toLowerCase().includes(query) ||
      order.status.toLowerCase().includes(query)
    );
  }, [orders, searchQuery]);

  const handleStatusUpdate = async (orderId: string, nextStatus: string) => {
    const success = await updateOrderStatus(orderId, nextStatus);
    if (success && selectedOrder && selectedOrder.orderId === orderId) {
      setSelectedOrder({ ...selectedOrder, status: nextStatus });
    }
  };

  const getNextStatus = (currentStatus: string): string | null => {
    const status = currentStatus.toUpperCase();
    if (['NEW', 'PENDING'].includes(status)) return 'PROCESSING';
    if (['PROCESSING', 'PREPARING'].includes(status)) return 'SHIPPED';
    if (['SHIPPING', 'SHIPPED', 'OUT_FOR_DELIVERY', 'DELIVERING'].includes(status)) return 'DELIVERED';
    return null;
  };

  if (loading) {
    return (
      <div className="glass-card rounded-2xl p-20 flex flex-col items-center justify-center space-y-4">
        <Loader2 className="w-8 h-8 text-verdant-500 animate-spin" />
        <p className="text-dark-200 text-sm animate-pulse">Fetching live orders...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="glass-card rounded-2xl p-20 flex flex-col items-center justify-center space-y-4 text-center">
        <AlertCircle className="w-10 h-10 text-red-400" />
        <p className="text-white font-semibold">Error Loading Orders</p>
        <p className="text-dark-300 text-xs max-w-xs">{error}</p>
      </div>
    );
  }

  return (
    <div className="space-y-4 relative">
      {/* Header & Search */}
      <div className="glass-card rounded-2xl p-5 flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h3 className="text-base font-semibold text-white font-display uppercase tracking-wider">Live Order Board</h3>
          <p className="text-[10px] text-dark-300 font-bold uppercase tracking-widest mt-1">
            {filteredOrders.length} orders match your view
          </p>
        </div>
        
        <div className="relative group min-w-[300px]">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-dark-400 group-focus-within:text-verdant-400 transition-colors" />
          <input 
            type="text" 
            placeholder="Search by ID, status or address..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full bg-dark-900/50 border border-white/5 rounded-xl py-2 pl-10 pr-4 text-sm focus:outline-none focus:border-verdant-500/50 focus:ring-1 focus:ring-verdant-500/30 transition-all placeholder:text-dark-500"
          />
        </div>
      </div>

      {/* Status Summary Section */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        {[
          { label: 'Total Shipped', status: 'shipped', color: 'text-purple-400', icon: Truck, bg: 'bg-purple-500/10', glow: 'shadow-[0_0_15px_rgba(192,132,252,0.1)]' },
          { label: 'Total Delivered', status: 'delivered', color: 'text-emerald-400', icon: CheckCircle2, bg: 'bg-emerald-500/10', glow: 'shadow-[0_0_15px_rgba(52,211,153,0.1)]' },
          { label: 'Total Cancelled', status: 'cancelled', color: 'text-red-400', icon: Ban, bg: 'bg-red-500/10', glow: 'shadow-[0_0_15px_rgba(248,113,113,0.1)]' },
        ].map((item) => {
          const count = orders.filter(o => COLUMNS_CONFIG.find(c => c.id === item.status)?.statuses.includes(o.status)).length;
          const Icon = item.icon;
          return (
            <div key={item.label} className={`glass-card rounded-2xl p-5 flex items-center gap-5 border border-white/5 transition-all hover:translate-y-[-2px] ${item.glow}`}>
              <div className={`w-14 h-14 rounded-2xl ${item.bg} flex items-center justify-center border border-white/5 shadow-inner`}>
                <Icon className={`w-7 h-7 ${item.color}`} />
              </div>
              <div>
                <p className="text-[10px] font-black uppercase tracking-[0.25em] text-dark-400 mb-1">{item.label}</p>
                <div className="flex items-baseline gap-2">
                  <h4 className="text-3xl font-black text-white">{count}</h4>
                  <span className="text-[10px] text-dark-500 font-bold uppercase tracking-widest">Orders</span>
                </div>
              </div>
            </div>
          );
        })}
      </div>

      <div className="glass-card rounded-2xl overflow-hidden divide-y divide-white/5">
        <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-5 gap-0 md:divide-x divide-white/5 divide-y md:divide-y-0">
          {COLUMNS_CONFIG.map((col) => {
            const colOrders = filteredOrders.filter(o => col.statuses.includes(o.status));

            return (
              <div key={col.id} className="p-3 bg-white/[0.01]">
                <div className="flex items-center gap-2 mb-4 px-2 py-1.5 rounded-lg bg-dark-900/50 border border-white/[0.03]">
                  <div className={`w-1.5 h-1.5 rounded-full ${col.dotColor} animate-pulse`} />
                  <span className={`text-[10px] font-bold uppercase tracking-[0.15em] ${col.color}`}>
                    {col.title}
                  </span>
                  <span className="ml-auto text-[10px] text-dark-200 bg-dark-700/50 px-2 py-0.5 rounded-full font-bold border border-white/5">
                    {colOrders.length}
                  </span>
                </div>

                <div className="space-y-3 max-h-[600px] overflow-y-auto px-1 custom-scrollbar pb-2">
                  {colOrders.length === 0 ? (
                    <div className="py-10 text-center opacity-30">
                      <p className="text-[10px] font-bold uppercase tracking-widest">No Orders</p>
                    </div>
                  ) : (
                    colOrders.map((order, idx) => {
                      const timeStr = order.createdAt ? formatDistanceToNow(order.createdAt.toDate(), { addSuffix: true }) : 'N/A';
                      const itemsCount = order.items.reduce((acc, item) => acc + item.quantity, 0);
                      const itemsLabel = itemsCount === 1 ? '1 Item' : `${itemsCount} Items`;

                      return (
                        <div
                          key={order.orderId}
                          onClick={() => setSelectedOrder(order)}
                          className="p-3.5 rounded-xl bg-dark-800/40 border border-white/[0.04] hover:border-verdant-800/30 hover:bg-dark-800/60 transition-all cursor-pointer group animate-fade-in relative overflow-hidden"
                          style={{ animationDelay: `${idx * 60}ms` }}
                        >
                          <div className="absolute top-0 right-0 p-1 opacity-0 group-hover:opacity-100 transition-opacity">
                              <ChevronRight className="w-3 h-3 text-dark-300" />
                          </div>
                          <div className="flex items-center justify-between mb-2.5">
                            <span className="text-[10px] font-mono font-bold text-dark-200 bg-dark-700/50 px-1.5 py-0.5 rounded uppercase tracking-tighter">#{order.orderId.substring(0, 8).toUpperCase()}</span>
                            <span className="text-[9px] font-bold text-dark-400 uppercase tracking-tighter">{timeStr}</span>
                          </div>
                          
                          <p className="text-xs font-bold text-white mb-2 leading-tight group-hover:text-verdant-300 transition-colors uppercase tracking-wide truncate">
                            {order.address.split(',')[0]}
                          </p>
                          
                          <div className="flex items-center justify-between mt-auto pt-2 border-t border-white/[0.03]">
                            <span className="text-[10px] text-dark-300 font-semibold">{itemsLabel}</span>
                            <span className="text-xs font-bold text-verdant-400">LKR {order.totalAmount.toLocaleString()}</span>
                          </div>
                        </div>
                      );
                    })
                  )}
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Detail Overlay */}
      {selectedOrder && (
        <div className="fixed inset-0 z-50 flex items-center justify-end bg-black/60 backdrop-blur-sm p-4 animate-fade-in">
          <div 
            className="w-full max-w-lg bg-dark-900 border border-white/10 rounded-3xl h-full flex flex-col shadow-2xl relative overflow-hidden animate-slide-in"
            onClick={(e) => e.stopPropagation()}
          >
            {/* Header */}
            <div className="p-6 border-b border-white/5 flex items-center justify-between bg-dark-800/50">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-verdant-500/20 flex items-center justify-center border border-verdant-500/30">
                  <Package className="w-5 h-5 text-verdant-400" />
                </div>
                <div>
                  <h2 className="text-lg font-bold text-white uppercase tracking-tight">Order Details</h2>
                  <p className="text-xs text-dark-300 font-mono">#{selectedOrder.orderId.toUpperCase()}</p>
                </div>
              </div>
              <button 
                onClick={() => setSelectedOrder(null)}
                className="w-8 h-8 rounded-lg bg-dark-800 flex items-center justify-center text-dark-300 hover:text-white transition-colors cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* Content */}
            <div className="flex-1 overflow-y-auto p-6 space-y-8 custom-scrollbar">
              {/* Status Section */}
              <section>
                <div className="flex items-center justify-between mb-4">
                  <h4 className="text-[10px] font-bold uppercase tracking-[0.2em] text-dark-300">Current Status</h4>
                  <span className={`px-3 py-1 rounded-full text-[10px] font-bold uppercase tracking-wider bg-dark-800 border ${
                    COLUMNS_CONFIG.find(c => c.statuses.includes(selectedOrder.status))?.color || 'text-white'
                  }`}>
                    {selectedOrder.status.replace('_', ' ')}
                  </span>
                </div>
                
                <div className="grid grid-cols-2 gap-3">
                  {getNextStatus(selectedOrder.status) && (
                    <button 
                      disabled={updating}
                      onClick={() => handleStatusUpdate(selectedOrder.orderId, getNextStatus(selectedOrder.status)!)}
                      className="flex items-center justify-center gap-2 bg-verdant-600 hover:bg-verdant-500 text-white py-3 rounded-xl font-bold text-xs transition-all disabled:opacity-50 cursor-pointer"
                    >
                      {updating ? <Loader2 className="w-4 h-4 animate-spin" /> : <ArrowRightCircle className="w-4 h-4" />}
                      MARK AS {getNextStatus(selectedOrder.status)}
                    </button>
                  )}
                  {selectedOrder.status !== 'CANCELLED' && selectedOrder.status !== 'COMPLETED' && (
                    <button 
                      disabled={updating}
                      onClick={() => handleStatusUpdate(selectedOrder.orderId, 'CANCELLED')}
                      className="flex items-center justify-center gap-2 bg-red-500/10 hover:bg-red-500/20 text-red-400 border border-red-500/20 py-3 rounded-xl font-bold text-xs transition-all disabled:opacity-50 cursor-pointer"
                    >
                      <Ban className="w-4 h-4" />
                      CANCEL ORDER
                    </button>
                  )}
                </div>
              </section>

              {/* Items Section */}
              <section className="space-y-4">
                <h4 className="text-[10px] font-bold uppercase tracking-[0.2em] text-dark-300">Order Items ({selectedOrder.items.length})</h4>
                <div className="space-y-3">
                  {selectedOrder.items.map((item, idx) => (
                    <div key={idx} className="flex items-center gap-4 bg-dark-800/30 p-3 rounded-2xl border border-white/[0.03]">
                      <div className="w-14 h-14 rounded-xl bg-dark-700 overflow-hidden shrink-0 border border-white/5">
                        <img src={item.productImage} alt={item.productName} className="w-full h-full object-cover" />
                      </div>
                      <div className="flex-1 min-w-0">
                        <h5 className="text-xs font-bold text-white truncate uppercase tracking-wide">{item.productName}</h5>
                        <p className="text-[10px] text-dark-300 font-semibold mt-0.5">Quantity: {item.quantity}</p>
                      </div>
                      <div className="text-right">
                        <p className="text-xs font-bold text-verdant-400">LKR {item.productPrice.toLocaleString()}</p>
                        <p className="text-[9px] text-dark-400 font-bold uppercase">Each</p>
                      </div>
                    </div>
                  ))}
                </div>
              </section>

              {/* Customer & Shipping */}
              <section className="grid grid-cols-1 gap-6">
                <div className="space-y-4">
                  <h4 className="text-[10px] font-bold uppercase tracking-[0.2em] text-dark-300 flex items-center gap-2">
                    <User className="w-3 h-3" /> Delivery Information
                  </h4>
                  <div className="bg-dark-800/30 p-4 rounded-2xl border border-white/[0.03] space-y-4">
                    <div className="flex items-start gap-3">
                      <MapPin className="w-4 h-4 text-dark-400 mt-0.5" />
                      <div>
                        <p className="text-xs font-bold text-white leading-relaxed">{selectedOrder.address}</p>
                      </div>
                    </div>
                    <div className="flex items-center gap-3 pt-3 border-t border-white/5">
                      <CreditCard className="w-4 h-4 text-dark-400" />
                      <p className="text-[10px] font-bold text-dark-200 uppercase tracking-widest">
                        Paid via {selectedOrder.paymentMethod.replace('_', ' ')}
                      </p>
                    </div>
                  </div>
                </div>
              </section>
            </div>

            {/* Footer Summary */}
            <div className="p-6 bg-dark-800/80 border-t border-white/5 space-y-3">
                <div className="flex justify-between items-center text-dark-300">
                    <span className="text-[10px] font-bold uppercase tracking-widest">Subtotal</span>
                    <span className="text-xs font-bold font-mono">LKR {(selectedOrder.totalAmount - selectedOrder.shippingFee).toLocaleString()}</span>
                </div>
                <div className="flex justify-between items-center text-dark-300">
                    <span className="text-[10px] font-bold uppercase tracking-widest">Shipping Fee</span>
                    <span className="text-xs font-bold font-mono">LKR {selectedOrder.shippingFee.toLocaleString()}</span>
                </div>
                <div className="flex justify-between items-center pt-2 border-t border-white/10">
                    <span className="text-xs font-black uppercase tracking-widest text-white">Grand Total</span>
                    <span className="text-base font-black text-verdant-400 font-mono">LKR {selectedOrder.totalAmount.toLocaleString()}</span>
                </div>
            </div>
          </div>
          {/* Backdrop Click */}
          <div className="absolute inset-0 -z-10" onClick={() => setSelectedOrder(null)} />
        </div>
      )}
    </div>
  );
};

export default OrderBoard;

