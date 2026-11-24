'use client';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { ReactNode } from 'react';
import { useAuth } from '@/lib/auth';
import { useI18n } from '@/lib/i18n';
import { Icon } from '@/lib/icons';
import { canWrite, meScope } from '@/lib/util';

const ROLE_ICON: Record<string, [string, string]> = { ADMIN: ['shield', 'c-brand'], PROVINCE: ['pin', 'c-brand'], REGIONAL: ['eye', 'c-sky'] };

export function LangButton() {
  const { lang, setLang } = useI18n();
  return <button className="btn sm" onClick={() => setLang(lang === 'fr' ? 'ar' : 'fr')} type="button"><Icon name="globe" size={16} />{lang === 'fr' ? 'العربية' : 'Français'}</button>;
}

export function Shell({ children }: { children: ReactNode }) {
  const { me, logout } = useAuth();
  const { t, lang } = useI18n();
  const path = usePathname();
  if (!me) return null;
  const tabs = [
    { href: '/dashboard', icon: 'dash', label: 'nav_dashboard' },
    { href: '/beneficiaries', icon: 'list', label: 'nav_beneficiaries', exact: true },
    ...(canWrite(me) ? [{ href: '/beneficiaries/new', icon: 'form', label: 'nav_new' }] : []),
    { href: '/associations', icon: 'asso', label: 'nav_associations' },
    ...(me.role === 'ADMIN' ? [{ href: '/users', icon: 'users', label: 'nav_users' }, { href: '/audit', icon: 'audit', label: 'nav_audit' }] : []),
  ];
  const active = (h: string, exact?: boolean) => (exact ? path === h || /^\/beneficiaries\/\d+/.test(path) : path === h || path.startsWith(h + '/'));
  const [ri, rc] = ROLE_ICON[me.role];
  return (
    <div className="page">
      <header className="hdr">
        <img className="lg" src="/logo.png" alt="Royaume du Maroc – Entraide Nationale" />
        <div className="tools">
          <div className="user">
            <span className={`chip ${rc}`} style={{ width: 40, height: 40 }}><Icon name={ri} /></span>
            <div><b>{t('role_' + me.role)}</b><small>{meScope(me, lang, t)}{canWrite(me) ? '' : ' · ' + t('readonly')}</small></div>
          </div>
          <LangButton />
          <button className="btn sm" onClick={logout} type="button"><Icon name="out" size={16} />{t('logout')}</button>
        </div>
      </header>
      <nav className="tabs" aria-label="Navigation">
        {tabs.map((x) => (
          <Link key={x.href} href={x.href} aria-current={active(x.href, (x as any).exact) ? 'page' : undefined}><Icon name={x.icon} />{t(x.label)}</Link>
        ))}
      </nav>
      {children}
    </div>
  );
}
