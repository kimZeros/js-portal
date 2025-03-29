-- PostgreSQL Database Schema for JS Portal

-- 키워드 테이블
CREATE TABLE keywords (
  id SERIAL PRIMARY KEY,
  keyword VARCHAR(100) NOT NULL,
  cpc INTEGER NOT NULL,
  search_volume INTEGER NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'pending', -- pending, active, used, rejected
  category VARCHAR(50), -- info, finance, health, tech, etc.
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  last_used_at TIMESTAMP,
  CONSTRAINT uk_keyword UNIQUE (keyword)
);

-- 콘텐츠 테이블 (정보글 및 트렌드/재미 콘텐츠 모두 포함)
CREATE TABLE contents (
  id SERIAL PRIMARY KEY,
  title VARCHAR(200) NOT NULL,
  slug VARCHAR(255) NOT NULL,
  excerpt TEXT NOT NULL,
  content TEXT NOT NULL,
  type VARCHAR(20) NOT NULL, -- info, fun
  status VARCHAR(20) NOT NULL DEFAULT 'draft', -- draft, published, archived
  thumbnail_url VARCHAR(255),
  author VARCHAR(100) DEFAULT 'AI Writer',
  view_count INTEGER DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  published_at TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_slug UNIQUE (slug)
);

-- 키워드와 콘텐츠 연결 테이블
CREATE TABLE content_keywords (
  id SERIAL PRIMARY KEY,
  content_id INTEGER NOT NULL,
  keyword_id INTEGER NOT NULL,
  is_primary BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_content_id FOREIGN KEY (content_id) REFERENCES contents(id) ON DELETE CASCADE,
  CONSTRAINT fk_keyword_id FOREIGN KEY (keyword_id) REFERENCES keywords(id) ON DELETE CASCADE,
  CONSTRAINT uk_content_keyword UNIQUE (content_id, keyword_id)
);

-- 태그 테이블
CREATE TABLE tags (
  id SERIAL PRIMARY KEY,
  name VARCHAR(50) NOT NULL,
  type VARCHAR(20) NOT NULL, -- info, fun
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_tag_name UNIQUE (name)
);

-- 콘텐츠와 태그 연결 테이블
CREATE TABLE content_tags (
  id SERIAL PRIMARY KEY,
  content_id INTEGER NOT NULL,
  tag_id INTEGER NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_content_id FOREIGN KEY (content_id) REFERENCES contents(id) ON DELETE CASCADE,
  CONSTRAINT fk_tag_id FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE,
  CONSTRAINT uk_content_tag UNIQUE (content_id, tag_id)
);

-- 콘텐츠 소스 (크롤링 출처) 테이블
CREATE TABLE content_sources (
  id SERIAL PRIMARY KEY,
  content_id INTEGER NOT NULL,
  source_name VARCHAR(100) NOT NULL, -- facebook, twitter, reddit, community
  source_url TEXT, -- 원본 URL
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_content_id FOREIGN KEY (content_id) REFERENCES contents(id) ON DELETE CASCADE
);

-- 게시 기록 테이블 (네이버, 페이스북 등 외부 게시 현황)
CREATE TABLE publish_logs (
  id SERIAL PRIMARY KEY,
  content_id INTEGER NOT NULL,
  platform VARCHAR(50) NOT NULL, -- naver_blog, facebook_page, etc.
  status VARCHAR(20) NOT NULL, -- success, failed, pending
  external_url TEXT, -- 게시된 외부 URL
  error_message TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_content_id FOREIGN KEY (content_id) REFERENCES contents(id) ON DELETE CASCADE
);

-- 수익 통계 테이블
CREATE TABLE revenue_stats (
  id SERIAL PRIMARY KEY,
  date DATE NOT NULL,
  content_id INTEGER,
  page_views INTEGER DEFAULT 0,
  ad_clicks INTEGER DEFAULT 0,
  revenue DECIMAL(10,2) DEFAULT 0,
  ctr DECIMAL(5,2) DEFAULT 0, -- Click-through rate
  cpc DECIMAL(7,2) DEFAULT 0, -- Cost per click
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_content_id FOREIGN KEY (content_id) REFERENCES contents(id) ON DELETE SET NULL
);

-- 자동화 프로세스 로그 테이블
CREATE TABLE batch_logs (
  id SERIAL PRIMARY KEY,
  process_name VARCHAR(100) NOT NULL, -- keyword_collection, content_generation, community_crawling
  status VARCHAR(20) NOT NULL, -- success, failed, running
  start_time TIMESTAMP NOT NULL,
  end_time TIMESTAMP,
  total_items INTEGER DEFAULT 0,
  processed_items INTEGER DEFAULT 0,
  success_items INTEGER DEFAULT 0,
  failed_items INTEGER DEFAULT 0,
  error_message TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 설정 테이블
CREATE TABLE settings (
  id SERIAL PRIMARY KEY,
  key VARCHAR(100) NOT NULL,
  value TEXT NOT NULL,
  description TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_setting_key UNIQUE (key)
);

-- 인덱스 생성
CREATE INDEX idx_content_type ON contents(type);
CREATE INDEX idx_content_status ON contents(status);
CREATE INDEX idx_keyword_status ON keywords(status);
CREATE INDEX idx_keyword_category ON keywords(category);
CREATE INDEX idx_revenue_date ON revenue_stats(date);
CREATE INDEX idx_revenue_content ON revenue_stats(content_id);
CREATE INDEX idx_batch_process ON batch_logs(process_name); 