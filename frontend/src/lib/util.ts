import { Me } from './api';
import { Lang } from './i18n';

export const pick = (lang: Lang, o: { nomFr?: string; nomAr?: string } | undefined) => (o ? (lang === 'ar' ? o.nomAr || o.nomFr : o.nomFr) ?? '' : '');
export const provinceName = (lang: Lang, p: { nomFr?: string; nomAr?: string }) => pick(lang, p);
export const meScope = (me: Me, lang: Lang, t: (k: string) => string) => {
  if (me.role === 'ADMIN') return t('national');
  const prov = me.provinceNomFr ? (lang === 'ar' ? me.provinceNomAr || me.provinceNomFr : me.provinceNomFr) : '';
  const reg = me.regionNomFr ? (lang === 'ar' ? me.regionNomAr || me.regionNomFr : me.regionNomFr) : '';
  return me.role === 'PROVINCE' ? `${prov} · ${reg}` : reg;
};
export const canWrite = (me: Me) => me.role !== 'REGIONAL';
export const fmtDate = (iso?: string) => (iso ? iso.slice(0, 10).split('-').reverse().join('/') : '');
export const fmtDateTime = (iso?: string) => (iso ? `${fmtDate(iso)} ${iso.slice(11, 16)}` : '');
export const initials = (b: { nomFr: string; prenomFr: string }) => ((b.prenomFr[0] ?? '') + (b.nomFr.replace(/^(EL|AL) /, '')[0] ?? '')).toUpperCase();
export const todayIso = () => new Date().toISOString().slice(0, 10);
