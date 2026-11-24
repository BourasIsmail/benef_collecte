'use client';
import Link from 'next/link';
import { useEffect, useState } from 'react';
import { api, Count, Stats } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { useI18n } from '@/lib/i18n';
import { Icon } from '@/lib/icons';
import { canWrite, initials } from '@/lib/util';

function Bars({ items, color, lang }: { items: Count[]; color: string; lang: string }) {
  const max = Math.max(1, ...items.map((i) => i.count));
  return (
    <div className="bars">
      {items.map((i, k) => {
        const label = lang === 'ar' ? i.labelAr || i.label : i.label;
        return (
          <div className="bar" key={k}>
            <span className="n" title={label}>{label}</span>
            <span className="t"><i style={{ width: `${(i.count / max) * 100}%`, background: color }} /></span>
            <span className="c">{i.count}</span>
          </div>
        );
      })}
    </div>
  );
}

export default function Dashboard() {
  const { me } = useAuth();
  const { t, lang } = useI18n();
  const [s, setS] = useState<Stats | null>(null);
  useEffect(() => { api<Stats>('/stats').then(setS).catch(() => {}); }, []);
  if (!me || !s) return <div className="center"><div className="spinner" /></div>;

  const gp = s.total ? Math.round((s.girls / s.total) * 100) : 0;
  const multiProvince = s.provinces > 1;
  const provItems = s.byProvince.slice(0, 12);
  const monthMax = Math.max(1, ...s.monthly.map((m) => m.count));
  const monthName = (ym: string) => new Intl.DateTimeFormat(lang === 'ar' ? 'ar-MA-u-nu-latn' : 'fr-FR', { month: 'short' }).format(new Date(ym + '-01T12:00:00'));
  const kpi = (chip: string, icon: string, v: string | number, l: string) => (
    <div className="card kpi"><span className={`chip ${chip}`}><Icon name={icon} size={22} /></span><div><div className="v">{v}</div><div className="l">{l}</div></div></div>
  );

  return (
    <>
      <div className="hero">
        <div><h1>{t('hello')}</h1><p>{t('helloSub')}</p></div>
        {canWrite(me)
          ? <Link className="btn hero-btn" href="/beneficiaries/new"><Icon name="form" />{t('cta')}</Link>
          : <span className="pill" style={{ background: 'rgba(255,255,255,.2)', color: '#fff', position: 'relative', zIndex: 1 }}><Icon name="eye" size={14} />{t('readonly')}</span>}
      </div>
      <div className="grid g4">
        {kpi('c-brand', 'list', s.total, t('kpi_b'))}
        {kpi('c-brand', 'asso', s.associations, t('kpi_a'))}
        {kpi('c-brand', 'pin', s.provinces, t('kpi_p'))}
        {kpi('c-pink', 'heart', s.total ? gp + '%' : '–', t('kpi_f'))}
      </div>
      <div className="grid g21">
        <div className="card">
          <div className="head"><h2>{t('monthly')}</h2><span>{t('monthlySub')}</span></div>
          <div className="months">
            {s.monthly.map((m, i) => (
              <div key={m.month} className={i === s.monthly.length - 1 ? 'last' : ''} title={`${m.month} : ${m.count}`}>
                <b>{i === s.monthly.length - 1 || m.count === monthMax ? m.count : ''}</b>
                <i style={{ height: `${Math.max(3, (m.count / monthMax) * 78)}%` }} />
                <em>{monthName(m.month)}</em>
              </div>
            ))}
          </div>
        </div>
        <div className="card">
          <div className="head"><h2>{t('sexSplit')}</h2></div>
          <div className="donutrow">
            <div className="donut" role="img" aria-label={`${t('girls')} ${s.girls}, ${t('boys')} ${s.boys}`}
              style={{ background: s.total ? `conic-gradient(var(--pink) 0 ${gp}%, var(--sky) ${gp}% 100%)` : 'var(--line)' }}>
              <div><b>{s.total}</b><span>{t('total')}</span></div>
            </div>
            <div className="legend">
              <div className="it"><i className="dot" style={{ background: 'var(--pink)' }} /><div><b>{s.girls}</b><span>{t('girls')} · {gp}%</span></div></div>
              <div className="it"><i className="dot" style={{ background: 'var(--sky)' }} /><div><b>{s.boys}</b><span>{t('boys')} · {s.total ? 100 - gp : 0}%</span></div></div>
            </div>
          </div>
        </div>
      </div>
      <div className="grid g21">
        <div className="card">
          <div className="head"><h2>{multiProvince ? t('byProvince') : t('byAssociation')}</h2>{multiProvince && s.byProvince.length > provItems.length && <span>+{s.byProvince.length - provItems.length} {t('moreProvinces')}</span>}</div>
          <Bars items={multiProvince ? provItems : s.byAssociation} color="var(--brand)" lang={lang} />
        </div>
        <div className="card"><div className="head"><h2>{t('ageBands')}</h2></div><Bars items={s.ageBands.map((a) => ({ ...a, label: `${a.label} ${t('ageYears')}`, labelAr: `${a.label} ${t('ageYears')}` }))} color="var(--sun)" lang={lang} /></div>
        <div className="card"><div className="head"><h2>{t('byCategory')}</h2></div><Bars items={s.associationsByCategory} color="var(--brand2)" lang={lang} /></div>
        <div className="card">
          <div className="head"><h2>{t('latest')}</h2></div>
          <div className="feed">
            {s.latest.map((b) => (
              <div className="row" key={b.id}>
                <span className={`av ${b.sexe}`}>{initials(b)}</span>
                <div><b>{b.prenomFr} {b.nomFr}</b><small>{b.associationNom}</small></div>
              </div>
            ))}
            {s.latest.length === 0 && <span className="muted">{t('empty')}</span>}
          </div>
        </div>
      </div>
    </>
  );
}
