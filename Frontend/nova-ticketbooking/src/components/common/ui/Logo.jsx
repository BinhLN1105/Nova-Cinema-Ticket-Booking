import React from 'react';
import { Link } from 'react-router-dom';

export default function Logo() {
  return (
    <Link 
      to="/" 
      onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}
      className="flex items-center gap-2.5 group select-none cursor-pointer"
    >
      {/* 🎬 BIỂU TƯỢNG LOGO SVG ĐỘC QUYỀN - MÀU VÀNG GOLD & GRADIENT ĐỎ CAO CẤP */}
      <svg 
        xmlns="http://www.w3.org/2000/svg" 
        viewBox="0 0 100 100" 
        className="w-9 h-9 transform group-hover:scale-110 transition-transform duration-300 drop-shadow-[0_0_8px_rgba(245,197,24,0.3)]"
      >
        <defs>
          <linearGradient id="logo-grad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="#F5C518" />
            <stop offset="100%" stopColor="#E50914" />
          </linearGradient>
        </defs>
        
        {/* Vòng ngoài cuộn phim điện ảnh cách điệu răng cưa */}
        <circle cx="50" cy="50" r="42" fill="none" stroke="url(#logo-grad)" strokeWidth="5" strokeDasharray="8 4" />
        <circle cx="50" cy="50" r="32" fill="none" stroke="url(#logo-grad)" strokeWidth="1.5" opacity="0.3" />
        
        {/* Chữ N cách điệu ở tâm */}
        <path 
          d="M36 32V68H44V50L56 68H64V32H56V50L44 32H36Z" 
          fill="url(#logo-grad)" 
        />
        
        {/* Ngôi sao lấp lánh (Cinema Star) */}
        <polygon points="50,15 53,22 60,22 55,26 57,33 50,29 43,33 45,26 40,22 47,22" fill="#F5C518" />
      </svg>

      {/* ✍️ PHẦN TEXT THƯƠNG HIỆU */}
      <span className="font-display text-xl font-bold tracking-wider transition-colors duration-200 text-white">
        Nova<span className="text-[#F5C518]">Ticket</span>
      </span>
    </Link>
  );
}
