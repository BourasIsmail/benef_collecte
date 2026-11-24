'use client';
import { FormEvent, useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { api, ApiError, Me } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { useI18n } from '@/lib/i18n';
import { LangButton } from '@/components/Shell';

export default function ChangePasswordPage() {
  const { t, err } = useI18n();
  const { me, loading, setMe, logout } = useAuth();
  const router = useRouter();
  const [cur, setCur] = useState('');
  const [next, setNext] = useState('');
  const [again, setAgain] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => { if (!loading && !me) router.replace('/login'); }, [me, loading, router]);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setError('');
    if (next !== again) { setError(t('pwdMismatch')); return; }
    setBusy(true);
    try {
      const m = await api<Me>('/auth/change-password', { method: 'POST', body: { currentPassword: cur, newPassword: next } });
      setMe(m);
      router.replace('/dashboard');
    } catch (ex) { setError(err(ex instanceof ApiError ? ex.code : 'INTERNAL')); }
    finally { setBusy(false); }
  }

  if (loading || !me) return <div className="center"><div className="spinner" /></div>;
  return (
    <div className="narrow">
      <img className="lg" src="/logo.png" alt="Royaume du Maroc – Entraide Nationale" />
      <form className="box" onSubmit={submit}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 8 }}>
          <h1 style={{ fontSize: 22 }}>{t('changePwdTitle')}</h1><LangButton />
        </div>
        <p className="muted">{t('changePwdIntro')}</p>
        <label>{t('currentPwd')}<input id="current" type="password" autoComplete="current-password" required value={cur} onChange={(e) => setCur(e.target.value)} dir="ltr" /></label>
        <label>{t('newPwd')}<input id="new" type="password" autoComplete="new-password" required minLength={10} value={next} onChange={(e) => setNext(e.target.value)} dir="ltr" /></label>
        <label>{t('confirmPwd')}<input id="again" type="password" autoComplete="new-password" required value={again} onChange={(e) => setAgain(e.target.value)} dir="ltr" /></label>
        <p className="hint">{t('pwdRules')}</p>
        {error && <div className="alert bad" role="alert">{error}</div>}
        <div className="actions" style={{ marginTop: 0 }}>
          <button className="btn primary" disabled={busy} type="submit">{t('save')}</button>
          <button className="btn" type="button" onClick={logout}>{t('logout')}</button>
        </div>
      </form>
    </div>
  );
}
