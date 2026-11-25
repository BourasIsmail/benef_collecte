'use client';
import { FormEvent, useCallback, useEffect, useState } from 'react';
import { api, ApiError, Association } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { useI18n } from '@/lib/i18n';
import { Icon } from '@/lib/icons';
import { Modal } from '@/components/Modal';
import { useToast } from '@/lib/toast';

type Dlg = { kind: 'edit'; a: Association | null } | { kind: 'delete'; a: Association } | null;

/** Everybody reads the associations of their perimeter; only the ADMIN can add, change or delete them. */
export default function AssociationsPage() {
  const { me } = useAuth();
  const { t, err } = useI18n();
  const toast = useToast();
  const [list, setList] = useState<Association[] | null>(null);
  const [q, setQ] = useState('');
  const [dlg, setDlg] = useState<Dlg>(null);
  const [error, setError] = useState('');

  const load = useCallback(() => {
    const p = new URLSearchParams();
    if (q.trim()) p.set('q', q.trim());
    api<Association[]>('/associations?' + p).then(setList).catch((e) => setError(err(e instanceof ApiError ? e.code : 'INTERNAL')));
  }, [q, err]);
  useEffect(() => { const h = setTimeout(load, 200); return () => clearTimeout(h); }, [load]);

  if (!me) return null;
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
          {!admin && <span className="ro-note"><Icon name="eye" size={16} />{t('readOnlyHint')}</span>}
        </div>
        {error && <div className="alert bad" style={{ marginTop: 12 }} role="alert">{error}</div>}
        <div className="tablewrap">
          <table>
            <thead><tr><th>{t('assocName')}</th><th>{t('colEtabs')}</th><th>{t('colBeneficiaries')}</th>{admin && <th />}</tr></thead>
            <tbody>
              {list?.map((a) => (
                <tr key={a.id}>
                  <td><div className="person"><span className="chip c-brand" style={{ width: 36, height: 36 }}><Icon name="asso" /></span><b className="ar" style={{ color: 'var(--ink)', fontSize: 14 }}>{a.nomAr}</b></div></td>
                  <td><span className="pill">{a.etablissementCount}</span></td>
                  <td><span className="pill ok">{a.beneficiaryCount}</span></td>
                  {admin && <td><div className="row-actions">
                    <button className="btn sm" type="button" onClick={() => setDlg({ kind: 'edit', a })}><Icon name="edit" size={14} />{t('edit')}</button>
                    <button className="btn sm danger" type="button" onClick={() => setDlg({ kind: 'delete', a })}><Icon name="trash" size={14} />{t('delete')}</button>
                  </div></td>}
                </tr>
              ))}
              {list && list.length === 0 && <tr><td colSpan={4} className="muted">{t('empty')}</td></tr>}
            </tbody>
          </table>
        </div>
        <div className="pager"><span>{list?.length ?? 0} {t('results')}</span></div>
      </div>
      {dlg?.kind === 'edit' && <EditDialog a={dlg.a} onClose={() => setDlg(null)} onSaved={() => { setDlg(null); toast(t('assocSaved')); load(); }} />}
      {dlg?.kind === 'delete' && <DeleteDialog a={dlg.a} onClose={() => setDlg(null)} onDone={() => { setDlg(null); toast(t('assocDeleted')); load(); }} />}
    </>
  );
}

function EditDialog({ a, onClose, onSaved }: { a: Association | null; onClose: () => void; onSaved: () => void }) {
  const { t, err } = useI18n();
  const [nom, setNom] = useState(a?.nomAr ?? '');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true); setError('');
    try {
      if (a) await api('/associations/' + a.id, { method: 'PUT', body: { nomAr: nom } }); else await api('/associations', { method: 'POST', body: { nomAr: nom } });
      onSaved();
    } catch (ex) { setError(err(ex instanceof ApiError ? ex.code : 'INTERNAL')); }
    finally { setBusy(false); }
  }
  return (
    <Modal title={a ? t('assocEdit') : t('assocAdd')} onClose={onClose}>
      <form onSubmit={submit} style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
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
