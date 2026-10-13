# HireAI Project Directives & Design Philosophy

This codebase is a serious, production-grade technical recruitment platform. To maintain strict visual and architectural integrity, adhere to the following rules across all tasks:

1. **Aesthetics & UI Styling**:
   - Strictly prohibit purple gradients and arbitrary purple/magenta hex values (`#6D3DF5`, `#C63FC5`, `text-purple-*`, `bg-purple-*`). Stick to the official brand blues (`#004499`, `#0192C6`, `#112358`), neutral grays/slates, emerald accents for verified states, and amber for pending states.
   - Absolutely no pill-shaped buttons (`rounded-full` on buttons or CTAs). Use modern, clean `rounded-lg` or `rounded-xl` borders.
   - Do not include gimmicky cursor animations or over-the-top scroll animations.

2. **Copywriting & Transparency**:
   - Zero fake reviews, fake metrics, fake customer counters, or fabricated company client lists. Ground all marketing and statistics in genuine system capabilities (deterministic ATS parsing, multi-skill matrix, pipeline stages).
   - Crisp, direct, technical copy with no vague buzzwords.
   - Never use emoji icons (e.g. ✨, 🚀, 🤖, 🎉) in UI labels, badges, or notification items. Use Lucide SVG icons instead.
   - Never use em dashes (`—`). Use standard hyphens (`-`), colons, or parentheses.
   - No AI-slop photos or synthetic stock imagery.

3. **Launch Completeness**:
   - Custom domain setup readiness.
   - Clean, professional HireAI SVG favicon (brand neural node/mark, never a generic purple bolt).
   - No "Made with AI" tags anywhere.
   - Fully functioning and linked Privacy Policy (`/privacy`) and Terms & Conditions (`/terms`) pages.
   - High technical quality with zero errors.
