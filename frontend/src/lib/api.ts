export class ApiError extends Error {
  constructor(public status: number, public code: string, public data?: any) { super(code); }
}

export async function api<T = any>(path: string, opts: { method?: string; body?: unknown } = {}): Promise<T> {
  const res = await fetch(`/api${path}`, {
    method: opts.method ?? 'GET',
    headers: opts.body !== undefined ? { 'Content-Type': 'application/json' } : undefined,
    body: opts.body !== undefined ? JSON.stringify(opts.body) : undefined,
    credentials: 'same-origin',
    cache: 'no-store',
  });
  if (res.ok) {
    const text = await res.text();
    return (text ? JSON.parse(text) : undefined) as T;
  }
  let code = 'INTERNAL', data: any;
  try { const j = await res.json(); code = j.code ?? code; data = j.data; } catch { /* no body */ }
  if (res.status === 401 && code !== 'INVALID_CREDENTIALS' && typeof window !== 'undefined' && !location.pathname.startsWith('/login')) {
    location.href = '/login';
  }
  if (res.status === 403 && code === 'PASSWORD_CHANGE_REQUIRED' && typeof window !== 'undefined' && !location.pathname.startsWith('/change-password')) {
    location.href = '/change-password';
  }
  throw new ApiError(res.status, code, data);
}

export type Role = 'ADMIN' | 'PROVINCE' | 'REGIONAL';
export interface Me {
  id: number; email: string; role: Role; mustChangePassword: boolean;
  provinceId?: number; provinceNomFr?: string; provinceNomAr?: string;
  regionId?: number; regionNomFr?: string; regionNomAr?: string;
}
export interface Region { id: number; code: string; nomFr: string; nomAr: string }
export interface Province { id: number; code: string; regionId: number; nomFr: string; nomAr: string }
export interface Category { id: number; code: string; nomFr: string; nomAr: string }
export interface RefData { regions: Region[]; provinces: Province[]; categories: Category[] }
export interface Association { id: number; provinceId: number; provinceNomFr: string; provinceNomAr: string; nomAr: string; etablissementCount: number; beneficiaryCount: number }
export interface Etablissement {
  id: number; associationId: number; associationNom: string; provinceId: number; provinceNomFr: string; provinceNomAr: string;
  categoryId: number; nomAr: string; typeEtablissement?: string; numeroLicence?: string; actif: boolean; beneficiaryCount: number;
}
export interface Beneficiary {
  id: number; etablissementId: number; etablissementNom: string; associationId: number; associationNom: string;
  provinceId: number; provinceNomFr: string; provinceNomAr: string;
  nomFr: string; prenomFr: string; nomAr: string; prenomAr: string; dateNaissance: string; sexe: 'F' | 'M'; cin?: string;
}
export interface Page<T> { items: T[]; total: number; page: number; size: number }
export interface Match { id?: number; nomFr?: string; prenomFr?: string; dateNaissance?: string; etablissementNom?: string; associationNom?: string; provinceNomFr?: string; provinceNomAr?: string; visible: boolean }
export interface CheckResult { key: string; exact: Match[]; similar: { match: Match; reason: string }[] }
export interface Count { label: string; labelAr: string; count: number }
export interface Stats {
  total: number; girls: number; boys: number; associations: number; etablissements: number; provinces: number;
  byProvince: Count[]; byEtablissement: Count[]; ageBands: Count[]; etablissementsByCategory: Count[];
  latest: { id: number; nomFr: string; prenomFr: string; sexe: string; etablissementNom: string }[];
}
export interface UserRow { id: number; email: string; role: Role; provinceNomFr?: string; regionNomFr?: string; enabled: boolean; mustChangePassword: boolean; lastLogin?: string }
export interface AuditRow { id: number; at: string; userEmail?: string; action: string; entity?: string; entityId?: number; details?: string }
