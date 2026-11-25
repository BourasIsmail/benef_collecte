'use client';
import { useEffect, useId, useMemo, useRef, useState } from 'react';
import { useI18n } from '@/lib/i18n';
import { normText } from '@/lib/util';

export interface Opt { value: string; label: string; hint?: string }

/** Searchable single select (the association list has ~1000 entries). Accessible combobox with keyboard support. */
export function SearchSelect({ id, options, value, onChange, placeholder, disabled, required, max = 40 }: {
  id?: string; options: Opt[]; value: string; onChange: (v: string) => void; placeholder?: string; disabled?: boolean; required?: boolean; max?: number;
}) {
  const { t } = useI18n();
  const uid = useId();
  const selected = options.find((o) => o.value === value);
  const [open, setOpen] = useState(false);
  const [q, setQ] = useState('');
  const [active, setActive] = useState(0);
  const box = useRef<HTMLDivElement>(null);

  const filtered = useMemo(() => {
    const n = normText(q);
    const all = n ? options.filter((o) => normText(o.label).includes(n) || (o.hint ? normText(o.hint).includes(n) : false)) : options;
    return all;
  }, [options, q]);
  const shown = filtered.slice(0, max);

  useEffect(() => {
    const h = (e: MouseEvent) => { if (box.current && !box.current.contains(e.target as Node)) { setOpen(false); setQ(''); } };
    document.addEventListener('mousedown', h);
    return () => document.removeEventListener('mousedown', h);
  }, []);
  useEffect(() => setActive(0), [q]);

  const pick = (o: Opt) => { onChange(o.value); setOpen(false); setQ(''); };
  const onKey = (e: React.KeyboardEvent) => {
    if (e.key === 'ArrowDown') { e.preventDefault(); setOpen(true); setActive((a) => Math.min(a + 1, shown.length - 1)); }
    else if (e.key === 'ArrowUp') { e.preventDefault(); setActive((a) => Math.max(a - 1, 0)); }
    else if (e.key === 'Enter' && open && shown[active]) { e.preventDefault(); pick(shown[active]); }
    else if (e.key === 'Escape') { setOpen(false); setQ(''); }
  };

  return (
    <div className="ss" ref={box}>
      <input id={id} type="text" role="combobox" aria-expanded={open} aria-controls={uid} aria-autocomplete="list" autoComplete="off"
        dir="auto" disabled={disabled} required={required && !value} placeholder={placeholder}
        value={open ? q : selected?.label ?? ''} onFocus={() => setOpen(true)} onChange={(e) => { setQ(e.target.value); setOpen(true); }} onKeyDown={onKey} />
      {open && !disabled && (
        <ul className="ss-list" id={uid} role="listbox">
          {shown.length === 0 && <li className="ss-empty">{t('ssNoResult')}</li>}
          {shown.map((o, i) => (
            <li key={o.value} role="option" aria-selected={o.value === value} className={'ss-opt' + (i === active ? ' on' : '')}
              onMouseDown={(e) => { e.preventDefault(); pick(o); }} onMouseEnter={() => setActive(i)}>
              <span dir="auto">{o.label}</span>{o.hint && <small>{o.hint}</small>}
            </li>
          ))}
          {filtered.length > shown.length && <li className="ss-empty">{t('ssMore')}</li>}
        </ul>
      )}
    </div>
  );
}
