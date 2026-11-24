'use client';
import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { api, AuditRow, Page } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { useI18n } from '@/lib/i18n';
import { fmtDateTime } from '@/lib/util';

export default function AuditPage() {
  const { me } = useAuth();
  const { t } = useI18n();
  const router = useRouter();
  const [page, setPage] = useState(0);
  const [data, setData] = useState<Page<AuditRow> | null>(null);
  useEffect(() => { if (me && me.role !== 'ADMIN') router.replace('/dashboard'); }, [me, router]);
  useEffect(() => { if (me?.role === 'ADMIN') api<Page<AuditRow>>(`/audit?page=${page}&size=50`).then(setData).catch(() => {}); }, [me, page]);
  if (!data) return <div className="center"><div className="spinner" /></div>;
  const pages = Math.max(1, Math.ceil(data.total / data.size));
  return (
    <>
      <div className="top"><div><h1>{t('auditTitle')}</h1><p className="sub">{t('auditSub')}</p></div></div>
      <div className="card">
        <div className="tablewrap" style={{ marginTop: 0 }}>
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
            </tbody>
          </table>
        </div>
        <div className="pager"><span>{data.total}</span>
          <span style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
            <button className="btn sm" type="button" disabled={page === 0} onClick={() => setPage(page - 1)}>{t('prev')}</button><b>{page + 1} / {pages}</b>
            <button className="btn sm" type="button" disabled={page >= pages - 1} onClick={() => setPage(page + 1)}>{t('next')}</button>
          </span>
        </div>
      </div>
    </>
  );
}
