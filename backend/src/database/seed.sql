-- 초기 데이터 세팅 (시드 데이터)

-- 초기 설정 데이터
INSERT INTO settings (key, value, description) VALUES
('batch_keyword_collection_time', '06:00', '키워드 수집 배치 실행 시간'),
('batch_content_generation_time', '08:00', '콘텐츠 생성 배치 실행 시간'),
('batch_community_crawling_time', '10:00', '커뮤니티 크롤링 배치 실행 시간'),
('batch_posting_time', '14:00', '외부 블로그 포스팅 배치 실행 시간'),
('keywords_per_day', '30', '하루에 수집할 키워드 수'),
('articles_per_day', '10', '하루에 생성할 글 수'),
('openai_model', 'gpt-4-turbo', 'OpenAI GPT 모델'),
('max_cpc_threshold', '2000', '최대 CPC 임계값 (원)'),
('min_cpc_threshold', '500', '최소 CPC 임계값 (원)');

-- 샘플 키워드 데이터
INSERT INTO keywords (keyword, cpc, search_volume, status, category) VALUES
('자동차보험', 1250, 5400, 'active', 'finance'),
('주식투자', 950, 8600, 'active', 'finance'),
('암호화폐', 1100, 4200, 'active', 'finance'),
('건강보조제', 800, 3700, 'pending', 'health'),
('IT트렌드', 650, 2800, 'active', 'tech'),
('부동산투자', 1400, 4900, 'pending', 'finance'),
('이더리움', 980, 3200, 'active', 'crypto'),
('헬스케어', 780, 5600, 'active', 'health'),
('자기계발', 550, 4800, 'active', 'lifestyle'),
('취업준비', 720, 7200, 'active', 'career'),
('다이어트', 890, 8900, 'active', 'health'),
('여행준비', 480, 3500, 'active', 'travel');

-- 샘플 태그 데이터
INSERT INTO tags (name, type) VALUES
('금융', 'info'),
('투자', 'info'),
('건강', 'info'),
('기술', 'info'),
('부동산', 'info'),
('자기계발', 'info'),
('취업', 'info'),
('밈', 'fun'),
('유머', 'fun'),
('일상', 'fun'),
('영상', 'fun'),
('패션', 'fun');

-- 샘플 콘텐츠 데이터 (정보)
INSERT INTO contents (title, slug, excerpt, content, type, status, author, view_count, published_at) VALUES
('최신 리액트 18 기능 완벽 정리', 'latest-react-18-features', '리액트 18에서 새롭게 추가된 기능과 성능 개선점을 자세히 알아봅니다.', 
 '<h2>리액트 18의 주요 변경사항</h2><p>리액트 18은 동시성 렌더링을 지원하는 새로운 기능과 성능 개선을 포함하고 있습니다.</p><h3>1. 동시성 렌더링</h3><p>리액트 18의 가장 큰 변화는 동시성 렌더링 도입입니다. 이를 통해 UI 업데이트의 우선순위를 지정할 수 있어 사용자 경험이 크게 향상됩니다.</p>', 
 'info', 'published', 'AI Writer', 342, '2025-03-28'),
('주식 초보자가 꼭 알아야 할 5가지 투자 원칙', 'stock-investment-principles-for-beginners', '성공적인 주식 투자를 위한 필수 원칙과 팁을 소개합니다.', 
 '<h2>주식 투자의 기본 원칙</h2><p>주식 투자를 시작하기 전에 알아야 할 기본적인 원칙들이 있습니다.</p><h3>1. 장기 투자의 중요성</h3><p>단기간의 시장 변동에 흔들리지 않고 장기적인 관점에서 투자하는 것이 중요합니다.</p>', 
 'info', 'published', 'AI Writer', 456, '2025-03-26'),
('자동차 보험 가입 시 꼭 체크해야 할 5가지 포인트', 'car-insurance-checklist', '자동차 보험 가입 시 놓치기 쉬운 중요한 체크포인트를 상세히 설명합니다.', 
 '<h2>자동차 보험 가입 시 체크포인트</h2><p>자동차 보험은 종류와 특약이 다양하기 때문에 가입 전 꼼꼼하게 확인해야 합니다.</p><h3>1. 보장 범위 확인</h3><p>자신에게 필요한 보장 범위를 파악하고 그에 맞는 상품을 선택하는 것이 중요합니다.</p>', 
 'info', 'draft', 'AI Writer', 0, NULL);

