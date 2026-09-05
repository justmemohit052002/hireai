import { useEffect, useState } from 'react';

// ─────────────────────────────────────────
// Custom Hooks — Shared
// ─────────────────────────────────────────

/** Debounces a value by `delay` ms. Useful for search inputs. */
export function useDebounce(value, delay = 300) {
  const [debounced, setDebounced] = useState(value);

  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(timer);
  }, [value, delay]);

  return debounced;
}

/** Returns true if the viewport matches the given media query. */
export function useMediaQuery(query) {
  const [matches, setMatches] = useState(() => window.matchMedia(query).matches);

  useEffect(() => {
    const mq = window.matchMedia(query);
    const handler = (e) => setMatches(e.matches);
    mq.addEventListener('change', handler);
    return () => mq.removeEventListener('change', handler);
  }, [query]);

  return matches;
}

/** Returns true if viewport is mobile (< 768px). */
export function useIsMobile() {
  return useMediaQuery('(max-width: 767px)');
}

/** Tracks scroll position, returning { x, y }. */
export function useScrollPosition() {
  const [position, setPosition] = useState({ x: 0, y: 0 });

  useEffect(() => {
    const handler = () => setPosition({ x: window.scrollX, y: window.scrollY });
    window.addEventListener('scroll', handler, { passive: true });
    return () => window.removeEventListener('scroll', handler);
  }, []);

  return position;
}

/** Copies text to clipboard and returns a `copied` state that resets after 2s. */
export function useClipboard() {
  const [copied, setCopied] = useState(false);

  const copy = async (text) => {
    await navigator.clipboard.writeText(text);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return { copy, copied };
}

/** Persists state to localStorage. */
export function useLocalStorage(key, initial) {
  const [value, setValue] = useState(() => {
    try {
      const stored = localStorage.getItem(key);
      return stored !== null ? JSON.parse(stored) : initial;
    } catch {
      return initial;
    }
  });

  const set = (v) => {
    const next = typeof v === 'function' ? v(value) : v;
    setValue(next);
    localStorage.setItem(key, JSON.stringify(next));
  };

  return [value, set];
}
