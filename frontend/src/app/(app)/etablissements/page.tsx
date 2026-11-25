'use client';
import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import { api, ApiError, Association, Etablissement, RefData } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { useI18n } from '@/lib/i18n';
import { Icon } from '@/lib/icons';
import { Modal } from '@/components/Modal';
import { SearchSelect } from '@/components/SearchSelect';
import { useToast } from '@/lib/toast';
import { pick } from '@/lib/util';

type Dlg = { kind: 'edit'; e: Etablissement | null } | { kind: 'delete'; e: Etablissement } | null;

/** Everybody reads the établissements of their perimeter; only the ADMIN can add, change, archive or delete them. */
export default function EtablissementsPage() {
  const { me } = useAuth();
  const { t, lang, err } = useI18n();
  const toast = useToast();
  const [ref, setRef] = useState<RefData | null>(null);
  const [assos, setAssos] = useState<Association[]>([]);
  const [list, setList] = useState<Etablissement[] | null>(null);
  const [q, setQ] = useState('');
  const [prov, setProv] = useState('');
  const [cat, setCat] = useState('');
  const [asso, setAsso] = useState('');
  const [archived, setArchived] = useState(false);
  const [dlg, setDlg] = useState<Dlg>(null);
  const [error, setError] = useState('');

  useEffect(() => { api<RefData>('/ref').then(setRef); api<Association[]>('/associations').then(setAssos); }, []);
  const load = useCallback(() => {
    const p = new URLSearchParams();
    if (q.trim()) p.set('q', q.trim());
    if (prov) p.set('provinceId', prov);
    if (cat) p.set('categoryId', cat);
    if (asso) p.set('associationId', asso);
    if (archived) p.set('includeArchived', 'true');
    api<Etablissement[]>('/etablissements?' + p).then(setList).catch((e) => setError(err(e instanceof ApiError ? e.code : 'INTERNAL')));
  }, [q, prov, cat, asso, archived, err]);
  useEffect(() => { const h = setTimeout(load, 200); return () => clearTimeout(h); }, [load]);

  const assocOptions = useMemo(() => [{ value: '', label: t('allAssociations') }, ...assos.map((a) => ({ value: String(a.id), label: a.nomAr }))], [assos, t]);

  async function setActive(e: Etablissement, active: boolean) {
    try { await api(`/etablissements/${e.id}/${active ? 'unarchive' : 'archive'}`, { method: 'POST' }); toast(active ? t('etabUnarchived') : t('etabArchived')); load(); }
    catch (ex) { setError(err(ex instanceof ApiError ? ex.code : 'INTERNAL')); }
  }

  if (!me || !ref) return <div className="center"><div className="spinner" /></div>;
  const admin = me.role === 'ADMIN';
  const catName = (id: number) => { const c = ref.categories.find((x) => x.id === id); return c ? pick(lang, c) : ''; };

  return (
    <>
      <div className="top">
        <div><h1>{t('etabTitle')}</h1><p className="sub">{t('etabSub')}</p></div>
        {admin && <button className="btn primary" type="button" onClick={() => setDlg({ kind: 'edit', e: null })}><Icon name="form" size={16} />{t('etabAdd')}</button>}
      </div>
      <div className="card">
        <div className="filters">
          <div className="field"><Icon name="search" /><input type="text" placeholder={t('etabName')} value={q} onChange={(e) => setQ(e.target.value)} aria-label={t('etabName')} dir="auto" /></div>
          {ref.provinces.length > 1 && (
            <select value={prov} onChange={(e) => setProv(e.target.value)} aria-label={t('allProvinces')}>
              <option value="">{t('allProvinces')}</option>
              {ref.provinces.map((p) => <option key={p.id} value={p.id}>{pick(lang, p)}</option>)}
            </select>
          )}
          <select value={cat} onChange={(e) => setCat(e.target.value)} aria-label={t('allCategories')}>
            <option value="">{t('allCategories')}</option>
            {ref.categories.map((c) => <option key={c.id} value={c.id}>{pick(lang, c)}</option>)}
          </select>
          <div style={{ flex: '1 1 220px', minWidth: 0 }}><SearchSelect id="filterAssociation" options={assocOptions} value={asso} onChange={setAsso} placeholder={t('allAssociations')} /></div>
          {admin && <label className="check-row"><input type="checkbox" checked={archived} onChange={(e) => setArchived(e.target.checked)} />{t('etabShowArchived')}</label>}
        </div>
        {!admin && <p className="ro-note" style={{ marginTop: 12 }}><Icon name="eye" size={16} />{t('readOnlyHint')}</p>}
        {error && <div className="alert bad" style={{ marginTop: 12 }} role="alert">{error}</div>}
        <div className="tablewrap">
          <table>
            <thead><tr><th>{t('colEtablissement')}</th><th>{t('colAssociation')}</th><th>{t('colProvince')}</th><th>{t('colCategories')}</th><th>{t('colType')}</th><th>{t('colLicense')}</th><th>{t('colBeneficiaries')}</th>{admin && <th />}</tr></thead>
            <tbody>
              {list?.map((e) => (
                <tr key={e.id}>
                  <td><div className="person"><span className="chip c-brand" style={{ width: 36, height: 36 }}><Icon name="etab" /></span><div><b className="ar" style={{ color: 'var(--ink)', fontSize: 14 }}>{e.nomAr}</b>{!e.actif && <div><span className="pill off">{t('etabArchivedTag')}</span></div>}</div></div></td>
                  <td className="ar" style={{ color: 'var(--ink)' }}>{e.associationNom}</td>
                  <td>{pick(lang, { nomFr: e.provinceNomFr, nomAr: e.provinceNomAr })}</td>
                  <td><span className="pill cat">{catName(e.categoryId)}</span></td>
                  <td className="ar" style={{ color: 'var(--ink)' }}>{e.typeEtablissement ?? '–'}</td>
                  <td dir="ltr" style={{ textAlign: 'start' }}>{e.numeroLicence ?? '–'}</td>
                  <td><span className="pill ok">{e.beneficiaryCount}</span></td>
                  {admin && <td><div className="row-actions">
                    {!e.actif && <button className="btn sm" type="button" onClick={() => setActive(e, true)}>{t('unarchive')}</button>}
                    <button className="btn sm" type="button" onClick={() => setDlg({ kind: 'edit', e })}><Icon name="edit" size={14} />{t('edit')}</button>
                    <button className="btn sm danger" type="button" onClick={() => setDlg({ kind: 'delete', e })}><Icon name="trash" size={14} />{t('delete')}</button>
                  </div></td>}
                </tr>
              ))}
              {list && list.length === 0 && <tr><td colSpan={8} className="muted">{t('empty')}</td></tr>}
            </tbody>
          </table>
        </div>
        <div className="pager"><span>{list?.length ?? 0} {t('results')}</span></div>
      </div>
      {dlg?.kind === 'edit' && <EditDialog e={dlg.e} ref_={ref} assocOptions={assocOptions.slice(1)} onClose={() => setDlg(null)} onSaved={() => { setDlg(null); toast(t('etabSaved')); load(); }} />}
      {dlg?.kind === 'delete' && <DeleteDialog e={dlg.e} onClose={() => setDlg(null)} onDone={(m) => { setDlg(null); toast(m); load(); }} />}
    </>
  );
}

