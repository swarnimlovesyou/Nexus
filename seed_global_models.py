import sqlite3
import os

db_path = "nexus.db"

if not os.path.exists(db_path):
    print(f"Error: Database {db_path} not found.")
    exit(1)

conn = sqlite3.connect(db_path)
cursor = conn.cursor()

# Format: (Name, Provider, CostPer1kTokens, [Suitability Map: {Task: Score}])
models = [
    # OPENAI
    ("gpt-4o", "OPENAI", 0.0050, {"CODE_GENERATION": 0.95, "REASONING": 0.98, "SUMMARIZATION": 0.90}),
    ("gpt-4o-mini", "OPENAI", 0.0003, {"GENERAL_CHAT": 0.90, "SUMMARIZATION": 0.85}),
    ("gpt-4-turbo", "OPENAI", 0.0150, {"REASONING": 0.95}),

    # ANTHROPIC (Direct or via OpenRouter/AWS)
    ("claude-3-5-sonnet", "ANTHROPIC", 0.0060, {"CODE_GENERATION": 0.98, "REASONING": 0.96, "CREATIVE_WRITING": 0.95}),
    ("claude-3-opus", "ANTHROPIC", 0.0450, {"REASONING": 0.99, "GENERAL_KNOWLEDGE": 0.97}),
    ("claude-3-haiku", "ANTHROPIC", 0.0005, {"GENERAL_CHAT": 0.95, "DATA_EXTRACTION": 0.90}),

    # GOOGLE
    ("gemini-1.5-pro", "GOOGLE", 0.0035, {"REASONING": 0.92, "SUMMARIZATION": 0.98}),
    ("gemini-1.5-flash", "GOOGLE", 0.0004, {"DATA_EXTRACTION": 0.95, "GENERAL_CHAT": 0.90}),

    # ALIBABA (QWEN)
    ("qwen-max", "ALIBABA", 0.0015, {"CODE_GENERATION": 0.90, "REASONING": 0.88}),
    ("qwen-plus", "ALIBABA", 0.0005, {"GENERAL_CHAT": 0.85}),
    ("qwen-turbo", "ALIBABA", 0.0001, {"SUMMARIZATION": 0.80}),

    # MOONSHOT (KIMI)
    ("kimi-k2", "MOONSHOT", 0.0012, {"GENERAL_KNOWLEDGE": 0.90, "REASONING": 0.85}),
    ("kimi-k2-thinking", "MOONSHOT", 0.0025, {"REASONING": 0.96}),

    # GROQ (ULTRA FAST)
    ("llama-3-70b-8192", "GROQ", 0.0007, {"REASONING": 0.85, "CODE_GENERATION": 0.82}),
    ("llama-3-8b-8192", "GROQ", 0.0001, {"GENERAL_CHAT": 0.88}),
    ("mixtral-8x7b-32768", "GROQ", 0.0002, {"DATA_EXTRACTION": 0.85})
]

print("Nexus Global Model Registry Seeding...")

for name, provider, cost, suitabilities in models:
    # 1. Insert/Update Model
    cursor.execute("""
        INSERT OR REPLACE INTO llm_models (name, provider, cost_per_1k_tokens) 
        VALUES (?, ?, ?)
    """, (name, provider, cost))
    
    # Get the model ID (works for REPLACE)
    cursor.execute("SELECT id FROM llm_models WHERE name = ? AND provider = ?", (name, provider))
    model_id = cursor.fetchone()[0]

    # 2. Seed Suitability Scores
    for task_name, score in suitabilities.items():
        cursor.execute("""
            INSERT OR REPLACE INTO model_suitability (model_id, task_type, base_score)
            VALUES (?, ?, ?)
        """, (model_id, task_name, score))

conn.commit()
conn.close()

print("\nSuccess! Global Registry populated for:")
print(" - OpenAI (GPT-4o/Mini/Turbo)")
print(" - Anthropic (Claude 3.5/Opus/Haiku)")
print(" - Google (Gemini Pro/Flash)")
print(" - Alibaba (Qwen Max/Plus/Turbo)")
print(" - Moonshot (Kimi K2/Thinking)")
print(" - Groq (Llama 3/Mixtral)")
print("\nYour Nexus Hub is now theoretically aware of the entire world-class LLM landscape.")
