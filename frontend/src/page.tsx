import Image from "next/image";
import Link from "next/link";

export default function Home() {
  // 임시 데이터 - 실제로는 API 또는 DB에서 가져올 것
  const featuredPosts = [
    {
      id: 1,
      title: "최신 리액트 18 기능 완벽 정리",
      excerpt: "리액트 18에서 새롭게 추가된 기능과 성능 개선점을 자세히 알아봅니다.",
      category: "info",
      date: "2025-03-28",
      readTime: "8분",
      thumbnail: "/images/placeholder.jpg",
    },
    {
      id: 2,
      title: "요즘 대세인 OO 밈 알고 계신가요?",
      excerpt: "최근 SNS에서 가장 핫한 밈을 모아봤습니다. 이 트렌드 놓치지 마세요!",
      category: "fun",
      date: "2025-03-27",
      readTime: "5분",
      thumbnail: "/images/placeholder.jpg",
    },
    {
      id: 3,
      title: "주식 초보자가 꼭 알아야 할 5가지 투자 원칙",
      excerpt: "성공적인 주식 투자를 위한 필수 원칙과 팁을 소개합니다.",
      category: "info",
      date: "2025-03-26",
      readTime: "10분",
      thumbnail: "/images/placeholder.jpg",
    },
  ];

  const trendingKeywords = [
    "인공지능", "NFT", "주식투자", "환율전망", "헬스케어", 
    "이더리움", "메타버스", "부동산", "자동차보험", "건강보조제"
  ];

  return (
    <div className="space-y-12">
      {/* 히어로 섹션 */}
      <section className="bg-gradient-to-r from-primary to-blue-700 text-white rounded-2xl p-10 text-center">
        <h1 className="text-3xl md:text-5xl font-bold mb-4">
          최신 트렌드와 유용한 정보를 한곳에서
        </h1>
        <p className="text-xl opacity-90 max-w-2xl mx-auto mb-8">
          매일 업데이트되는 고품질 콘텐츠로 트렌드를 놓치지 마세요
        </p>
        <div className="flex flex-col sm:flex-row gap-4 justify-center">
          <Link 
            href="/info" 
            className="bg-white text-primary font-medium px-6 py-3 rounded-full transition-transform hover:scale-105"
          >
            정보글 보기
          </Link>
          <Link 
            href="/fun" 
            className="bg-transparent text-white border border-white font-medium px-6 py-3 rounded-full transition-transform hover:scale-105"
          >
            트렌드/재미 보기
          </Link>
        </div>
      </section>

      {/* 인기 키워드 */}
      <section>
        <div className="flex items-center gap-2 mb-4">
          <h2 className="text-2xl font-bold">인기 키워드</h2>
          <span className="bg-red-500 text-white text-xs px-2 py-1 rounded-full">HOT</span>
        </div>
        <div className="flex flex-wrap gap-2">
          {trendingKeywords.map((keyword, index) => (
            <Link 
              key={index} 
              href={`/info?keyword=${keyword}`}
              className="bg-white border border-gray-200 hover:border-primary px-4 py-2 rounded-full text-sm transition-colors"
            >
              #{keyword}
            </Link>
          ))}
        </div>
      </section>

      {/* 추천 콘텐츠 */}
      <section>
        <h2 className="text-2xl font-bold mb-6">추천 콘텐츠</h2>
        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
          {featuredPosts.map((post) => (
            <div key={post.id} className="bg-white rounded-xl shadow-sm overflow-hidden hover:shadow-md transition-shadow">
              <div className="relative h-48 bg-gray-200">
                {/* 실제로는 실제 이미지로 교체 */}
                <div className="absolute inset-0 flex items-center justify-center text-gray-400">
                  썸네일 이미지
                </div>
              </div>
              <div className="p-6">
                <div className="flex justify-between items-center mb-2">
                  <span className={`text-xs font-medium px-2 py-1 rounded-full ${
                    post.category === 'info' 
                      ? 'bg-blue-100 text-blue-700' 
                      : 'bg-orange-100 text-orange-700'
                  }`}>
                    {post.category === 'info' ? '정보' : '트렌드/재미'}
                  </span>
                  <span className="text-xs text-gray-500">{post.date} · {post.readTime} 소요</span>
                </div>
                <h3 className="font-bold text-xl mb-2 hover:text-primary transition-colors">
                  <Link href={`/${post.category}/${post.id}`}>
                    {post.title}
                  </Link>
                </h3>
                <p className="text-gray-600 text-sm mb-4">{post.excerpt}</p>
                <Link 
                  href={`/${post.category}/${post.id}`}
                  className="text-primary font-medium text-sm hover:underline"
                >
                  더 읽기 →
                </Link>
              </div>
            </div>
          ))}
        </div>
        <div className="text-center mt-8">
          <Link 
            href="/info" 
            className="inline-block bg-primary text-white font-medium px-6 py-3 rounded-full transition-transform hover:scale-105"
          >
            더 많은 콘텐츠 보기
          </Link>
        </div>
      </section>

      {/* 구독 섹션 */}
      <section className="bg-light rounded-2xl p-8 text-center">
        <h2 className="text-2xl font-bold mb-2">최신 콘텐츠 소식 받기</h2>
        <p className="text-gray-600 mb-6">
          최신 트렌드와 인기 콘텐츠를 이메일로 받아보세요
        </p>
        <form className="flex flex-col sm:flex-row gap-2 max-w-lg mx-auto">
          <input 
            type="email" 
            placeholder="이메일 주소"
            className="flex-1 px-4 py-3 rounded-full border border-gray-200 focus:outline-none focus:ring-2 focus:ring-primary"
          />
          <button 
            type="submit"
            className="bg-primary text-white font-medium px-6 py-3 rounded-full transition-transform hover:scale-105"
          >
            구독하기
          </button>
        </form>
      </section>
    </div>
  );
}