-- 샘플 콘텐츠 데이터 (트렌드/재미)
INSERT INTO contents (title, slug, excerpt, content, type, status, author, view_count, published_at) VALUES
('요즘 대세인 OO 밈 알고 계신가요?', 'trending-memes', '최근 SNS에서 가장 핫한 밈을 모아봤습니다. 이 트렌드 놓치지 마세요!', 
 '<h2>최근 인기 밈 모음</h2><p>SNS에서 화제가 되고 있는 최신 밈들을 소개합니다.</p><h3>1. "아니 이게 뭐야" 밈</h3><p>일상 속 예상치 못한 상황에서 사용되는 이 밈은 특히 Z세대 사이에서 큰 인기를 끌고 있습니다.</p>', 
 'fun', 'published', 'AI Writer', 789, '2025-03-27'),
('트위터에서 화제가 된 Z세대 유행어 TOP 10', 'z-generation-slang', 'Z세대가 사용하는 신조어와 유행어를 정리했습니다. 당신은 몇 개나 알고 있나요?', 
 '<h2>Z세대 유행어 모음</h2><p>트위터와 인스타그램에서 자주 볼 수 있는 Z세대 유행어들을 소개합니다.</p><h3>1. "ㅈㄱㄴ"</h3><p>"진짜 괜찮네"의 줄임말로, 놀라움이나 감탄을 표현할 때 사용합니다.</p>', 
 'fun', 'published', 'AI Writer', 356, '2025-03-27');

-- 샘플 콘텐츠-키워드 연결
INSERT INTO content_keywords (content_id, keyword_id, is_primary) VALUES
(1, 5, true),  -- 리액트 18 - IT트렌드
(2, 2, true),  -- 주식 투자 - 주식투자
(3, 1, true);  -- 자동차 보험 - 자동차보험

-- 샘플 콘텐츠-태그 연결
INSERT INTO content_tags (content_id, tag_id) VALUES
(1, 4),  -- 리액트 18 - 기술
(2, 1),  -- 주식 투자 - 금융
(2, 2),  -- 주식 투자 - 투자
(3, 1),  -- 자동차 보험 - 금융
(4, 8),  -- 밈 - 밈
(5, 8);  -- Z세대 유행어 - 밈

-- 샘플 콘텐츠 소스
INSERT INTO content_sources (content_id, source_name, source_url) VALUES
(4, 'facebook', 'https://www.facebook.com/example/post123'),
(5, 'twitter', 'https://twitter.com/example/status/123456789');

-- 샘플 외부 게시 기록
INSERT INTO publish_logs (content_id, platform, status, external_url) VALUES
(1, 'naver_blog', 'success', 'https://blog.naver.com/jsportal/123456789'),
(2, 'naver_blog', 'success', 'https://blog.naver.com/jsportal/123456790'),
(4, 'facebook_page', 'success', 'https://www.facebook.com/jsportal/posts/123456');

-- 샘플 수익 통계
INSERT INTO revenue_stats (date, content_id, page_views, ad_clicks, revenue, ctr, cpc) VALUES
('2025-03-28', 1, 342, 8, 3920.00, 2.34, 490.00),
('2025-03-28', 2, 156, 4, 3400.00, 2.56, 850.00),
('2025-03-28', 4, 289, 6, 2530.00, 2.08, 421.67),
('2025-03-27', 1, 250, 5, 2450.00, 2.00, 490.00),
('2025-03-27', 2, 120, 3, 2550.00, 2.50, 850.00),
('2025-03-27', 4, 210, 4, 1670.00, 1.90, 417.50),
('2025-03-27', 5, 180, 3, 1320.00, 1.67, 440.00);

-- 샘플 배치 로그
INSERT INTO batch_logs (process_name, status, start_time, end_time, total_items, processed_items, success_items, failed_items) VALUES
('keyword_collection', 'success', '2025-03-28 06:00:00', '2025-03-28 06:05:32', 50, 50, 45, 5),
('content_generation', 'success', '2025-03-28 08:00:00', '2025-03-28 08:15:45', 10, 10, 9, 1),
('community_crawling', 'success', '2025-03-28 10:00:00', '2025-03-28 10:08:23', 20, 20, 18, 2); 