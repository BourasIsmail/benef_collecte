const IC: Record<string, string> = {
  dash: '<rect x="3" y="3" width="7" height="9" rx="2"/><rect x="14" y="3" width="7" height="5" rx="2"/><rect x="14" y="12" width="7" height="9" rx="2"/><rect x="3" y="16" width="7" height="5" rx="2"/>',
  list: '<circle cx="9" cy="8" r="3.5"/><path d="M2.5 20c.6-3.6 3.2-5.5 6.5-5.5s5.9 1.9 6.5 5.5"/><circle cx="17.5" cy="9" r="2.5"/><path d="M17 14.5c2.6 0 4.2 1.5 4.5 4.5"/>',
  form: '<circle cx="12" cy="12" r="9"/><path d="M12 8v8M8 12h8"/>',
  asso: '<path d="M4 21V8l8-5 8 5v13"/><path d="M9 21v-6h6v6"/>',
  etab: '<path d="M3 21h18M5 21V10l7-5 7 5v11"/><path d="M9 21v-5h6v5M9 12h.01M15 12h.01"/>',
  users: '<rect x="4" y="11" width="16" height="10" rx="2"/><path d="M8 11V8a4 4 0 018 0v3"/>',
  audit: '<path d="M6 3h9l4 4v14H6z"/><path d="M14 3v5h5M9 13h7M9 17h7"/>',
  out: '<path d="M9 4H5a2 2 0 00-2 2v12a2 2 0 002 2h4"/><path d="M16 8l4 4-4 4M20 12H9"/>',
  globe: '<circle cx="12" cy="12" r="9"/><path d="M3 12h18M12 3c3 3 3 15 0 18M12 3c-3 3-3 15 0 18"/>',
  edit: '<path d="M4 20h4L19 9l-4-4L4 16z"/>', trash: '<path d="M5 7h14M10 7V4h4v3M7 7l1 13h8l1-13"/>',
  check: '<circle cx="12" cy="12" r="9"/><path d="M8 12.5l2.7 2.7L16 9.5"/>', alert: '<path d="M12 3l10 18H2z"/><path d="M12 10v5M12 18v.1"/>',
  dup: '<rect x="8" y="8" width="12" height="12" rx="2"/><path d="M16 8V6a2 2 0 00-2-2H6a2 2 0 00-2 2v8a2 2 0 002 2h2"/>',
  search: '<circle cx="11" cy="11" r="7"/><path d="M20 20l-4-4"/>', pin: '<path d="M12 21s7-6.2 7-11a7 7 0 10-14 0c0 4.8 7 11 7 11z"/><circle cx="12" cy="10" r="2.5"/>',
  heart: '<path d="M12 20s-8-5-8-11a4.5 4.5 0 018-2.7A4.5 4.5 0 0120 9c0 6-8 11-8 11z"/>', eye: '<path d="M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12z"/><circle cx="12" cy="12" r="3"/>',
  shield: '<path d="M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6z"/><path d="M9 12l2 2 4-4"/>', user: '<circle cx="12" cy="8" r="4"/><path d="M4 21c.8-4 4-6 8-6s7.2 2 8 6"/>',
  bolt: '<path d="M13 3L5 14h6l-1 7 8-11h-6z"/>', download: '<path d="M12 4v11M7 11l5 5 5-5M5 20h14"/>', key: '<circle cx="8" cy="15" r="4"/><path d="M11 12l9-9M16 7l3 3"/>',
};

export function Icon({ name, size = 18 }: { name: string; size?: number }) {
  return <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" dangerouslySetInnerHTML={{ __html: IC[name] ?? '' }} />;
}
