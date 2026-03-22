import React, { useState, useEffect } from 'react';
import { Edit3, Trash2, Filter, Plus, ArrowUpDown, Loader2 } from 'lucide-react';
import { useProducts } from '../hooks/useProducts';
import { doc, updateDoc, deleteDoc } from 'firebase/firestore';
import { db } from '../lib/firebase';
import { Product } from '../types';
import ProductModal from './ProductModal';

const categoryColors: Record<string, string> = {
  'Indoor Plants': 'bg-emerald-500/15 text-emerald-400 border-emerald-500/20',
  'Indoor': 'bg-emerald-500/15 text-emerald-400 border-emerald-500/20',
  'Outdoor Plants': 'bg-blue-500/15 text-blue-400 border-blue-500/20',
  'Outdoor': 'bg-blue-500/15 text-blue-400 border-blue-500/20',
  'Succulents': 'bg-amber-500/15 text-amber-400 border-amber-500/20',
  'Succulent': 'bg-amber-500/15 text-amber-400 border-amber-500/20',
  'Flowering Plants': 'bg-pink-500/15 text-pink-400 border-pink-500/20',
  'Flowering': 'bg-pink-500/15 text-pink-400 border-pink-500/20',
  'Herbs': 'bg-teal-500/15 text-teal-400 border-teal-500/20',
};

