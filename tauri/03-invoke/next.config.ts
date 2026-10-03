import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: "export",
  // 複数ページ（/about）を静的エクスポートすると、既定では out/about.html になる。
  // Tauri の frontendDist はディレクトリの index.html を期待するため、
  // trailingSlash で out/about/index.html の形にする。
  trailingSlash: true,
  images: {
    unoptimized: true,
  },
};

export default nextConfig;
