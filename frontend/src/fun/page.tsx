import Link from "next/link";

export default function FunPage() {
  // 임시 데이터 - 실제로는 API 또는 DB에서 가져올 것
  const funArticles = [
    {
      id: 1,
      title: "요즘 대세인 OO 밈 알고 계신가요?",
      excerpt: "최근 SNS에서 가장 핫한 밈을 모아봤습니다. 이 트렌드 놓치지 마세요!",
      date: "2025-03-28",
      readTime: "5분",
      source: "페이스북",
      thumbnail: "/images/placeholder.jpg",
      tags: ["밈", "트렌드", "SNS"],
      likes: 456
    },
    {
      id: 2,
      title: "트위터에서 화제가 된 Z세대 유행어 TOP 10",
      excerpt: "Z세대가 사용하는 신조어와 유행어를 정리했습니다. 당신은 몇 개나 알고 있나요?",
      date: "2025-03-27",
      readTime: "7분",
      source: "트위터",
      thumbnail: "/images/placeholder.jpg",
      tags: ["Z세대", "유행어", "신조어"],
      likes: 782
    },
    {
      id: 3,
      title: "요리 과정이 웃음폭탄! 인스타그램 인기 요리 실패 사례 모음",
      excerpt: "인스타그램에서 화제가 된 요리 실패 사례들을 모아봤습니다. 보고 있으면 웃음이 절로 나와요!",
      date: "2025-03-26",
      readTime: "6분",
      source: "인스타그램",
      thumbnail: "/images/placeholder.jpg",
      tags: ["요리", "인스타그램", "실패담"],
      likes: 350
    },
    {
      id: 4,
      title: "직장인들 공감 100%! 회사에서 겪는 웃픈 상황 모음",
      excerpt: "직장인이라면 누구나 공감할 수 있는 웃기면서도 슬픈 상황들을 모아봤습니다.",
      date: "2025-03-25",
      readTime: "8분",
      source: "레딧",
      thumbnail: "/images/placeholder.jpg",
      tags: ["직장인", "공감", "일상"],
      likes: 912
    },
    {
      id: 5,
      title: "이번 주 화제의 영상: '길고양이와 친구된 강아지' 스토리",
      excerpt: "SNS에서 300만 뷰를 돌파한 감동적인 동물 우정 스토리를 소개합니다.",
      date: "2025-03-24",
      readTime: "4분",
      source: "유튜브",
      thumbnail: "/images/placeholder.jpg",
      tags: ["동물", "감동", "영상"],
      likes: 1245
    },
    {
      id: 6,
      title: "올해의 패션 트렌드, 당신이 놓치고 있는 것은?",
      excerpt: "SNS에서 핫한 2025년 패션 트렌드를 한 눈에 정리해드립니다.",
      date: "2025-03-23",
      readTime: "9분",
      source: "핀터레스트",
      thumbnail: "/images/placeholder.jpg",
      tags: ["패션", "트렌드", "스타일"],
      likes: 670
    },
  ];

  const trendingTags = [
    "밈", "유머", "일상", "영상", "라이프", "패션", "음식", "여행", "게임", "동물"
  ];

  return (
    <div className="space-y-8">
      {/* 페이지 헤더 */}
      <div className="bg-white rounded-xl p-6 shadow-sm">
        <h1 className="text-3xl font-bold mb-2">트렌드/재미</h1>
        <p className="text-gray-600">
          SNS와 커뮤니티에서 화제가 되고 있는 재미있는 콘텐츠
        </p>
        
        {/* 인기 태그 필터 */}
        <div className="mt-4">
          <h3 className="text-sm font-medium text-gray-500 mb-2">인기 태그로 찾기</h3>
          <div className="flex flex-wrap gap-2">
            {trendingTags.map((tag, index) => (
              <Link 
                key={index} 
                href={`/fun?tag=${tag}`}
                className="bg-gray-100 px-3 py-1 rounded-full text-sm hover:bg-accent hover:text-white transition-colors"
              >
                #{tag}
              </Link>
            ))}
          </div>
        </div>
      </div>
      
      {/* 아티클 목록 */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {funArticles.map((article) => (
          <article key={article.id} className="bg-white rounded-xl shadow-sm overflow-hidden hover:shadow-md transition-shadow group">
            <div className="relative h-48 bg-gray-200">
              {/* 실제로는 실제 이미지로 교체 */}
              <div className="absolute inset-0 flex items-center justify-center text-gray-400">
                썸네일 이미지
              </div>
              <div className="absolute top-2 left-2 bg-white text-gray-700 text-xs px-2 py-1 rounded-full">
                출처: {article.source}
              </div>
              <div className="absolute bottom-0 left-0 right-0 bg-gradient-to-t from-black/70 to-transparent p-4">
                <h2 className="font-bold text-lg text-white group-hover:text-accent transition-colors">
                  <Link href={`/fun/${article.id}`}>
                    {article.title}
                  </Link>
                </h2>
              </div>
            </div>
            <div className="p-6">
              <div className="flex flex-wrap gap-1 mb-2">
                {article.tags.map((tag, idx) => (
                  <span key={idx} className="text-xs bg-orange-50 text-orange-700 px-2 py-0.5 rounded">
                    #{tag}
                  </span>
                ))}
              </div>
              <p className="text-gray-600 text-sm mb-4">{article.excerpt}</p>
              <div className="flex justify-between items-center">
                <div className="flex items-center gap-1">
                  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-4 h-4 text-red-500">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M21 8.25c0-2.485-2.099-4.5-4.688-4.5-1.935 0-3.597 1.126-4.312 2.733-.715-1.607-2.377-2.733-4.313-2.733C5.1 3.75 3 5.765 3 8.25c0 7.22 9 12 9 12s9-4.78 9-12Z" />
                  </svg>
                  <span className="text-xs text-gray-500">{article.likes.toLocaleString()}</span>
                </div>
                <div className="flex items-center gap-2">
                  <span className="text-xs text-gray-500">{article.date} · {article.readTime} 소요</span>
                  <Link 
                    href={`/fun/${article.id}`}
                    className="text-accent font-medium text-sm hover:underline"
                  >
                    더 보기 →
                  </Link>
                </div>
              </div>
            </div>
          </article>
        ))}
      </div>
      
      {/* 광고 배너 (예시) */}
      <div className="bg-gradient-to-r from-accent/10 to-primary/10 p-4 rounded-xl text-center">
        <p className="text-gray-500 text-sm">광고 영역</p>
        <div className="h-20 flex items-center justify-center border border-dashed border-gray-300 my-2">
          <span className="text-gray-400">Google AdSense 광고</span>
        </div>
        <p className="text-gray-500 text-xs">트렌드 키워드 기반 광고</p>
      </div>
      
      {/* 페이지네이션 */}
      <div className="flex justify-center space-x-2 mt-8">
        <a href="#" className="px-4 py-2 rounded border text-gray-500 bg-white hover:bg-gray-50">이전</a>
        <a href="#" className="px-4 py-2 rounded border bg-accent text-white">1</a>
        <a href="#" className="px-4 py-2 rounded border text-gray-500 bg-white hover:bg-gray-50">2</a>
        <a href="#" className="px-4 py-2 rounded border text-gray-500 bg-white hover:bg-gray-50">3</a>
        <a href="#" className="px-4 py-2 rounded border text-gray-500 bg-white hover:bg-gray-50">다음</a>
      </div>
    </div>
  );
} 