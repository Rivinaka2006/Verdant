import React from 'react';
import {
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  AreaChart,
  Area,
  BarChart,
  Bar,
} from 'recharts';
import { TrendingUp, ArrowUpRight } from 'lucide-react';



const CustomTooltip = ({ active, payload, label }: any) => {
  if (active && payload && payload.length) {
    return (
      <div className="bg-dark-900 border border-dark-600 rounded-xl px-3 py-2 shadow-xl">
        <p className="text-xs font-medium text-dark-200">{label}</p>
        {payload.map((entry: any, idx: number) => (
          <p key={idx} className="text-sm font-semibold" style={{ color: entry.color }}>
            {entry.name === 'sales' ? `LKR ${entry.value.toLocaleString()}` : entry.value}
          </p>
        ))}
      </div>
    );
  }
  return null;
};

import { DashboardMetrics } from '../types';

interface AnalyticsWidgetsProps {
  metrics: DashboardMetrics;
}

const AnalyticsWidgets: React.FC<AnalyticsWidgetsProps> = ({ metrics }) => {
  const latestMonthSales = metrics.monthlySalesTrend.length > 0
    ? metrics.monthlySalesTrend[metrics.monthlySalesTrend.length - 1].sales
    : 0;

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
      {/* Monthly Sales Trend */}
      <div className="glass-card rounded-2xl p-5">
        <div className="flex items-center justify-between mb-4">
          <div>
            <h3 className="text-base font-semibold text-white">Monthly Sales Trend</h3>
            <div className="flex items-center gap-2 mt-1">
              <span className="text-2xl font-bold text-white">LKR {latestMonthSales.toLocaleString()}</span>
              <span className="flex items-center gap-0.5 text-xs font-medium text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded-full">
                <TrendingUp className="w-3 h-3" />
                +18.2%
              </span>
            </div>
          </div>
          <button className="flex items-center gap-1 text-xs text-verdant-300 hover:text-verdant-200 transition-colors cursor-pointer font-medium">
            View Report <ArrowUpRight className="w-3.5 h-3.5" />
          </button>
        </div>

        <div className="h-[180px]">
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={metrics.monthlySalesTrend} margin={{ top: 5, right: 5, left: -20, bottom: 0 }}>
              <defs>
                <linearGradient id="salesGradient" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="#98FF98" stopOpacity={0.3} />
                  <stop offset="100%" stopColor="#98FF98" stopOpacity={0} />
                </linearGradient>
              </defs>
              <XAxis
                dataKey="month"
                axisLine={false}
                tickLine={false}
                tick={{ fill: '#666', fontSize: 11 }}
              />
              <YAxis
                axisLine={false}
                tickLine={false}
                tick={{ fill: '#666', fontSize: 11 }}
                tickFormatter={(v) => `LKR ${v / 1000}k`}
              />
              <Tooltip content={<CustomTooltip />} />
              <Area
                type="monotone"
                dataKey="sales"
                stroke="#98FF98"
                strokeWidth={2.5}
                fill="url(#salesGradient)"
                dot={false}
                activeDot={{ r: 5, fill: '#98FF98', stroke: '#121212', strokeWidth: 2 }}
              />
            </AreaChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Top Performing Nurseries */}
      <div className="glass-card rounded-2xl p-5">
        <div className="flex items-center justify-between mb-4">
          <div>
            <h3 className="text-base font-semibold text-white">Top Performing Nurseries</h3>
            <p className="text-xs text-dark-200 mt-1">Revenue breakdown by partner nursery</p>
          </div>
          <button className="flex items-center gap-1 text-xs text-verdant-300 hover:text-verdant-200 transition-colors cursor-pointer font-medium">
            All Nurseries <ArrowUpRight className="w-3.5 h-3.5" />
          </button>
        </div>

        <div className="h-[180px]">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={metrics.topPerformingNurseries} margin={{ top: 5, right: 5, left: -20, bottom: 0 }} barCategoryGap="25%">
              <XAxis
                dataKey="name"
                axisLine={false}
                tickLine={false}
                tick={{ fill: '#666', fontSize: 10 }}
                interval={0}
              />
              <YAxis
                axisLine={false}
                tickLine={false}
                tick={{ fill: '#666', fontSize: 11 }}
                tickFormatter={(v) => `LKR ${v / 1000}k`}
              />
              <Tooltip content={<CustomTooltip />} />
              <Bar dataKey="revenue" radius={[6, 6, 0, 0]} fill="#2D5A27">
                {metrics.topPerformingNurseries.map((entry, index) => (
                  <rect key={index} fill={entry.color} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>
    </div>
  );
};

export default AnalyticsWidgets;
