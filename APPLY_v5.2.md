# Apply v5.2 (on top of v5.1)

1. Copy the folders in this zip over your project (same relative paths). Nothing is deleted.
2. New backend files: `logic/AdvisorPlanner.java`, `logic/ExpenseCategories.java`, `logic/AuthMessages.java`, `service/AiClient.java`, `service/AnthropicClient.java`.
3. `backend/.env`: add `ANTHROPIC_API_KEY=...` (optional). Anthropic is used when its key is set, otherwise OpenAI. `AI_PROVIDER=anthropic|openai|auto` forces one.
4. Backend: `mvn test` (expect more than the 43 tests from v5.1: new tests for pay frequency, expense categories, advisor planner, advisor provider choice).
5. Frontend: `npm run build`.
6. Database: no schema change.
