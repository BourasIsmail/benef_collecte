'use client';
import { FormEvent, useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { api, ApiError, Me } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { useI18n } from '@/lib/i18n';
import { LangButton } from '@/components/Shell';

export default function LoginPage() {
  const { t, err } = useI18n();
  const { me, loading, setMe } = useAuth();
  const router = useRouter();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => { if (!loading && me) router.replace(me.mustChangePassword ? '/change-password' : '/dashboard'); }, [me, loading, router]);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true); setError('');
    try {
      const m = await api<Me>('/auth/login', { method: 'POST', body: { email, password } });
      setMe(m);
      router.replace(m.mustChangePassword ? '/change-password' : '/dashboard');
    } catch (ex) { setError(err(ex instanceof ApiError ? ex.code : 'INTERNAL')); }
    finally { setBusy(false); }
  }

  return (
    <div className="narrow">
      <img className="lg" src="/logo.png" alt="Royaume du Maroc – Entraide Nationale" />
      <form className="box" onSubmit={submit}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 8 }}>
          <div><h1 style={{ fontSize: 22 }}>{t('login')}</h1><p className="muted">{t('app')}</p></div>
          <LangButton />
        </div>
        <label>{t('email')}<input id="email" type="email" autoComplete="username" required value={email} onChange={(e) => setEmail(e.target.value)} dir="ltr" /></label>
        <label>{t('password')}<input id="password" type="password" autoComplete="current-password" required value={password} onChange={(e) => setPassword(e.target.value)} dir="ltr" /></label>
        {error && <div className="alert bad" role="alert">{error}</div>}
        <button className="btn primary" style={{ justifyContent: 'center', padding: 11 }} disabled={busy} type="submit">{t('signin')}</button>
        <p className="hint">{t('loginHint')}</p>
      </form>
    </div>
  );
}
