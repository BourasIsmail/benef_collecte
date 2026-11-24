'use client';
import { ReactNode, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/lib/auth';
import { Shell } from '@/components/Shell';

export default function AppLayout({ children }: { children: ReactNode }) {
  const { me, loading } = useAuth();
  const router = useRouter();
  useEffect(() => {
    if (loading) return;
    if (!me) router.replace('/login');
    else if (me.mustChangePassword) router.replace('/change-password');
  }, [me, loading, router]);
  if (loading || !me || me.mustChangePassword) return <div className="center"><div className="spinner" aria-label="loading" /></div>;
  return <Shell>{children}</Shell>;
}
