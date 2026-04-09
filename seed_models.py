import sqlite3
import os

db_path = "nexus.db"

if not os.path.exists(db_path):
    print(f"Error: Database {db_path} not found.")
    exit(1)

conn = sqlite3.connect(db_path)
cursor = conn.cursor()

# 1. Clear existing models to avoid duplicates (optional, but cleaner)
# cursor.execute("DELETE FROM model_suitability")
# cursor.execute("DELETE FROM llm_models")

models = [
    # GROQ
    ("llama-3-70b-8192", "GROQ", 0.0006),
    ("llama-3-8b-8192", "GROQ", 0.0001),
    ("mixtral-8x7b-32768", "GROQ", 0.0002),
    
    # OPENROUTER (Representative Pricing)
    ("anthropic/claude-3.5-sonnet", "OPENROUTER", 0.003),
    ("google/gemini-pro-1.5", "OPENROUTER", 0.00125),
    ("mistralai/mixtral-8x7b-instruct", "OPENROUTER", 0.0003),
    
    # NATIVE
    ("gpt-4o", "OPENAI", 0.01),
    ("claude-3-opus-20240229", "ANTHROPIC", 0.015)
]

print("Seeding Model Registry...")
for name, provider, cost in models:
    cursor.execute("INSERT OR REPLACE INTO llm_models (name, provider, cost_per_1k_tokens) VALUES (?, ?, ?)", 
                   (name, provider, cost))
    model_id = cursor.lastrowid
    
    # Add default suitability scores for Reasoning and Coding
    cursor.execute("INSERT OR REPLACE INTO model_suitability (model_id, task_type, base_score) VALUES (?, ?, ?)", 
                   (model_id, "CODE_GENERATION", 0.8))
    cursor.execute("INSERT OR REPLACE INTO model_suitability (model_id, task_type, base_score) VALUES (?, ?, ?)", 
                   (model_id, "REASONING", 0.85))

conn.commit()
conn.close()
print("Success! Groq, OpenRouter, and standard models registered with latest pricing.")
