import React from 'react';
import { DollarSign, ShoppingBag, Package, Users, TrendingUp, TrendingDown } from 'lucide-react';

interface Stat {
  label: string;
  value: string;
  change: string;
  trend: 'up' | 'down';
  icon: React.FC<{ className?: string }>;
  iconBg: string;
  iconColor: string;
}

import { DashboardMetrics } from '../types';

interface StatsCardsProps {
  metrics: DashboardMetrics;
}

const StatsCards: React.FC<StatsCardsProps> = ({ metrics }) => {
  const stats: Stat[] = [
    {
      label: 'Total Revenue',
      value: `LKR ${metrics.totalRevenue.toLocaleString()}`,
      change: '+12.5%', // Mocking change for now as schema doesn't have historical deltas
      trend: 'up',
      icon: DollarSign,
      iconBg: 'bg-verdant-700/20',
      iconColor: 'text-verdant-300',
    },
    {
      label: 'Total Orders',
      value: metrics.totalOrders.toLocaleString(),
      change: '+8.2%',
      trend: 'up',
      icon: ShoppingBag,
      iconBg: 'bg-blue-500/10',
      iconColor: 'text-blue-400',
    },
    {
      label: 'Products Listed',
      value: metrics.productsListed.toLocaleString(),
      change: '+24',
      trend: 'up',
      icon: Package,
      iconBg: 'bg-amber-500/10',
      iconColor: 'text-amber-400',
    },
    {
      label: 'Active Users',
      value: metrics.activeUsers.toLocaleString(),
      change: '-2.1%',
      trend: 'down',
      icon: Users,
      iconBg: 'bg-purple-500/10',
      iconColor: 'text-purple-400',
    },
  ];

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      {stats.map((stat, idx) => {
        const Icon = stat.icon;
        return (
          <div
            key={stat.label}
            className="glass-card glass-card-hover rounded-2xl p-5 transition-all duration-300 animate-slide-in cursor-pointer"
            style={{ animationDelay: `${idx * 80}ms` }}
          >
            <div className="flex items-center justify-between mb-4">
              <div className={`w-11 h-11 rounded-xl ${stat.iconBg} flex items-center justify-center`}>
                <Icon className={`w-5 h-5 ${stat.iconColor}`} />
              </div>
              <span
                className={`flex items-center gap-0.5 text-xs font-semibold px-2 py-1 rounded-full ${stat.trend === 'up'
                  ? 'text-emerald-400 bg-emerald-500/10'
                  : 'text-red-400 bg-red-500/10'
                  }`}
              >
                {stat.trend === 'up' ? (
                  <TrendingUp className="w-3 h-3" />
                ) : (
                  <TrendingDown className="w-3 h-3" />
                )}
                {stat.change}
              </span>
            </div>
            <p className="text-2xl font-bold text-white mb-1">{stat.value}</p>
            <p className="text-xs text-dark-200 font-medium">{stat.label}</p>
          </div>
        );
      })}
    </div>
  );
};

export default StatsCards;
