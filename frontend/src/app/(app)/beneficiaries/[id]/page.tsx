'use client';
import { useEffect, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { api, Beneficiary } from '@/lib/api';
import { BeneficiaryForm } from '@/components/BeneficiaryForm';
import { useAuth } from '@/lib/auth';
import { useI18n } from '@/lib/i18n';
import { canWrite } from '@/lib/util';

export default function EditBeneficiary() {
  const { t } = useI18n();
  const { me } = useAuth();
  const router = useRouter();
  const { id } = useParams<{ id: string }>();
  const [b, setB] = useState<Beneficiary | null>(null);
  useEffect(() => { api<Beneficiary>('/beneficiaries/' + id).then(setB).catch(() => router.replace('/beneficiaries')); }, [id, router]);
  useEffect(() => { if (me && !canWrite(me)) router.replace('/beneficiaries'); }, [me, router]);
  if (!b) return <div className="center"><div className="spinner" /></div>;
  return (
    <>
      <div className="top"><div><h1>{t('formEdit')}</h1><p className="sub">{b.nomFr} {b.prenomFr}</p></div></div>
      <BeneficiaryForm existing={b} />
    </>
  );
}
