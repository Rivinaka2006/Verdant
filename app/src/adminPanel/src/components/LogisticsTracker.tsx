import React from 'react';
import { Truck, Package, MapPin, Navigation } from 'lucide-react';

interface RoutePoint {
  id: number;
  type: 'courier' | 'delivery';
  name: string;
  status: string;
  x: number;
  y: number;
}

const routes: RoutePoint[] = [
  { id: 1, type: 'courier', name: 'Raj M.', status: 'En route', x: 25, y: 35 },
  { id: 2, type: 'courier', name: 'Priya S.', status: 'Picking up', x: 65, y: 55 },
  { id: 3, type: 'courier', name: 'Arun D.', status: 'En route', x: 45, y: 20 },
  { id: 4, type: 'delivery', name: 'ORD-1847', status: 'Awaiting', x: 30, y: 65 },
  { id: 5, type: 'delivery', name: 'ORD-1852', status: 'Awaiting', x: 70, y: 30 },
  { id: 6, type: 'delivery', name: 'ORD-1855', status: 'Awaiting', x: 80, y: 70 },
  { id: 7, type: 'courier', name: 'Meena K.', status: 'Returning', x: 55, y: 80 },
  { id: 8, type: 'delivery', name: 'ORD-1860', status: 'Awaiting', x: 15, y: 50 },
];

const LogisticsTracker: React.FC = () => {
  return (
    <div className="glass-card rounded-2xl overflow-hidden h-full flex flex-col">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between p-5 border-b border-white/5 gap-3">
        <div>
          <h3 className="text-base font-semibold text-white">Logistics Tracker</h3>
          <p className="text-xs text-dark-200 mt-0.5">Live delivery routes</p>
        </div>
        <div className="flex items-center gap-4">
          <div className="flex items-center gap-1.5 shrink-0">
            <div className="w-2.5 h-2.5 rounded-full bg-verdant-300 animate-pulse" />
            <span className="text-xs text-dark-200 font-medium">4 Couriers</span>
          </div>
          <div className="flex items-center gap-1.5 shrink-0">
            <div className="w-2.5 h-2.5 rounded-full bg-amber-400" />
            <span className="text-xs text-dark-200 font-medium">4 Deliveries</span>
          </div>
        </div>
      </div>

      {/* Map Area */}
      <div className="relative flex-1 min-h-[220px] map-grid bg-dark-800/50 m-3 rounded-xl overflow-hidden">
        {/* Roads */}
        <svg className="absolute inset-0 w-full h-full" style={{ zIndex: 0 }}>
          <line x1="10%" y1="40%" x2="90%" y2="40%" stroke="rgba(45,90,39,0.15)" strokeWidth="2" strokeDasharray="6,4" />
          <line x1="50%" y1="10%" x2="50%" y2="90%" stroke="rgba(45,90,39,0.15)" strokeWidth="2" strokeDasharray="6,4" />
          <line x1="20%" y1="70%" x2="80%" y2="25%" stroke="rgba(45,90,39,0.1)" strokeWidth="1.5" strokeDasharray="4,6" />
          <line x1="15%" y1="20%" x2="85%" y2="80%" stroke="rgba(45,90,39,0.1)" strokeWidth="1.5" strokeDasharray="4,6" />
          {/* Route lines connecting couriers to deliveries */}
          <line x1="25%" y1="35%" x2="30%" y2="65%" stroke="rgba(152,255,152,0.25)" strokeWidth="2" />
          <line x1="65%" y1="55%" x2="80%" y2="70%" stroke="rgba(152,255,152,0.25)" strokeWidth="2" />
          <line x1="45%" y1="20%" x2="70%" y2="30%" stroke="rgba(152,255,152,0.25)" strokeWidth="2" />
          <line x1="55%" y1="80%" x2="15%" y2="50%" stroke="rgba(152,255,152,0.25)" strokeWidth="2" />
        </svg>

        {/* Map Points */}
        {routes.map((point) => (
          <div
            key={point.id}
            className="absolute group cursor-pointer"
            style={{ left: `${point.x}%`, top: `${point.y}%`, transform: 'translate(-50%, -50%)', zIndex: 10 }}
          >
            {/* Ping animation for couriers */}
            {point.type === 'courier' && (
              <div className="absolute inset-0 w-8 h-8 -m-1 rounded-full bg-verdant-300/20 animate-ping" />
            )}
            <div
              className={`w-6 h-6 rounded-full flex items-center justify-center shadow-lg ${point.type === 'courier'
                  ? 'bg-verdant-600 border-2 border-verdant-300'
                  : 'bg-amber-600 border-2 border-amber-300'
                }`}
            >
              {point.type === 'courier' ? (
                <Truck className="w-3 h-3 text-white" />
              ) : (
                <Package className="w-3 h-3 text-white" />
              )}
            </div>

            {/* Tooltip */}
            <div className="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 px-2.5 py-1.5 rounded-lg bg-dark-900 border border-dark-600 opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none whitespace-nowrap shadow-xl">
              <p className="text-xs font-medium text-white">{point.name}</p>
              <p className="text-[10px] text-dark-200">{point.status}</p>
            </div>
          </div>
        ))}

        {/* Map Controls */}
        <div className="absolute top-3 right-3 flex flex-col gap-1.5 z-20">
          <button className="w-8 h-8 rounded-lg bg-dark-900/80 backdrop-blur border border-dark-600 flex items-center justify-center text-dark-200 hover:text-white transition-all cursor-pointer text-sm font-bold">
            +
          </button>
          <button className="w-8 h-8 rounded-lg bg-dark-900/80 backdrop-blur border border-dark-600 flex items-center justify-center text-dark-200 hover:text-white transition-all cursor-pointer text-sm font-bold">
            −
          </button>
          <button className="w-8 h-8 rounded-lg bg-dark-900/80 backdrop-blur border border-dark-600 flex items-center justify-center text-dark-200 hover:text-white transition-all cursor-pointer">
            <Navigation className="w-3.5 h-3.5" />
          </button>
        </div>

        {/* Location Label */}
        <div className="absolute bottom-3 left-3 flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg bg-dark-900/80 backdrop-blur border border-dark-600 z-20">
          <MapPin className="w-3 h-3 text-verdant-300" />
          <span className="text-[11px] text-dark-100 font-medium">Bengaluru Metro Area</span>
        </div>
      </div>
    </div>
  );
};

export default LogisticsTracker;
