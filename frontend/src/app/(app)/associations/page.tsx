'use client';
import { FormEvent, useCallback, useEffect, useState } from 'react';
import { api, ApiError, Association, RefData } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { useI18n } from '@/lib/i18n';
import { Icon } from '@/lib/icons';
import { Modal } from '@/components/Modal';
import { useToast } from '@/lib/toast';
import { pick } from '@/lib/util';
import { Pager } from '@/components/Pager';
import { usePaged } from '@/lib/paging';

type Dlg = { kind: 'edit'; a: Association | null } | { kind: 'delete'; a: Association } | null;

/** Everybody reads the associations of their perimeter; only the ADMIN can add, change or delete them. */
export default function AssociationsPage() {
  const { me } = useAuth();
  const { t, lang, err } = useI18n();
  const toast = useToast();
  const [ref, setRef] = useState<RefData | null>(null);
  const [list, setList] = useState<Association[] | null>(null);
  const [q, setQ] = useState('');
  const [prov, setProv] = useState('');
  const [cat, setCat] = useState('');
  const [dlg, setDlg] = useState<Dlg>(null);
  const [error, setError] = useState('');

  const load = useCallback(() => {
    const p = new URLSearchParams();
    if (q.trim()) p.set('q', q.trim());
    if (prov) p.set('provinceId', prov);
    if (cat) p.set('categoryId', cat);
    api<Association[]>('/associations?' + p).then(setList).catch((e) => setError(err(e instanceof ApiError ? e.code : 'INTERNAL')));
  }, [q, prov, cat, err]);
  const pg = usePaged(list, [q, prov, cat].join('|'));
  useEffect(() => { api<RefData>('/ref').then(setRef); }, []);
  useEffect(() => { const h = setTimeout(load, 200); return () => clearTimeout(h); }, [load]);

  if (!me || !ref) return <div className="center"><div className="spinner" /></div>;
  const admin = me.role === 'ADMIN';

  return (
    <>
      <div className="top">
        <div><h1>{t('assocTitle')}</h1><p className="sub">{t('assocSub')}</p></div>
        {admin && <button className="btn primary" type="button" onClick={() => setDlg({ kind: 'edit', a: null })}><Icon name="form" size={16} />{t('assocAdd')}</button>}
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
          {(q || prov || cat) && <button className="linkbtn" type="button" onClick={() => { setQ(''); setProv(''); setCat(''); }}>{t('resetFilters')}</button>}
          {!admin && <span className="ro-note"><Icon name="eye" size={16} />{t('readOnlyHint')}</span>}
        </div>
        {error && <div className="alert bad" style={{ marginTop: 12 }} role="alert">{error}</div>}
        <div className="tablewrap">
          <table>
            <thead><tr><th>{t('assocName')}</th><th>{t('colProvince')}</th><th>{t('colEtabs')}</th><th>{t('colBeneficiaries')}</th>{admin && <th />}</tr></thead>
            <tbody>
              {pg.rows?.map((a) => (
                <tr key={a.id}>
                  <td><div className="person"><span className="chip c-brand" style={{ width: 36, height: 36 }}><Icon name="asso" /></span><b className="ar" style={{ color: 'var(--ink)', fontSize: 14 }}>{a.nomAr}</b></div></td>
                  <td>{pick(lang, { nomFr: a.provinceNomFr, nomAr: a.provinceNomAr })}</td>
                  <td><span className="pill">{a.etablissementCount}</span></td>
                  <td><span className="pill ok">{a.beneficiaryCount}</span></td>
                  {admin && <td><div className="row-actions">
                    <button className="btn sm" type="button" onClick={() => setDlg({ kind: 'edit', a })}><Icon name="edit" size={14} />{t('edit')}</button>
                    <button className="btn sm danger" type="button" onClick={() => setDlg({ kind: 'delete', a })}><Icon name="trash" size={14} />{t('delete')}</button>
                  </div></td>}
                </tr>
              ))}
              {list && list.length === 0 && <tr><td colSpan={5} className="muted">{t('empty')}</td></tr>}
            </tbody>
          </table>
        </div>
        <Pager total={pg.total} page={pg.page} size={pg.size} onPage={pg.setPage} onSize={pg.setSize} />
      </div>
      {dlg?.kind === 'edit' && <EditDialog a={dlg.a} ref_={ref} onClose={() => setDlg(null)} onSaved={() => { setDlg(null); toast(t('assocSaved')); load(); }} />}
      {dlg?.kind === 'delete' && <DeleteDialog a={dlg.a} onClose={() => setDlg(null)} onDone={() => { setDlg(null); toast(t('assocDeleted')); load(); }} />}
    </>
  );
}

function EditDialog({ a, ref_, onClose, onSaved }: { a: Association | null; ref_: RefData; onClose: () => void; onSaved: () => void }) {
  const { t, lang, err } = useI18n();
  const [nom, setNom] = useState(a?.nomAr ?? '');
  const [provinceId, setProvinceId] = useState(String(a?.provinceId ?? ref_.provinces[0]?.id ?? ''));
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true); setError('');
    try {
      if (a) await api('/associations/' + a.id, { method: 'PUT', body: { nomAr: nom, provinceId: Number(provinceId) } }); else await api('/associations', { method: 'POST', body: { nomAr: nom, provinceId: Number(provinceId) } });
      onSaved();
    } catch (ex) { setError(err(ex instanceof ApiError ? ex.code : 'INTERNAL')); }
    finally { setBusy(false); }
  }
  return (
    <Modal title={a ? t('assocEdit') : t('assocAdd')} onClose={onClose}>
      <form onSubmit={submit} style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
        <label>{t('colProvince')}
          <select id="assoProvince" value={provinceId} onChange={(e) => setProvinceId(e.target.value)}>
            {ref_.provinces.map((p) => <option key={p.id} value={p.id}>{pick(lang, p)}</option>)}
          </select>
        </label>
        <label>{t('assocName')}<input id="assoName" type="text" dir="rtl" required maxLength={255} value={nom} onChange={(e) => setNom(e.target.value)} autoFocus /></label>
        {error && <div className="alert bad" role="alert">{error}</div>}
        <div className="actions" style={{ marginTop: 0 }}>
          <button className="btn" type="button" onClick={onClose}>{t('cancel')}</button>
          <button className="btn primary" id="assoSave" type="submit" disabled={busy}>{t('save')}</button>
        </div>
      </form>
    </Modal>
  );
}

function DeleteDialog({ a, onClose, onDone }: { a: Association; onClose: () => void; onDone: () => void }) {
  const { t, err } = useI18n();
  const [error, setError] = useState('');
  const blocked = a.etablissementCount > 0;
  async function del() {
    try { await api('/associations/' + a.id, { method: 'DELETE' }); onDone(); }
    catch (ex) { setError(err(ex instanceof ApiError ? ex.code : 'INTERNAL')); }
  }
  return (
    <Modal title={t('assocDeleteTitle')} onClose={onClose}>
      <p><b className="ar" style={{ color: 'var(--ink)', fontSize: 14 }}>{a.nomAr}</b></p>
      <p className="muted">{blocked ? t('assocDeleteHasEtabs', { n: a.etablissementCount }) : t('assocDeleteOk')}</p>
      {error && <div className="alert bad" role="alert">{error}</div>}
      <div className="actions" style={{ marginTop: 0 }}>
        <button className="btn" type="button" onClick={onClose}>{t('cancel')}</button>
        {!blocked && <button className="btn danger" type="button" onClick={del}>{t('delete')}</button>}
      </div>
    </Modal>
  );
}
