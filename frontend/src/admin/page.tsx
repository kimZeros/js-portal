import Link from "next/link";

export default function AdminPage() {
  // 임시 데이터 - 실제로는 API 또는 DB에서 가져올 것
  const dailyStats = {
    pageViews: 12845,
    adClicks: 256,
    ctr: "1.99%",
    revenue: 125680,
    articles: 18,
    trending: ["자동차보험", "주식투자", "암호화폐", "건강보조제", "IT트렌드"]
  };

  const recentArticles = [
    {
      id: 101,
      title: "최신 리액트 18 기능 완벽 정리",
      category: "info",
      status: "published",
      views: 342,
      revenue: 7850,
      date: "2025-03-28"
    },
    {
      id: 102,
      title: "요즘 대세인 OO 밈 알고 계신가요?",
      category: "fun",
      status: "published",
      views: 789,
      revenue: 5420,
      date: "2025-03-27"
    },
    {
      id: 103,
      title: "주식 초보자가 꼭 알아야 할 5가지 투자 원칙",
      category: "info",
      status: "published",
      views: 456,
      revenue: 9760,
      date: "2025-03-26"
    },
    {
      id: 104,
      title: "자동차 보험 가입 시 꼭 체크해야 할 5가지 포인트",
      category: "info",
      status: "draft",
      views: 0,
      revenue: 0,
      date: "2025-03-28"
    },
    {
      id: 105,
      title: "트위터에서 화제가 된 Z세대 유행어 TOP 10",
      category: "fun",
      status: "published",
      views: 356,
      revenue: 4250,
      date: "2025-03-27"
    }
  ];

  const keywordsList = [
    { keyword: "자동차보험", cpc: 1250, searchVolume: 5400, status: "active" },
    { keyword: "주식투자", cpc: 950, searchVolume: 8600, status: "active" },
    { keyword: "암호화폐", cpc: 1100, searchVolume: 4200, status: "active" },
    { keyword: "건강보조제", cpc: 800, searchVolume: 3700, status: "pending" },
    { keyword: "IT트렌드", cpc: 650, searchVolume: 2800, status: "active" },
    { keyword: "부동산투자", cpc: 1400, searchVolume: 4900, status: "pending" },
    { keyword: "이더리움", cpc: 980, searchVolume: 3200, status: "active" },
    { keyword: "헬스케어", cpc: 780, searchVolume: 5600, status: "active" }
  ];

  return (
    <div className="space-y-8">
      <h1 className="text-3xl font-bold mb-8">관리자 대시보드</h1>
      
      {/* 상단 통계 카드 */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <div className="bg-white p-6 rounded-xl shadow-sm">
          <h3 className="text-sm text-gray-500 mb-1">오늘 방문자</h3>
          <p className="text-3xl font-bold text-primary">{dailyStats.pageViews.toLocaleString()}</p>
          <p className="text-xs text-gray-500 mt-2">어제 대비 +12.4%</p>
        </div>
        
        <div className="bg-white p-6 rounded-xl shadow-sm">
          <h3 className="text-sm text-gray-500 mb-1">광고 클릭</h3>
          <p className="text-3xl font-bold text-primary">{dailyStats.adClicks.toLocaleString()}</p>
          <p className="text-xs text-gray-500 mt-2">CTR: {dailyStats.ctr}</p>
        </div>
        
        <div className="bg-white p-6 rounded-xl shadow-sm">
          <h3 className="text-sm text-gray-500 mb-1">오늘 수익</h3>
          <p className="text-3xl font-bold text-primary">₩{dailyStats.revenue.toLocaleString()}</p>
          <p className="text-xs text-gray-500 mt-2">CPC 평균: ₩491</p>
        </div>
        
        <div className="bg-white p-6 rounded-xl shadow-sm">
          <h3 className="text-sm text-gray-500 mb-1">게시된 글</h3>
          <p className="text-3xl font-bold text-primary">{dailyStats.articles}</p>
          <p className="text-xs text-gray-500 mt-2">오늘 새 글 +5</p>
        </div>
      </div>
      
      {/* 차트 표시 영역 - 실제로는 Recharts 등 라이브러리 사용 */}
      <div className="bg-white p-6 rounded-xl shadow-sm">
        <h2 className="text-xl font-semibold mb-4">수익 추이</h2>
        <div className="h-64 bg-gray-50 flex items-center justify-center">
          <p className="text-gray-400">이곳에 Recharts 등으로 구현한 차트가 표시됩니다</p>
        </div>
      </div>
      
      {/* 인기 키워드 */}
      <div className="bg-white p-6 rounded-xl shadow-sm">
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-xl font-semibold">인기 키워드</h2>
          <Link href="/admin/keywords" className="text-primary text-sm hover:underline">
            모든 키워드 보기
          </Link>
        </div>
        <div className="overflow-x-auto">
          <table className="min-w-full">
            <thead>
              <tr className="border-b">
                <th className="py-3 px-4 text-left text-xs font-medium text-gray-500">키워드</th>
                <th className="py-3 px-4 text-left text-xs font-medium text-gray-500">CPC (원)</th>
                <th className="py-3 px-4 text-left text-xs font-medium text-gray-500">검색량</th>
                <th className="py-3 px-4 text-left text-xs font-medium text-gray-500">상태</th>
                <th className="py-3 px-4 text-left text-xs font-medium text-gray-500">작업</th>
              </tr>
            </thead>
            <tbody>
              {keywordsList.map((keyword, index) => (
                <tr key={index} className="border-b hover:bg-gray-50">
                  <td className="py-3 px-4">{keyword.keyword}</td>
                  <td className="py-3 px-4 font-medium text-primary">{keyword.cpc.toLocaleString()}</td>
                  <td className="py-3 px-4">{keyword.searchVolume.toLocaleString()}</td>
                  <td className="py-3 px-4">
                    <span className={`px-2 py-1 rounded-full text-xs ${
                      keyword.status === 'active' 
                        ? 'bg-green-100 text-green-700' 
                        : 'bg-yellow-100 text-yellow-700'
                    }`}>
                      {keyword.status === 'active' ? '사용 중' : '대기 중'}
                    </span>
                  </td>
                  <td className="py-3 px-4">
                    <button className="text-primary text-sm hover:underline">작성하기</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
      
      {/* 최근 게시글 */}
      <div className="bg-white p-6 rounded-xl shadow-sm">
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-xl font-semibold">최근 게시글</h2>
          <Link href="/admin/articles" className="text-primary text-sm hover:underline">
            모든 글 보기
          </Link>
        </div>
        <div className="overflow-x-auto">
          <table className="min-w-full">
            <thead>
              <tr className="border-b">
                <th className="py-3 px-4 text-left text-xs font-medium text-gray-500">제목</th>
                <th className="py-3 px-4 text-left text-xs font-medium text-gray-500">카테고리</th>
                <th className="py-3 px-4 text-left text-xs font-medium text-gray-500">상태</th>
                <th className="py-3 px-4 text-left text-xs font-medium text-gray-500">조회수</th>
                <th className="py-3 px-4 text-left text-xs font-medium text-gray-500">수익 (원)</th>
                <th className="py-3 px-4 text-left text-xs font-medium text-gray-500">날짜</th>
              </tr>
            </thead>
            <tbody>
              {recentArticles.map((article) => (
                <tr key={article.id} className="border-b hover:bg-gray-50">
                  <td className="py-3 px-4 font-medium">
                    <Link href={`/admin/articles/${article.id}`} className="hover:text-primary">
                      {article.title}
                    </Link>
                  </td>
                  <td className="py-3 px-4">
                    <span className={`px-2 py-1 rounded-full text-xs ${
                      article.category === 'info' 
                        ? 'bg-blue-100 text-blue-700' 
                        : 'bg-orange-100 text-orange-700'
                    }`}>
                      {article.category === 'info' ? '정보' : '트렌드/재미'}
                    </span>
                  </td>
                  <td className="py-3 px-4">
                    <span className={`px-2 py-1 rounded-full text-xs ${
                      article.status === 'published' 
                        ? 'bg-green-100 text-green-700' 
                        : 'bg-gray-100 text-gray-700'
                    }`}>
                      {article.status === 'published' ? '게시됨' : '초안'}
                    </span>
                  </td>
                  <td className="py-3 px-4">{article.views.toLocaleString()}</td>
                  <td className="py-3 px-4 font-medium">{article.revenue.toLocaleString()}</td>
                  <td className="py-3 px-4 text-gray-500">{article.date}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
      
      {/* 배치 프로세스 상태 */}
      <div className="bg-white p-6 rounded-xl shadow-sm">
        <h2 className="text-xl font-semibold mb-4">자동화 프로세스 상태</h2>
        <div className="flex flex-col md:flex-row gap-6">
          <div className="flex-1 p-4 border rounded-lg bg-gray-50">
            <div className="flex justify-between mb-2">
              <h3 className="font-medium">키워드 수집 배치</h3>
              <span className="px-2 py-1 rounded-full text-xs bg-green-100 text-green-700">정상</span>
            </div>
            <p className="text-sm text-gray-600 mb-2">마지막 실행: 오늘 06:00</p>
            <p className="text-sm text-gray-600">수집된 키워드: 32개</p>
          </div>
          
          <div className="flex-1 p-4 border rounded-lg bg-gray-50">
            <div className="flex justify-between mb-2">
              <h3 className="font-medium">콘텐츠 생성 배치</h3>
              <span className="px-2 py-1 rounded-full text-xs bg-green-100 text-green-700">정상</span>
            </div>
            <p className="text-sm text-gray-600 mb-2">마지막 실행: 오늘 08:15</p>
            <p className="text-sm text-gray-600">생성된 글: 18개 (성공률: 95%)</p>
          </div>
          
          <div className="flex-1 p-4 border rounded-lg bg-gray-50">
            <div className="flex justify-between mb-2">
              <h3 className="font-medium">커뮤니티 크롤링</h3>
              <span className="px-2 py-1 rounded-full text-xs bg-yellow-100 text-yellow-700">주의</span>
            </div>
            <p className="text-sm text-gray-600 mb-2">마지막 실행: 오늘 10:30</p>
            <p className="text-sm text-gray-600">수집된 콘텐츠: 12개 (오류: 2개)</p>
          </div>
        </div>
      </div>
    </div>
  );
} 