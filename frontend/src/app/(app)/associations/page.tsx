'use client';
import { FormEvent, useCallback, useEffect, useState } from 'react';
import { api, ApiError, Association, RefData } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { useI18n } from '@/lib/i18n';
import { Icon } from '@/lib/icons';
import { Modal } from '@/components/Modal';
import { useToast } from '@/lib/toast';
import { canWrite, pick } from '@/lib/util';

type Dlg = { kind: 'edit'; a: Association | null } | { kind: 'delete'; a: Association } | null;

export default function AssociationsPage() {
  const { me } = useAuth();
  const { t, lang, err } = useI18n();
  const toast = useToast();
  const [ref, setRef] = useState<RefData | null>(null);
  const [list, setList] = useState<Association[] | null>(null);
  const [cat, setCat] = useState('');
  const [prov, setProv] = useState('');
  const [q, setQ] = useState('');
  const [archived, setArchived] = useState(false);
  const [dlg, setDlg] = useState<Dlg>(null);
  const [error, setError] = useState('');

  const load = useCallback(() => {
    const p = new URLSearchParams();
    if (cat) p.set('categoryId', cat);
    if (prov) p.set('provinceId', prov);
    if (q.trim()) p.set('q', q.trim());
    if (archived) p.set('includeArchived', 'true');
    api<Association[]>('/associations?' + p).then(setList).catch((e) => setError(err(e instanceof ApiError ? e.code : 'INTERNAL')));
  }, [cat, prov, q, archived, err]);
  useEffect(() => { api<RefData>('/ref').then(setRef); }, []);
  useEffect(() => { const h = setTimeout(load, 200); return () => clearTimeout(h); }, [load]);

  if (!me || !ref) return <div className="center"><div className="spinner" /></div>;
  const write = canWrite(me);
  const catName = (id: number) => { const c = ref.categories.find((x) => x.id === id); return c ? pick(lang, c) : ''; };
  const provName = (a: Association) => pick(lang, { nomFr: a.provinceNomFr, nomAr: a.provinceNomAr });

  return (
    <>
      <div className="top">
        <div><h1>{t('assocTitle')}</h1><p className="sub">{t('assocSub')}</p></div>
        {write && <button className="btn primary" type="button" onClick={() => setDlg({ kind: 'edit', a: null })}><Icon name="form" size={16} />{t('assocAdd')}</button>}
      </div>
      <div className="card">
        <div className="filters">
          <div className="field"><Icon name="search" /><input type="text" placeholder={t('assocName')} value={q} onChange={(e) => setQ(e.target.value)} aria-label={t('assocName')} dir="auto" /></div>
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
          <label className="check-row"><input type="checkbox" checked={archived} onChange={(e) => setArchived(e.target.checked)} />{t('assocShowArchived')}</label>
        </div>
        {error && <div className="alert bad" style={{ marginTop: 12 }} role="alert">{error}</div>}
        <div className="tablewrap">
          <table>
            <thead><tr><th>{t('assocName')}</th><th>{t('colProvince')}</th><th>{t('colCategories')}</th><th>{t('colBeneficiaries')}</th>{write && <th />}</tr></thead>
            <tbody>
              {list?.map((a) => (
                <tr key={a.id}>
                  <td><div className="person"><span className="chip c-brand" style={{ width: 36, height: 36 }}><Icon name="asso" /></span><div><b className="ar" style={{ color: 'var(--ink)', fontSize: 14 }}>{a.nomAr}</b>{!a.actif && <div><span className="pill off">{t('assocArchivedTag')}</span></div>}</div></div></td>
                  <td>{provName(a)}</td>
                  <td><div className="cats">{a.categoryIds.length ? a.categoryIds.map((id) => <span key={id} className="pill cat">{catName(id)}</span>) : <span className="muted">–</span>}</div></td>
                  <td><span className="pill ok">{a.beneficiaryCount}</span></td>
                  {write && <td><div className="row-actions">
                    <button className="btn sm" type="button" onClick={() => setDlg({ kind: 'edit', a })}><Icon name="edit" size={14} />{t('edit')}</button>
                    <button className="btn sm danger" type="button" onClick={() => setDlg({ kind: 'delete', a })}><Icon name="trash" size={14} />{t('delete')}</button>
                  </div></td>}
                </tr>
              ))}
              {list && list.length === 0 && <tr><td colSpan={5} className="muted">{t('empty')}</td></tr>}
            </tbody>
          </table>
        </div>
        <div className="pager"><span>{list?.length ?? 0} {t('results')}</span></div>
      </div>
      {dlg?.kind === 'edit' && <EditDialog a={dlg.a} ref_={ref} onClose={() => setDlg(null)} onSaved={() => { setDlg(null); toast(t('assocSaved')); load(); }} />}
      {dlg?.kind === 'delete' && <DeleteDialog a={dlg.a} onClose={() => setDlg(null)} onDone={(msg) => { setDlg(null); toast(msg); load(); }} />}
    </>
  );
}

