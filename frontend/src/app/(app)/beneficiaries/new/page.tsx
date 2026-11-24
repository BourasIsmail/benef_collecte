'use client';
import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { BeneficiaryForm } from '@/components/BeneficiaryForm';
import { useAuth } from '@/lib/auth';
import { useI18n } from '@/lib/i18n';
import { canWrite } from '@/lib/util';

export default function NewBeneficiary() {
  const { t } = useI18n();
  const { me } = useAuth();
  const router = useRouter();
  useEffect(() => { if (me && !canWrite(me)) router.replace('/beneficiaries'); }, [me, router]);
  return (
    <>
      <div className="top"><div><h1>{t('formNew')}</h1><p className="sub">{t('formSub')}</p></div></div>
      <BeneficiaryForm />
    </>
  );
}
