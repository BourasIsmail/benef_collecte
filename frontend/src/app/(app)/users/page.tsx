'use client';
import { useCallback, useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { api, ApiError, UserRow } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { useI18n } from '@/lib/i18n';
import { Icon } from '@/lib/icons';
import { Modal } from '@/components/Modal';
import { useToast } from '@/lib/toast';
import { fmtDateTime } from '@/lib/util';

export default function UsersPage() {
  const { me } = useAuth();
  const { t, err } = useI18n();
  const toast = useToast();
  const router = useRouter();
  const [rows, setRows] = useState<UserRow[] | null>(null);
  const [q, setQ] = useState('');
  const [role, setRole] = useState('');
  const [reset, setReset] = useState<UserRow | null>(null);
  const [error, setError] = useState('');

  useEffect(() => { if (me && me.role !== 'ADMIN') router.replace('/dashboard'); }, [me, router]);
  const load = useCallback(() => { api<UserRow[]>('/users').then(setRows).catch((e) => setError(err(e instanceof ApiError ? e.code : 'INTERNAL'))); }, [err]);
  useEffect(() => { if (me?.role === 'ADMIN') load(); }, [me, load]);

  async function doReset() {
    if (!reset) return;
    try { await api(`/users/${reset.id}/reset-password`, { method: 'POST' }); toast(t('resetDone')); } catch (e) { setError(err(e instanceof ApiError ? e.code : 'INTERNAL')); }
    setReset(null); load();
  }
  async function toggle(u: UserRow) {
    try { await api(`/users/${u.id}/enabled`, { method: 'PUT', body: { enabled: !u.enabled } }); toast(t('userUpdated')); load(); } catch (e) { setError(err(e instanceof ApiError ? e.code : 'INTERNAL')); }
  }

  if (!rows) return <div className="center"><div className="spinner" /></div>;
  const shown = rows.filter((u) => (!role || u.role === role) && (!q || u.email.includes(q.toLowerCase().trim())));
  return (
    <>
      <div className="top"><div><h1>{t('usersTitle')}</h1><p className="sub">{t('usersSub')}</p></div></div>
      <div className="card">
        <div className="filters">
          <div className="field"><Icon name="search" /><input type="text" placeholder={t('email')} value={q} onChange={(e) => setQ(e.target.value)} dir="ltr" aria-label={t('email')} /></div>
          <select value={role} onChange={(e) => setRole(e.target.value)} aria-label={t('colRole')}>
            <option value="">{t('colRole')} : {t('all')}</option>
            {['ADMIN', 'PROVINCE', 'REGIONAL'].map((r) => <option key={r} value={r}>{t('role_' + r)}</option>)}
          </select>
        </div>
        {error && <div className="alert bad" style={{ marginTop: 12 }} role="alert">{error}</div>}
        <div className="tablewrap">
          <table>
            <thead><tr><th>{t('email')}</th><th>{t('colRole')}</th><th>{t('colScope')}</th><th>{t('colState')}</th><th>{t('colLastLogin')}</th><th /></tr></thead>
            <tbody>
              {shown.map((u) => (
                <tr key={u.id}>
                  <td dir="ltr" style={{ textAlign: 'start' }}><b>{u.email}</b></td>
                  <td>{t('role_' + u.role)}</td>
                  <td>{u.provinceNomFr ?? u.regionNomFr ?? t('national')}</td>
                  <td><div className="row-actions">
                    <span className={`pill ${u.enabled ? 'ok' : 'off'}`}>{u.enabled ? t('stActive') : t('stDisabled')}</span>
                    {u.mustChangePassword && <span className="pill warn">{t('stMustChange')}</span>}
                  </div></td>
                  <td>{u.lastLogin ? fmtDateTime(u.lastLogin) : <span className="muted">{t('neverLogged')}</span>}</td>
                  <td>{u.role !== 'ADMIN' && <div className="row-actions">
                    <button className="btn sm" type="button" onClick={() => setReset(u)}><Icon name="key" size={14} />{t('resetPwd')}</button>
                    <button className={`btn sm ${u.enabled ? 'danger' : ''}`} type="button" onClick={() => toggle(u)}>{u.enabled ? t('disable') : t('enable')}</button>
                  </div>}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="pager"><span>{shown.length} / {rows.length}</span></div>
      </div>
      {reset && (
        <Modal title={t('resetTitle')} onClose={() => setReset(null)}>
          <p dir="ltr"><b>{reset.email}</b></p>
          <p className="muted">{t('resetText')}</p>
          <div className="actions" style={{ marginTop: 0 }}><button className="btn" type="button" onClick={() => setReset(null)}>{t('cancel')}</button><button className="btn primary" type="button" onClick={doReset}>{t('resetPwd')}</button></div>
        </Modal>
      )}
    </>
  );
}