function EditDialog({ a, ref_, onClose, onSaved }: { a: Association | null; ref_: RefData; onClose: () => void; onSaved: () => void }) {
  const { me } = useAuth();
  const { t, lang, err } = useI18n();
  const [nom, setNom] = useState(a?.nomAr ?? '');
  const [provinceId, setProvinceId] = useState(String(a?.provinceId ?? (me?.role === 'PROVINCE' ? me.provinceId : ref_.provinces[0]?.id) ?? ''));
  const [cats, setCats] = useState<number[]>(a?.categoryIds ?? []);
  const [actif, setActif] = useState(a?.actif ?? true);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const toggle = (id: number) => setCats((c) => (c.includes(id) ? c.filter((x) => x !== id) : [...c, id]));

  async function submit(e: FormEvent) {
    e.preventDefault();
    if (!nom.trim()) return;
    if (cats.length === 0) { setError(err('INVALID_CATEGORIES')); return; }
    setBusy(true); setError('');
    const body = { nomAr: nom, provinceId: Number(provinceId), categoryIds: cats, actif };
    try {
      if (a) await api('/associations/' + a.id, { method: 'PUT', body }); else await api('/associations', { method: 'POST', body });
      onSaved();
    } catch (ex) { setError(err(ex instanceof ApiError ? ex.code : 'INTERNAL')); }
    finally { setBusy(false); }
  }

  return (
    <Modal title={a ? t('assocEdit') : t('assocAdd')} onClose={onClose}>
      <form onSubmit={submit} style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
        <label>{t('assocName')}<input id="assoName" type="text" dir="rtl" required maxLength={255} value={nom} onChange={(e) => setNom(e.target.value)} autoFocus /></label>
        {me?.role !== 'PROVINCE' && (
          <label>{t('colProvince')}
            <select id="assoProvince" value={provinceId} onChange={(e) => setProvinceId(e.target.value)}>
              {ref_.provinces.map((p) => <option key={p.id} value={p.id}>{pick(lang, p)}</option>)}
            </select>
          </label>
        )}
        <fieldset className="catset">
          <legend>{t('assocCategories')} <small>({t('assocCategoriesHint')})</small></legend>
          {ref_.categories.map((c) => (
            <label key={c.id} className="cb"><input type="checkbox" name="cat" checked={cats.includes(c.id)} onChange={() => toggle(c.id)} /><span>{pick(lang, c)}</span></label>
          ))}
        </fieldset>
        {a && <label className="check-row"><input type="checkbox" checked={actif} onChange={(e) => setActif(e.target.checked)} />{t('assocActive')}</label>}
        {error && <div className="alert bad" role="alert">{error}</div>}
        <div className="actions" style={{ marginTop: 0 }}>
          <button className="btn" type="button" onClick={onClose}>{t('cancel')}</button>
          <button className="btn primary" id="assoSave" type="submit" disabled={busy}>{t('save')}</button>
        </div>
      </form>
    </Modal>
  );
}

function DeleteDialog({ a, onClose, onDone }: { a: Association; onClose: () => void; onDone: (msg: string) => void }) {
  const { t, err } = useI18n();
  const [error, setError] = useState('');
  const blocked = a.beneficiaryCount > 0;
  async function del() {
    try { await api('/associations/' + a.id, { method: 'DELETE' }); onDone(t('assocDeleted')); }
    catch (ex) { setError(err(ex instanceof ApiError ? ex.code : 'INTERNAL')); }
  }
  async function archive() {
    try { await api('/associations/' + a.id, { method: 'PUT', body: { nomAr: a.nomAr, provinceId: a.provinceId, categoryIds: a.categoryIds, actif: false } }); onDone(t('assocSaved')); }
    catch (ex) { setError(err(ex instanceof ApiError ? ex.code : 'INTERNAL')); }
  }
  return (
    <Modal title={t('assocDeleteTitle')} onClose={onClose}>
      <p><b className="ar" style={{ color: 'var(--ink)', fontSize: 14 }}>{a.nomAr}</b></p>
      <p className="muted">{blocked ? t('assocDeleteBlocked', { n: a.beneficiaryCount }) : t('assocDeleteOk')}</p>
      {error && <div className="alert bad" role="alert">{error}</div>}
      <div className="actions" style={{ marginTop: 0 }}>
        <button className="btn" type="button" onClick={onClose}>{t('cancel')}</button>
        {blocked
          ? (a.actif && <button className="btn primary" type="button" onClick={archive}>{t('archive')}</button>)
          : <button className="btn danger" type="button" onClick={del}>{t('delete')}</button>}
      </div>
    </Modal>
  );
}
