# HireAI Quality & Anti-Vibe Standards

All code, UI design, copywriting, and architecture for HireAI must strictly obey the following mandatory standards at all times.

## 1. Visual & UI Design Rules
- **NEVER use purple gradients** or arbitrary purple/violet hex codes (e.g. `#6D3DF5`, `#C63FC5`, `from-purple`, `to-violet`).
- Use the official project brand palette:
  - Deep Dark Navy: `#112358`
  - Fright Night Navy: `#004499`
  - Dynamic Blue: `#0192C6`
  - Veiling Waterfalls: `#D4EAFF`
  - Clean Neutrals / Slate / Zinc for containers and borders
  - Emerald for verified/success states, Amber for pending/screening states
- **NEVER use pill-shaped buttons** (`rounded-full` on buttons, navigation bars, or CTAs). Use clean, professional geometric radii (`rounded-lg` or `rounded-xl`).
- **NO over-the-top animations or gimmicks**:
  - No custom cursor animations or cursor canvas effects.
  - No disorienting parallax or extreme scroll triggers.
  - Micro-interactions must be fast, subtle, and utilitarian (0.15s - 0.25s transitions).

## 2. Copywriting & Content Integrity
- **NO fake reviews, fake ratings, or fake testimonials**: Never fabricate user quotes or 5-star badges.
- **NO fake metrics or fake customer counters**: Never invent arbitrary numbers like "45,000+ candidates" or "+27,000 companies". Always present genuine technical specifications, architectural features, and workflow capabilities.
- **NO vague hero texts or buzzword soup**: Headings and descriptions must be concrete, direct, and technically accurate.
- **NO emoji icons**: Never use emojis (such as ✨, 🚀, 🤖, 🎉, 📝) in UI elements or notification alerts. Use clean SVG / Lucide React icons exclusively.
- **NO em dashes (`—`)**: Never use em dashes in UI copy. Use hyphens (`-`), colons (`:`), or parentheses `()` instead.
- **NO AI-slop photos or AI-slop copy**: No synthetic uncanny-valley stock photos or generic generic sales pitch fluff. Real UI previews and authentic system diagrams only.

## 3. Pre-Launch Requirements
- Connected custom domain configured.
- Clean, brand-aligned SVG favicon in place (no generic purple lightning bolts).
- Zero "Made with AI" tags or badges anywhere in the application.
- Dedicated, fully routed, and comprehensive **Privacy Policy** (`/privacy`) and **Terms and Conditions** (`/terms`) pages.
- Absolute engineering rigor: zero broken links, clean syntax, type/prop safety, and robust error handling.
