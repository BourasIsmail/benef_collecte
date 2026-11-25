import { useEffect, useState } from 'react';

export const PAGE_SIZES = [10, 20, 50, 100];

/** Client-side pagination of an already filtered list. The page goes back to 1 when `resetKey` (the filters) changes. */
export function usePaged<T>(items: T[] | null, resetKey: string, initialSize = 20) {
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(initialSize);
  useEffect(() => setPage(0), [resetKey, size]);
  const total = items?.length ?? 0;
  const pages = Math.max(1, Math.ceil(total / size));
  const current = Math.min(page, pages - 1);
  return { rows: items ? items.slice(current * size, (current + 1) * size) : null, page: current, size, total, setPage, setSize };
}
