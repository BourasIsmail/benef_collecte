'use client';
import { useEffect, useMemo, useState } from 'react';
import { useRouter } from 'next/navigation';
import { api, AuditRow, Page } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { useI18n } from '@/lib/i18n';
import { Icon } from '@/lib/icons';
import { Pager } from '@/components/Pager';
import { fmtDateTime } from '@/lib/util';

export default function AuditPage() {
  const { me } = useAuth();
  const { t } = useI18n();
  const router = useRouter();
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(50);
  const [action, setAction] = useState('');
  const [user, setUser] = useState('');
  const [duser, setDuser] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [actions, setActions] = useState<string[]>([]);
  const [data, setData] = useState<Page<AuditRow> | null>(null);

  useEffect(() => { if (me && me.role !== 'ADMIN') router.replace('/dashboard'); }, [me, router]);
  useEffect(() => { if (me?.role === 'ADMIN') api<string[]>('/audit/actions').then(setActions).catch(() => {}); }, [me]);
  useEffect(() => { const h = setTimeout(() => { setDuser(user); setPage(0); }, 300); return () => clearTimeout(h); }, [user]);

  const params = useMemo(() => {
    const p = new URLSearchParams({ page: String(page), size: String(size) });
    if (action) p.set('action', action);
    if (duser) p.set('user', duser);
    if (from) p.set('from', from);
    if (to) p.set('to', to);
    return p;
  }, [page, size, action, duser, from, to]);
  useEffect(() => { if (me?.role === 'ADMIN') api<Page<AuditRow>>('/audit?' + params).then(setData).catch(() => {}); }, [me, params]);

  if (!data) return <div className="center"><div className="spinner" /></div>;
  return (
    <>
      <div className="top"><div><h1>{t('auditTitle')}</h1><p className="sub">{t('auditSub')}</p></div></div>
      <div className="card">
        <div className="filters">
          <div className="field"><Icon name="search" /><input type="text" placeholder={t('auditUser')} value={user} onChange={(e) => setUser(e.target.value)} dir="ltr" aria-label={t('auditUser')} /></div>
          <select value={action} onChange={(e) => { setAction(e.target.value); setPage(0); }} aria-label={t('allActions')}>
            <option value="">{t('allActions')}</option>
            {actions.map((a) => <option key={a} value={a}>{a}</option>)}
          </select>
          <label className="check-row">{t('dateFrom')}<input className="date" type="date" value={from} max={to || undefined} onChange={(e) => { setFrom(e.target.value); setPage(0); }} /></label>
          <label className="check-row">{t('dateTo')}<input className="date" type="date" value={to} min={from || undefined} onChange={(e) => { setTo(e.target.value); setPage(0); }} /></label>
          {(user || action || from || to) && <button className="linkbtn" type="button" onClick={() => { setUser(''); setAction(''); setFrom(''); setTo(''); setPage(0); }}>{t('resetFilters')}</button>}
        </div>
        <div className="tablewrap">
          <table>
            <thead><tr><th>{t('colDate')}</th><th>{t('colUser')}</th><th>{t('colAction')}</th><th>{t('colDetail')}</th></tr></thead>
            <tbody>
              {data.items.map((a) => (
                <tr key={a.id}>
                  <td style={{ whiteSpace: 'nowrap' }}>{fmtDateTime(a.at)}</td>
                  <td dir="ltr" style={{ textAlign: 'start' }}>{a.userEmail}</td>
                  <td><span className="pill">{a.action}</span></td>
                  <td className="mono" dir="ltr" style={{ textAlign: 'start' }}>{[a.entity, a.entityId, a.details].filter(Boolean).join(' · ')}</td>
                </tr>
              ))}
              {data.items.length === 0 && <tr><td colSpan={4} className="muted">{t('empty')}</td></tr>}
            </tbody>
          </table>
        </div>
        <Pager total={data.total} page={page} size={size} onPage={setPage} onSize={(n) => { setSize(n); setPage(0); }} />
      </div>
    </>
  );
}