function EditDialog({ e, ref_, assocOptions, onClose, onSaved }: { e: Etablissement | null; ref_: RefData; assocOptions: { value: string; label: string }[]; onClose: () => void; onSaved: () => void }) {
  const { t, lang, err } = useI18n();
  const [assoc, setAssoc] = useState(e ? String(e.associationId) : '');
  const [provinceId, setProvinceId] = useState(String(e?.provinceId ?? ref_.provinces[0]?.id ?? ''));
  const [catId, setCatId] = useState<number | null>(e?.categoryId ?? null);
  const [nom, setNom] = useState(e?.nomAr ?? '');
  const [type, setType] = useState(e?.typeEtablissement ?? '');
  const [lic, setLic] = useState(e?.numeroLicence ?? '');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  async function submit(ev: FormEvent) {
    ev.preventDefault();
    if (!assoc) { setError(err('ASSOCIATION_NOT_FOUND')); return; }
    if (catId === null) { setError(err('INVALID_CATEGORIES')); return; }
    setBusy(true); setError('');
    const body = { associationId: Number(assoc), provinceId: Number(provinceId), categoryId: catId, nomAr: nom, typeEtablissement: type || null, numeroLicence: lic || null };
    try {
      if (e) await api('/etablissements/' + e.id, { method: 'PUT', body }); else await api('/etablissements', { method: 'POST', body });
      onSaved();
    } catch (ex) { setError(err(ex instanceof ApiError ? ex.code : 'INTERNAL')); }
    finally { setBusy(false); }
  }

  return (
    <Modal title={e ? t('etabEdit') : t('etabAdd')} onClose={onClose}>
      <form onSubmit={submit} style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
        <label><span>{t('fAssociation')}<span className="req" aria-hidden="true">*</span></span><SearchSelect id="etabAssociation" options={assocOptions} value={assoc} onChange={setAssoc} placeholder={t('chooseAssociation')} /></label>
        <label><span>{t('etabName')}<span className="req" aria-hidden="true">*</span></span><input id="etabName" type="text" dir="rtl" required maxLength={255} value={nom} onChange={(ev) => setNom(ev.target.value)} /></label>
        <label><span>{t('colProvince')}<span className="req" aria-hidden="true">*</span></span>
          <select id="etabProvince" value={provinceId} onChange={(ev) => setProvinceId(ev.target.value)}>
            {ref_.provinces.map((p) => <option key={p.id} value={p.id}>{pick(lang, p)}</option>)}
          </select>
        </label>
        <fieldset className="catset">
          <legend>{t('assocCategories')}<span className="req" aria-hidden="true">*</span> <small>({t('assocCategoriesHint')})</small></legend>
          {ref_.categories.map((c) => (
            <label key={c.id} className="cb radio"><input type="radio" name="cat" checked={catId === c.id} onChange={() => setCatId(c.id)} /><span>{pick(lang, c)}</span></label>
          ))}
        </fieldset>
        <label>{t('etabType')}<input id="etabType" type="text" dir="rtl" maxLength={120} value={type} onChange={(ev) => setType(ev.target.value)} /></label>
        <label>{t('etabLicense')}<input id="etabLicense" type="text" dir="ltr" maxLength={40} value={lic} onChange={(ev) => setLic(ev.target.value)} /></label>
        {error && <div className="alert bad" role="alert">{error}</div>}
        <div className="actions" style={{ marginTop: 0 }}>
          <button className="btn" type="button" onClick={onClose}>{t('cancel')}</button>
          <button className="btn primary" id="etabSave" type="submit" disabled={busy}>{t('save')}</button>
        </div>
      </form>
    </Modal>
  );
}