const InventoryTable: React.FC = () => {
  const { products, loading } = useProducts();
  const [stockToggles, setStockToggles] = useState<Record<string, boolean>>({});
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('All');
  const [isFilterOpen, setIsFilterOpen] = useState(false);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingProduct, setEditingProduct] = useState<Product | null>(null);

  useEffect(() => {
    if (products.length > 0) {
      setStockToggles(Object.fromEntries(products.map((p) => [p.productId, p.available])));
    }
  }, [products]);

  const toggleStock = async (id: string, currentStatus: boolean) => {
    try {
      setStockToggles((prev) => ({ ...prev, [id]: !currentStatus }));
      await updateDoc(doc(db, "products", id), {
        available: !currentStatus
      });
    } catch (err) {
      console.error("Failed to update stock status:", err);
      // Revert on failure
      setStockToggles((prev) => ({ ...prev, [id]: currentStatus }));
    }
  };
  const deleteProduct = async (id: string, name: string) => {
    if (window.confirm(`Are you sure you want to delete "${name}"? This action cannot be undone.`)) {
      try {
        await deleteDoc(doc(db, "products", id));
      } catch (err) {
        console.error("Failed to delete product:", err);
        alert("Failed to delete product. Please try again.");
      }
    }
  };

  const filteredProducts = products.filter(product => {
    const matchesSearch = product.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      product.productId.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesCategory = selectedCategory === 'All' || 
      product.category === selectedCategory ||
      (selectedCategory === 'Indoor Plants' && product.category === 'Indoor') ||
      (selectedCategory === 'Outdoor Plants' && product.category === 'Outdoor') ||
      (selectedCategory === 'Succulents' && product.category === 'Succulent') ||
      (selectedCategory === 'Flowering Plants' && product.category === 'Flowering');
    return matchesSearch && matchesCategory;
  });

  const categories = ['All', 'Indoor Plants', 'Outdoor Plants', 'Succulents', 'Flowering Plants', 'Herbs'];

  if (loading) {
    return (
      <div className="glass-card rounded-2xl p-20 flex flex-col items-center justify-center space-y-4">
        <Loader2 className="w-8 h-8 text-verdant-500 animate-spin" />
        <p className="text-dark-200 text-sm animate-pulse">Synchronizing inventory data...</p>
      </div>
    );
  }

  return (
    <>
      <div className="glass-card rounded-2xl overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between p-5 border-b border-white/5">
          <div>
            <h3 className="text-base font-semibold text-white">Inventory Control</h3>
            <p className="text-xs text-dark-200 mt-0.5">
              {filteredProducts.length} products found · {filteredProducts.filter((p) => p.available && p.stock > 0).length} in stock
            </p>
          </div>
          <div className="flex items-center gap-3">
            <div className="relative group">
              <input
                type="text"
                placeholder="Search products..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-48 md:w-64 px-4 py-2 pl-10 rounded-xl bg-dark-700/50 border border-white/5 text-dark-100 text-xs font-semibold focus:outline-none focus:border-verdant-500/50 focus:bg-dark-700 transition-all"
              />
              <Filter className="absolute left-3.5 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-dark-400" />
            </div>
  
            <div className="relative">
              <button
                onClick={() => setIsFilterOpen(!isFilterOpen)}
                className={`flex items-center gap-2 px-4 py-2 rounded-xl bg-dark-700/50 border border-white/5 text-dark-100 text-xs font-semibold hover:bg-dark-600 transition-all cursor-pointer ${isFilterOpen ? 'border-verdant-500/50 text-white' : ''}`}
              >
                Category: {selectedCategory}
              </button>
  
              {isFilterOpen && (
                <div className="absolute right-0 mt-2 w-48 rounded-xl bg-dark-800 border border-white/10 shadow-xl z-20 py-1 overflow-hidden animate-in fade-in slide-in-from-top-2 duration-200">
                  {categories.map((category) => (
                    <button
                      key={category}
                      onClick={() => {
                        setSelectedCategory(category);
                        setIsFilterOpen(false);
                      }}
                      className={`w-full text-left px-4 py-2 text-xs hover:bg-white/5 transition-colors ${selectedCategory === category ? 'text-verdant-400 bg-verdant-500/5' : 'text-dark-200'}`}
                    >
                      {category}
                    </button>
                  ))}
                </div>
              )}
            </div>
  
            <button
              onClick={() => {
                setEditingProduct(null);
                setIsModalOpen(true);
              }}
              className="flex items-center gap-2 px-4 py-2 rounded-xl bg-verdant-700 text-white text-xs font-semibold hover:bg-verdant-600 transition-all glow-green cursor-pointer shadow-lg shadow-verdant-900/20"
            >
              <Plus className="w-3.5 h-3.5" />
              Add Plant
            </button>
          </div>
        </div>
  
        {/* Table */}
        <div className="overflow-x-auto custom-scrollbar">
          <table className="w-full">
            <thead>
              <tr className="border-b border-white/5 bg-white/[0.01]">
                <th className="text-left px-6 py-4 text-[10px] font-bold text-dark-300 uppercase tracking-[0.1em]">
                  <div className="flex items-center gap-2 cursor-pointer hover:text-white transition-colors">
                    Plant <ArrowUpDown className="w-3 h-3" />
                  </div>
                </th>
                <th className="hidden md:table-cell text-left px-4 py-4 text-[10px] font-bold text-dark-300 uppercase tracking-[0.1em]">SKU</th>
                <th className="hidden sm:table-cell text-left px-4 py-4 text-[10px] font-bold text-dark-300 uppercase tracking-[0.1em]">Category</th>
                <th className="text-right px-4 py-4 text-[10px] font-bold text-dark-300 uppercase tracking-[0.1em]">Price</th>
                <th className="text-right px-4 py-4 text-[10px] font-bold text-dark-300 uppercase tracking-[0.1em]">Stock</th>
                <th className="hidden sm:table-cell text-center px-4 py-4 text-[10px] font-bold text-dark-300 uppercase tracking-[0.1em]">Live Status</th>
                <th className="text-center px-6 py-4 text-[10px] font-bold text-dark-300 uppercase tracking-[0.1em]">Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredProducts.length === 0 ? (
                <tr>
                  <td colSpan={7} className="px-6 py-20 text-center">
                    <div className="flex flex-col items-center justify-center opacity-40">
                      <p className="text-sm">No products found matching your criteria.</p>
                    </div>
                  </td>
                </tr>
              ) : (
                filteredProducts.map((plant, idx) => (
                  <tr
                    key={plant.productId}
                    className="border-b border-white/[0.03] hover:bg-white/[0.02] transition-colors animate-fade-in group"
                    style={{ animationDelay: `${idx * 40}ms` }}
                  >
                    <td className="px-6 py-3.5">
                      <div className="flex items-center gap-4">
                        <div className="w-12 h-12 rounded-xl bg-dark-700/50 border border-white/5 flex items-center justify-center relative overflow-hidden group-hover:border-verdant-800/30 transition-all">
                          {plant.imageUrls && plant.imageUrls.length > 0 ? (
                            <img src={plant.imageUrls[0]} alt={plant.name} className="w-full h-full object-cover" />
                          ) : (
                            <span className="text-xl">🪴</span>
                          )}
                          {plant.stock === 0 && (
                            <div className="absolute inset-0 bg-dark-950/60 backdrop-blur-[1px] flex items-center justify-center">
                              <span className="text-[8px] font-bold text-white uppercase tracking-tighter">Empty</span>
                            </div>
                          )}
                        </div>
                        <div className="min-w-0">
                          <p className="text-sm font-semibold text-white group-hover:text-verdant-300 transition-colors truncate">{plant.name}</p>
                          <p className="md:hidden text-[10px] text-dark-300 font-mono mt-0.5 truncate max-w-[100px]">{plant.productId.substring(0, 8)}</p>
                        </div>
                      </div>
                    </td>
                    <td className="hidden md:table-cell px-4 py-3.5">
                      <span className="text-xs text-dark-200 font-mono bg-dark-800 px-2 py-1 rounded border border-white/5">{plant.productId.substring(0, 8).toUpperCase()}</span>
                    </td>
                    <td className="hidden sm:table-cell px-4 py-3.5">
                      <span
                        className={`inline-flex px-2.5 py-1 rounded-lg text-[10px] font-bold uppercase tracking-wider border ${categoryColors[plant.category] || 'bg-dark-700 text-dark-300'}`}
                      >
                        {plant.category}
                      </span>
                    </td>
                    <td className="px-4 py-3.5 text-right font-mono">
                      <span className="text-sm font-bold text-verdant-300">LKR {plant.price.toLocaleString()}</span>
                    </td>
                    <td className="px-4 py-3.5 text-right">
                      <div className="flex flex-col items-end">
                        <span
                          className={`text-sm font-bold ${plant.stock === 0 ? 'text-rose-500' : plant.stock < 10 ? 'text-amber-400' : 'text-white'}`}
                        >
                          {plant.stock}
                        </span>
                        {plant.stock < 10 && plant.stock > 0 && (
                          <span className="text-[8px] font-bold uppercase text-amber-500 mt-1">Low Stock</span>
                        )}
                      </div>
                    </td>
                    <td className="hidden sm:table-cell px-4 py-3.5">
                      <div className="flex justify-center">
                        <button
                          onClick={() => toggleStock(plant.productId, !!stockToggles[plant.productId])}
                          className={`relative w-10 h-5 rounded-full transition-all duration-300 cursor-pointer ${stockToggles[plant.productId] ? 'bg-verdant-700 glow-green' : 'bg-dark-600'}`}
                        >
                          <div
                            className={`absolute top-1 w-3 h-3 rounded-full bg-white shadow-md transition-all duration-300 ${stockToggles[plant.productId] ? 'left-6' : 'left-1'}`}
                          />
                        </button>
                      </div>
                    </td>
                    <td className="px-6 py-3.5">
                      <div className="flex items-center justify-center gap-1.5 opacity-0 group-hover:opacity-100 transition-opacity">
                        <button
                          onClick={() => {
                            setEditingProduct(plant);
                            setIsModalOpen(true);
                          }}
                          className="w-8 h-8 rounded-lg flex items-center justify-center text-dark-200 hover:text-white hover:bg-dark-700 border border-transparent hover:border-white/10 transition-all cursor-pointer"
                        >
                          <Edit3 className="w-3.5 h-3.5" />
                        </button>
                        <button
                          onClick={() => deleteProduct(plant.productId, plant.name)}
                          className="w-8 h-8 rounded-lg flex items-center justify-center text-dark-200 hover:text-rose-400 hover:bg-rose-400/10 border border-transparent hover:border-rose-400/20 transition-all cursor-pointer"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
      <ProductModal
        isOpen={isModalOpen}
        onClose={() => {
          setIsModalOpen(false);
          setEditingProduct(null);
        }}
        product={editingProduct}
      />
    </>
  );
};

export default InventoryTable;
