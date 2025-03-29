import Link from "next/link";

export default function InfoPage() {
  // 임시 데이터 - 실제로는 API 또는 DB에서 가져올 것
  const infoArticles = [
    {
      id: 1,
      title: "최신 리액트 18 기능 완벽 정리",
      excerpt: "리액트 18에서 새롭게 추가된 기능과 성능 개선점을 자세히 알아봅니다.",
      date: "2025-03-28",
      readTime: "8분",
      thumbnail: "/images/placeholder.jpg",
      keywords: ["리액트", "프론트엔드", "개발"],
      views: 1245
    },
    {
      id: 2,
      title: "주식 초보자가 꼭 알아야 할 5가지 투자 원칙",
      excerpt: "성공적인 주식 투자를 위한 필수 원칙과 팁을 소개합니다.",
      date: "2025-03-26",
      readTime: "10분",
      thumbnail: "/images/placeholder.jpg",
      keywords: ["주식투자", "재테크", "금융"],
      views: 2340
    },
    {
      id: 3,
      title: "자동차 보험 가입 시 꼭 체크해야 할 5가지 포인트",
      excerpt: "자동차 보험 가입 시 놓치기 쉬운 중요한 체크포인트를 상세히 설명합니다.",
      date: "2025-03-25",
      readTime: "7분",
      thumbnail: "/images/placeholder.jpg",
      keywords: ["자동차보험", "보험비교", "금융"],
      views: 1876
    },
    {
      id: 4,
      title: "2025년 가장 주목해야 할 IT 트렌드 10가지",
      excerpt: "올해 IT 업계에서 가장 주목받는 기술 트렌드와 그 영향력에 대해 알아봅니다.",
      date: "2025-03-24",
      readTime: "12분",
      thumbnail: "/images/placeholder.jpg",
      keywords: ["IT", "기술트렌드", "인공지능"],
      views: 3150
    },
    {
      id: 5,
      title: "부동산 투자, 지금이 적기일까? 전문가 3인의 조언",
      excerpt: "부동산 시장 전망과 투자 전략에 대한 전문가들의 의견을 정리했습니다.",
      date: "2025-03-23",
      readTime: "9분",
      thumbnail: "/images/placeholder.jpg",
      keywords: ["부동산", "투자", "재테크"],
      views: 2980
    },
    {
      id: 6,
      title: "건강 검진 결과 제대로 이해하는 방법",
      excerpt: "건강 검진 결과표를 올바르게 해석하고 후속 조치를 취하는 방법을 알려드립니다.",
      date: "2025-03-22",
      readTime: "8분",
      thumbnail: "/images/placeholder.jpg",
      keywords: ["건강", "의료", "건강검진"],
      views: 1650
    },
  ];

  const popularKeywords = [
    "주식투자", "부동산", "건강", "보험", "IT", "금융", "자기계발", "취업", "이직", "암호화폐"
  ];

  return (
    <div className="space-y-8">
      {/* 페이지 헤더 */}
      <div className="bg-white rounded-xl p-6 shadow-sm">
        <h1 className="text-3xl font-bold mb-2">정보글</h1>
        <p className="text-gray-600">
          최신 트렌드와 유용한 정보를 제공하는 고품질 콘텐츠
        </p>
        
        {/* 인기 키워드 필터 */}
        <div className="mt-4">
          <h3 className="text-sm font-medium text-gray-500 mb-2">인기 키워드로 찾기</h3>
          <div className="flex flex-wrap gap-2">
            {popularKeywords.map((keyword, index) => (
              <Link 
                key={index} 
                href={`/info?keyword=${keyword}`}
                className="bg-gray-100 px-3 py-1 rounded-full text-sm hover:bg-primary hover:text-white transition-colors"
              >
                {keyword}
              </Link>
            ))}
          </div>
        </div>
      </div>
      
      {/* 아티클 목록 */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {infoArticles.map((article) => (
          <article key={article.id} className="bg-white rounded-xl shadow-sm overflow-hidden hover:shadow-md transition-shadow">
            <div className="relative h-48 bg-gray-200">
              {/* 실제로는 실제 이미지로 교체 */}
              <div className="absolute inset-0 flex items-center justify-center text-gray-400">
                썸네일 이미지
              </div>
              <div className="absolute top-2 right-2 bg-white text-gray-600 text-xs px-2 py-1 rounded-full">
                조회수 {article.views.toLocaleString()}
              </div>
            </div>
            <div className="p-6">
              <div className="flex flex-wrap gap-1 mb-2">
                {article.keywords.map((keyword, idx) => (
                  <span key={idx} className="text-xs bg-blue-50 text-blue-700 px-2 py-0.5 rounded">
                    {keyword}
                  </span>
                ))}
              </div>
              <h2 className="font-bold text-xl mb-2 hover:text-primary transition-colors">
                <Link href={`/info/${article.id}`}>
                  {article.title}
                </Link>
              </h2>
              <p className="text-gray-600 text-sm mb-4">{article.excerpt}</p>
              <div className="flex justify-between items-center">
                <span className="text-xs text-gray-500">{article.date} · {article.readTime} 소요</span>
                <Link 
                  href={`/info/${article.id}`}
                  className="text-primary font-medium text-sm hover:underline"
                >
                  더 읽기 →
                </Link>
              </div>
            </div>
          </article>
        ))}
      </div>
      
      {/* 페이지네이션 */}
      <div className="flex justify-center space-x-2 mt-8">
        <a href="#" className="px-4 py-2 rounded border text-gray-500 bg-white hover:bg-gray-50">이전</a>
        <a href="#" className="px-4 py-2 rounded border bg-primary text-white">1</a>
        <a href="#" className="px-4 py-2 rounded border text-gray-500 bg-white hover:bg-gray-50">2</a>
        <a href="#" className="px-4 py-2 rounded border text-gray-500 bg-white hover:bg-gray-50">3</a>
        <a href="#" className="px-4 py-2 rounded border text-gray-500 bg-white hover:bg-gray-50">다음</a>
      </div>
      
      {/* 광고 배너 (예시) */}
      <div className="bg-gray-100 p-4 rounded-xl text-center">
        <p className="text-gray-500 text-sm">광고 영역</p>
        <div className="h-20 flex items-center justify-center border border-dashed border-gray-300 my-2">
          <span className="text-gray-400">Google AdSense 광고</span>
        </div>
        <p className="text-gray-500 text-xs">고수익 키워드 기반 광고</p>
      </div>
    </div>
  );
} 