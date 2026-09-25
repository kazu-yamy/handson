import type { NextConfig } from "next";
import path from "node:path";

const nextConfig: NextConfig = {
  sassOptions: {
    loadPaths: [path.join(process.cwd(), "src/styles")],
    additionalData: '@use "variables" as *; @use "mixins" as *;',
  },
};

export default nextConfig;
