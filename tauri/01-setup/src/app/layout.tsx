import type { Metadata } from "next";
import "@/styles/globals.scss";

export const metadata: Metadata = {
  title: "handson-tauri-01",
  description: "Tauri トラック 01: 環境構築とプロジェクト作成",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="ja">
      <body>{children}</body>
    </html>
  );
}
