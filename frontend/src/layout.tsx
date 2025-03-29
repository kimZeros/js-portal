import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "JS Portal - 최신 트렌드 & 정보",
  description: "매일 업데이트되는 최신 트렌드와 실용적인 정보를 제공합니다",
  keywords: "트렌드, 정보, 커뮤니티, 이슈, 자동 콘텐츠",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko">
      <body
        className={`${geistSans.variable} ${geistMono.variable} antialiased bg-gray-50`}
      >
        <header className="sticky top-0 bg-white shadow-sm z-50">
          <nav className="container mx-auto px-4 py-4 flex justify-between items-center">
            <div className="font-bold text-2xl text-primary">JS Portal</div>
            <div className="flex gap-6">
              <a href="/" className="hover:text-primary transition-colors">홈</a>
              <a href="/info" className="hover:text-primary transition-colors">정보글</a>
              <a href="/fun" className="hover:text-primary transition-colors">트렌드/재미</a>
            </div>
          </nav>
        </header>
        
        <main className="container mx-auto px-4 py-8">
          {children}
        </main>
        
        <footer className="bg-dark text-white py-8">
          <div className="container mx-auto px-4">
            <div className="flex flex-col md:flex-row justify-between">
              <div className="mb-6 md:mb-0">
                <h3 className="font-bold text-xl mb-2">JS Portal</h3>
                <p className="text-gray-300">매일 업데이트되는 최신 트렌드와 정보</p>
              </div>
              <div>
                <h4 className="font-semibold mb-2">카테고리</h4>
                <ul className="text-gray-300">
                  <li><a href="/info" className="hover:text-primary">정보글</a></li>
                  <li><a href="/fun" className="hover:text-primary">트렌드/재미</a></li>
                </ul>
              </div>
            </div>
            <div className="mt-8 pt-6 border-t border-gray-700 text-center text-gray-400">
              &copy; {new Date().getFullYear()} JS Portal - 모든 권리 보유
            </div>
          </div>
        </footer>
      </body>
    </html>
  );
}
