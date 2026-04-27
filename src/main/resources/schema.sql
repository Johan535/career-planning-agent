-- 启用 pgvector 扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- 创建 vector_store 表
CREATE TABLE IF NOT EXISTS vector_store (
    id UUID PRIMARY KEY,
    content TEXT,
    metadata JSON,
    embedding VECTOR(1536)
);

-- 创建 HNSW 索引
CREATE INDEX IF NOT EXISTS vector_store_embedding_idx 
ON vector_store 
USING hnsw (embedding vector_cosine_ops);