function DeleteDialog({ e, onClose, onDone }: { e: Etablissement; onClose: () => void; onDone: (msg: string) => void }) {
  const { t, err } = useI18n();
  const [error, setError] = useState('');
  const blocked = e.beneficiaryCount > 0;
  async function del() {
    try { await api('/etablissements/' + e.id, { method: 'DELETE' }); onDone(t('etabDeleted')); }
    catch (ex) { setError(err(ex instanceof ApiError ? ex.code : 'INTERNAL')); }
  }
  async function archive() {
    try { await api(`/etablissements/${e.id}/archive`, { method: 'POST' }); onDone(t('etabArchived')); }
    catch (ex) { setError(err(ex instanceof ApiError ? ex.code : 'INTERNAL')); }
  }
  return (
    <Modal title={t('etabDeleteTitle')} onClose={onClose}>
      <p><b className="ar" style={{ color: 'var(--ink)', fontSize: 14 }}>{e.nomAr}</b></p>
      <p className="muted">{blocked ? t('etabDeleteBlocked', { n: e.beneficiaryCount }) : t('etabDeleteOk')}</p>
      {error && <div className="alert bad" role="alert">{error}</div>}
      <div className="actions" style={{ marginTop: 0 }}>
        <button className="btn" type="button" onClick={onClose}>{t('cancel')}</button>
        {blocked ? (e.actif && <button className="btn primary" type="button" onClick={archive}>{t('archive')}</button>) : <button className="btn danger" type="button" onClick={del}>{t('delete')}</button>}
      </div>
    </Modal>
  );
}
