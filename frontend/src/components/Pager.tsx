'use client';
import { useI18n } from '@/lib/i18n';
import { PAGE_SIZES } from '@/lib/paging';

/** Footer shared by every table: range, rows per page, first / previous / next / last. */
export function Pager({ total, page, size, onPage, onSize }: { total: number; page: number; size: number; onPage: (p: number) => void; onSize: (s: number) => void }) {
  const { t, lang } = useI18n();
  const pages = Math.max(1, Math.ceil(total / size));
  const from = total === 0 ? 0 : page * size + 1;
  const to = Math.min(total, (page + 1) * size);
  const rtl = lang === 'ar';
  return (
    <div className="pager">
      <span>{from}–{to} / {total} {t('results')}</span>
      <span className="pager-ctl">
        <label className="check-row">{t('rowsPerPage')}
          <select value={size} onChange={(e) => onSize(Number(e.target.value))} aria-label={t('rowsPerPage')}>
            {PAGE_SIZES.map((s) => <option key={s} value={s}>{s}</option>)}
          </select>
        </label>
        <button className="btn sm" type="button" disabled={page === 0} onClick={() => onPage(0)} aria-label={t('first')}>{rtl ? '»' : '«'}</button>
        <button className="btn sm" type="button" disabled={page === 0} onClick={() => onPage(page - 1)}>{t('prev')}</button>
        <b>{page + 1} / {pages}</b>
        <button className="btn sm" type="button" disabled={page >= pages - 1} onClick={() => onPage(page + 1)}>{t('next')}</button>
        <button className="btn sm" type="button" disabled={page >= pages - 1} onClick={() => onPage(pages - 1)} aria-label={t('last')}>{rtl ? '«' : '»'}</button>
      </span>
    </div>
  );
}
