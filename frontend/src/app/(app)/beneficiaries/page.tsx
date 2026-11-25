'use client';
import Link from 'next/link';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { api, ApiError, Beneficiary, Etablissement, Page, RefData } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { useI18n } from '@/lib/i18n';
import { Icon } from '@/lib/icons';
import { Modal } from '@/components/Modal';
import { SearchSelect } from '@/components/SearchSelect';
import { useToast } from '@/lib/toast';
import { canWrite, fmtDate, initials, pick } from '@/lib/util';

export default function BeneficiariesPage() {
  const { me } = useAuth();
  const { t, lang, err } = useI18n();
  const toast = useToast();
  const [ref, setRef] = useState<RefData | null>(null);
  const [etabs, setEtabs] = useState<Etablissement[]>([]);
  const [q, setQ] = useState('');
  const [dq, setDq] = useState('');
  const [prov, setProv] = useState('');
  const [asso, setAsso] = useState('');
  const [etab, setEtab] = useState('');
  const [sexe, setSexe] = useState('');
  const [page, setPage] = useState(0);
  const [data, setData] = useState<Page<Beneficiary> | null>(null);
  const [toArchive, setToArchive] = useState<Beneficiary | null>(null);
  const [error, setError] = useState('');

  useEffect(() => { api<RefData>('/ref').then(setRef); api<Etablissement[]>('/etablissements').then(setEtabs); }, []);
  useEffect(() => { const h = setTimeout(() => { setDq(q); setPage(0); }, 300); return () => clearTimeout(h); }, [q]);

  const params = useMemo(() => {
    const p = new URLSearchParams();
    if (dq) p.set('q', dq);
    if (prov) p.set('provinceId', prov);
    if (asso) p.set('associationId', asso);
    if (etab) p.set('etablissementId', etab);
    if (sexe) p.set('sexe', sexe);
    return p;
  }, [dq, prov, asso, etab, sexe]);

  const load = useCallback(() => {
    const p = new URLSearchParams(params); p.set('page', String(page)); p.set('size', '15');
    api<Page<Beneficiary>>('/beneficiaries?' + p).then(setData).catch((e) => setError(err(e instanceof ApiError ? e.code : 'INTERNAL')));
  }, [params, page, err]);
  useEffect(load, [load]);

  const assocOptions = useMemo(() => {
    const m = new Map<number, { name: string; prov: string }>();
    for (const e of etabs) if (!prov || String(e.provinceId) === prov) m.set(e.associationId, { name: e.associationNom, prov: pick(lang, { nomFr: e.provinceNomFr, nomAr: e.provinceNomAr }) });
    return [{ value: '', label: t('allAssociations') }, ...[...m.entries()].sort((a, b) => a[1].name.localeCompare(b[1].name, 'ar')).map(([id, v]) => ({ value: String(id), label: v.name, hint: v.prov }))];
  }, [etabs, prov, t, lang]);
  const etabOptions = useMemo(() => etabs.filter((e) => (!prov || String(e.provinceId) === prov) && (!asso || String(e.associationId) === asso)), [etabs, prov, asso]);

  async function doArchive() {
    if (!toArchive) return;
    try { await api('/beneficiaries/' + toArchive.id, { method: 'DELETE' }); toast(t('archived')); setToArchive(null); load(); }
    catch (e) { setError(err(e instanceof ApiError ? e.code : 'INTERNAL')); setToArchive(null); }
  }

  if (!me) return null;
  const write = canWrite(me);
  const provs = ref?.provinces ?? [];
  const pages = data ? Math.max(1, Math.ceil(data.total / data.size)) : 1;

  return (
    <>
      <div className="top">
        <div><h1>{t('nav_beneficiaries')}</h1><p className="sub">{t('listSub')}</p></div>
        <div className="toolbar">
          <a className="btn" href={'/api/beneficiaries/export.csv?' + params}><Icon name="download" size={16} />{t('exportCsv')}</a>
          {write && <Link className="btn primary" href="/beneficiaries/new"><Icon name="form" size={16} />{t('nav_new')}</Link>}
        </div>
      </div>
      <div className="card">
        <div className="filters">
          <div className="field"><Icon name="search" /><input id="q" type="text" placeholder={t('searchPh')} value={q} onChange={(e) => setQ(e.target.value)} aria-label={t('searchPh')} /></div>
          {provs.length > 1 && (
            <select value={prov} onChange={(e) => { setProv(e.target.value); setAsso(''); setEtab(''); setPage(0); }} aria-label={t('allProvinces')}>
              <option value="">{t('allProvinces')}</option>
              {provs.map((p) => <option key={p.id} value={p.id}>{pick(lang, p)}</option>)}
            </select>
          )}
          <div style={{ flex: '1 1 220px', minWidth: 0 }}>
            <SearchSelect id="filterAssociation" options={assocOptions} value={asso} onChange={(v) => { setAsso(v); setEtab(''); setPage(0); }} placeholder={t('allAssociations')} />
          </div>
          <select value={etab} onChange={(e) => { setEtab(e.target.value); setPage(0); }} aria-label={t('colEtablissement')}>
            <option value="">{t('colEtablissement')} : {t('all')}</option>
            {etabOptions.map((e) => <option key={e.id} value={e.id}>{e.nomAr}</option>)}
          </select>
          <select value={sexe} onChange={(e) => { setSexe(e.target.value); setPage(0); }} aria-label={t('allSexes')}>
            <option value="">{t('allSexes')}</option><option value="F">{t('F')}</option><option value="M">{t('M')}</option>
          </select>
        </div>
        {error && <div className="alert bad" style={{ marginTop: 12 }} role="alert">{error}</div>}
        <div className="tablewrap">
          <table>
            <thead><tr><th>{t('colName')}</th><th>{t('colDob')}</th><th>{t('colSex')}</th><th>{t('colEtablissement')}</th><th>{t('colProvince')}</th>{write && <th />}</tr></thead>
            <tbody>
              {data?.items.map((b) => (
                <tr key={b.id}>
                  <td><div className="person"><span className={`av ${b.sexe}`}>{initials(b)}</span><div><b>{b.nomFr} {b.prenomFr}</b><div className="ar">{b.nomAr} {b.prenomAr}</div></div></div></td>
                  <td>{fmtDate(b.dateNaissance)}</td>
                  <td><span className={`pill ${b.sexe}`}>{t(b.sexe)}</span></td>
                  <td><div className="ar" style={{ color: 'var(--ink)' }}>{b.etablissementNom}</div><div className="ar" style={{ fontSize: 12 }}>{b.associationNom}</div></td>
                  <td>{pick(lang, { nomFr: b.provinceNomFr, nomAr: b.provinceNomAr })}</td>
                  {write && <td><div className="row-actions">
                    <Link className="btn sm" href={`/beneficiaries/${b.id}`}><Icon name="edit" size={14} />{t('edit')}</Link>
                    <button className="btn sm danger" type="button" onClick={() => setToArchive(b)}><Icon name="trash" size={14} />{t('archive')}</button>
                  </div></td>}
                </tr>
              ))}
              {data && data.items.length === 0 && <tr><td colSpan={6} className="muted">{t('empty')}</td></tr>}
            </tbody>
          </table>
        </div>
        <div className="pager">
          <span>{data?.total ?? 0} {t('results')}</span>
          <span style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
            <button className="btn sm" type="button" disabled={page === 0} onClick={() => setPage(page - 1)}>{t('prev')}</button>
            <b>{page + 1} / {pages}</b>
            <button className="btn sm" type="button" disabled={page >= pages - 1} onClick={() => setPage(page + 1)}>{t('next')}</button>
          </span>
        </div>
      </div>
      {toArchive && (
        <Modal title={t('archiveTitle')} onClose={() => setToArchive(null)}>
          <p><b>{toArchive.nomFr} {toArchive.prenomFr}</b></p>
          <p className="muted">{t('archiveText')}</p>
          <div className="actions"><button className="btn" type="button" onClick={() => setToArchive(null)}>{t('cancel')}</button><button className="btn danger" type="button" onClick={doArchive}>{t('archive')}</button></div>
        </Modal>
      )}
    </>
  );
}
