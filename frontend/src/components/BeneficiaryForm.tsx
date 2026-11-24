'use client';
import { FormEvent, useEffect, useMemo, useRef, useState } from 'react';
import { useRouter } from 'next/navigation';
import { api, ApiError, Association, Beneficiary, CheckResult, Match, RefData } from '@/lib/api';
import { useI18n } from '@/lib/i18n';
import { Icon } from '@/lib/icons';
import { useToast } from '@/lib/toast';
import { fmtDate, pick, todayIso } from '@/lib/util';

interface Form { associationId: string; nomFr: string; prenomFr: string; nomAr: string; prenomAr: string; dateNaissance: string; sexe: '' | 'F' | 'M'; cin: string }
const empty: Form = { associationId: '', nomFr: '', prenomFr: '', nomAr: '', prenomAr: '', dateNaissance: '', sexe: '', cin: '' };

/** Red star = required field. Decorative: the inputs carry the real `required` attribute. */
function Req() { return <span className="req" aria-hidden="true">*</span>; }

export function BeneficiaryForm({ existing }: { existing?: Beneficiary }) {
  const { t, lang, err } = useI18n();
  const toast = useToast();
  const router = useRouter();
  const [ref, setRef] = useState<RefData | null>(null);
  const [assos, setAssos] = useState<Association[]>([]);
  const [f, setF] = useState<Form>(existing ? {
    associationId: String(existing.associationId), nomFr: existing.nomFr, prenomFr: existing.prenomFr, nomAr: existing.nomAr,
    prenomAr: existing.prenomAr, dateNaissance: existing.dateNaissance, sexe: existing.sexe, cin: existing.cin ?? '',
  } : empty);
  const [check, setCheck] = useState<CheckResult | null>(null);
  const [checking, setChecking] = useState(false);
  const [confirm, setConfirm] = useState(false);
  const [error, setError] = useState('');
  const [fields, setFields] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState(false);
  const seq = useRef(0);

  useEffect(() => { api<RefData>('/ref').then(setRef); api<Association[]>('/associations').then((a) => { setAssos(a); setF((cur) => (cur.associationId || existing ? cur : { ...cur, associationId: a[0] ? String(a[0].id) : '' })); }); }, [existing]);

  const ready = f.nomFr.trim() && f.prenomFr.trim() && f.dateNaissance.length === 10;
  useEffect(() => {
    setConfirm(false);
    if (!ready) { setCheck(null); return; }
    const my = ++seq.current;
    setChecking(true);
    const h = setTimeout(() => {
      api<CheckResult>('/beneficiaries/check', { method: 'POST', body: { nomFr: f.nomFr, prenomFr: f.prenomFr, dateNaissance: f.dateNaissance, excludeId: existing?.id } })
        .then((r) => { if (my === seq.current) setCheck(r); })
        .catch(() => { if (my === seq.current) setCheck(null); })
        .finally(() => { if (my === seq.current) setChecking(false); });
    }, 350);
    return () => clearTimeout(h);
  }, [f.nomFr, f.prenomFr, f.dateNaissance, ready, existing?.id]);

  const set = (k: keyof Form) => (e: { target: { value: string } }) => setF((c) => ({ ...c, [k]: e.target.value } as Form));
  const blocked = !!check && check.exact.length > 0 && !confirm;
  const complete = f.associationId && f.nomFr.trim() && f.prenomFr.trim() && f.nomAr.trim() && f.prenomAr.trim() && f.dateNaissance && f.sexe;

  const selected = assos.find((a) => String(a.id) === f.associationId);
  const catNames = useMemo(() => { const c = selected?.categoryId && ref ? ref.categories.find((x) => x.id === selected.categoryId) : undefined; return c ? [pick(lang, c)] : []; }, [selected, ref, lang]);
  const grouped = useMemo(() => {
    const m = new Map<number, { name: string; items: Association[] }>();
    for (const a of assos) { if (!m.has(a.provinceId)) m.set(a.provinceId, { name: pick(lang, { nomFr: a.provinceNomFr, nomAr: a.provinceNomAr }), items: [] }); m.get(a.provinceId)!.items.push(a); }
    return [...m.values()];
  }, [assos, lang]);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setError(''); setFields({}); setBusy(true);
    const body = { ...f, associationId: Number(f.associationId), cin: f.cin.trim() || null, confirmDuplicate: confirm };
    try {
      if (existing) await api('/beneficiaries/' + existing.id, { method: 'PUT', body }); else await api('/beneficiaries', { method: 'POST', body });
      toast(t('saved'));
      router.push('/beneficiaries');
    } catch (ex) {
      if (ex instanceof ApiError) {
        if (ex.code === 'DUPLICATE') setCheck(ex.data);
        else if (ex.code === 'VALIDATION') { setFields(ex.data ?? {}); setError(err(ex.code)); }
        else setError(err(ex.code));
      } else setError(err('INTERNAL'));
    } finally { setBusy(false); }
  }

  const matchCard = (m: Match, k: number) => (
    <div className="m" key={k}>
      {m.nomFr ? <><b>{m.nomFr} {m.prenomFr}</b><span>{fmtDate(m.dateNaissance)}{m.visible && <> · <span className="ar">{m.associationNom}</span> · {pick(lang, { nomFr: m.provinceNomFr, nomAr: m.provinceNomAr })}</>}</span></> : null}
      {!m.visible && <span>{t('dupHidden')}</span>}
    </div>
  );

  return (
    <form className="form" onSubmit={submit}>
      <div className="card">
        <div className="fields">
          <div className="sect"><Icon name="asso" size={16} />{t('secAssociation')}</div>
          <label className="full"><span>{t('fAssociation')}<Req /></span>
            <select id="association" required value={f.associationId} onChange={set('associationId')}>
              {grouped.map((g) => <optgroup key={g.name} label={g.name}>{g.items.map((a) => <option key={a.id} value={a.id}>{a.nomAr}</option>)}</optgroup>)}
            </select>
            {catNames.length > 0 && <span className="cats" style={{ marginTop: 4 }}>{catNames.map((c) => <span key={c} className="pill cat">{c}</span>)}</span>}
          </label>
          <div className="sect"><Icon name="user" size={16} />{t('secIdentity')}</div>
          <label><span>{t('fNomFr')}<Req /></span><input id="nomFr" type="text" required maxLength={120} value={f.nomFr} onChange={set('nomFr')} autoComplete="off" />{fields.nomFr && <span className="fielderr">{fields.nomFr}</span>}</label>
          <label><span>{t('fPrenomFr')}<Req /></span><input id="prenomFr" type="text" required maxLength={120} value={f.prenomFr} onChange={set('prenomFr')} autoComplete="off" /></label>
          <label><span>{t('fNomAr')}<Req /></span><input id="nomAr" type="text" dir="rtl" required maxLength={120} value={f.nomAr} onChange={set('nomAr')} autoComplete="off" /></label>
          <label><span>{t('fPrenomAr')}<Req /></span><input id="prenomAr" type="text" dir="rtl" required maxLength={120} value={f.prenomAr} onChange={set('prenomAr')} autoComplete="off" /></label>
          <label><span>{t('fDob')}<Req /></span><input id="dob" type="date" required min="1900-01-01" max={todayIso()} value={f.dateNaissance} onChange={set('dateNaissance')} />{fields.dateNaissance && <span className="fielderr">{fields.dateNaissance}</span>}</label>
          <label><span>{t('fSex')}<Req /></span>
            <div className="seg" role="radiogroup" aria-required="true">
              <label><input type="radio" name="sexe" value="F" checked={f.sexe === 'F'} onChange={set('sexe')} />{t('F')}</label>
              <label><input type="radio" name="sexe" value="M" checked={f.sexe === 'M'} onChange={set('sexe')} />{t('M')}</label>
            </div>
          </label>
          <div className="sect"><Icon name="bolt" size={16} />{t('secMore')}</div>
          <label className="full">{t('fCin')}<input id="cin" type="text" maxLength={20} value={f.cin} onChange={set('cin')} autoComplete="off" dir="ltr" /></label>
        </div>
        {error && <div className="alert bad" style={{ marginTop: 14 }} role="alert">{error}</div>}
        <div className="actions">
          <button className="btn primary" id="save" type="submit" disabled={busy || blocked || !complete}><Icon name="check" size={16} />{t('save')}</button>
          <button className="btn" type="button" onClick={() => router.push('/beneficiaries')}>{t('cancel')}</button>
        </div>
      </div>
      <div className="card" aria-live="polite">
        <div className="head"><h2>{t('dupTitle')}</h2></div>
        {!ready && <div className="idle"><span className="chip"><Icon name="dup" size={26} /></span><span>{t('dupIdle')}</span></div>}
        {ready && !check && checking && <div className="idle"><span>{t('dupChecking')}</span></div>}
        {ready && check && (
          <>
            {check.exact.length > 0 ? (
              <div className="chk bad">
                <div className="hd"><Icon name="alert" size={24} />{t('dupExact')}</div>
                {check.exact.map(matchCard)}
                <label className="cb"><input id="confirmDup" type="checkbox" checked={confirm} onChange={(e) => setConfirm(e.target.checked)} /><span>{t('dupConfirm')}</span></label>
              </div>
            ) : <div className="chk ok"><div className="hd"><Icon name="check" size={24} />{t('dupOk')}</div></div>}
            {check.similar.length > 0 && (
              <div className="chk warn" style={{ marginTop: 12 }}>
                <div className="hd"><Icon name="alert" size={22} />{t('dupSimilar')}</div>
                {check.similar.map((s, k) => <div key={k}>{matchCard(s.match, k)}<span style={{ fontSize: 12.5 }}>→ {t(s.reason)}</span></div>)}
              </div>
            )}
            <div className="keyline hint" style={{ marginTop: 12 }}>{t('dupKey')} : <code>{check.key}</code></div>
          </>
        )}
      </div>
    </form>
  );
}
