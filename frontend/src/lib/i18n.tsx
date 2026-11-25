'use client';
import { createContext, useCallback, useContext, useEffect, useMemo, useState, ReactNode } from 'react';

export type Lang = 'fr' | 'ar';

const fr = {
  app: 'Suivi des bénéficiaires',
  login: 'Connexion', email: 'Adresse e-mail', password: 'Mot de passe', signin: 'Se connecter', logout: 'Déconnexion',
  loginHint: 'Utilisez le compte de votre province ou de votre région.',
  changePwdTitle: 'Choisissez votre mot de passe', changePwdIntro: 'Pour votre sécurité, vous devez remplacer le mot de passe par défaut avant de continuer.',
  currentPwd: 'Mot de passe actuel', newPwd: 'Nouveau mot de passe', confirmPwd: 'Confirmer le nouveau mot de passe', save: 'Enregistrer', cancel: 'Annuler',
  pwdRules: 'Au moins 10 caractères, avec une majuscule, une minuscule et un chiffre.', pwdMismatch: 'Les deux mots de passe ne correspondent pas.',
  nav_dashboard: 'Tableau de bord', nav_beneficiaries: 'Bénéficiaires', nav_new: 'Ajouter un bénéficiaire', nav_associations: 'Associations', nav_etablissements: 'Établissements', nav_users: 'Utilisateurs', nav_audit: 'Journal',
  role_ADMIN: 'Administrateur national', role_PROVINCE: 'Responsable de province', role_REGIONAL: 'Consultation régionale',
  readonly: 'Lecture seule', national: 'National (12 régions)', loading: 'Chargement…', yes: 'Oui', no: 'Non', all: 'Tous', actions: 'Actions', edit: 'Modifier', delete: 'Supprimer', archive: 'Archiver', close: 'Fermer',
  hello: 'Bonjour', helloSub: 'Voici l’activité de votre périmètre.', cta: 'Ajouter un bénéficiaire',
  kpi_b: 'Bénéficiaires', kpi_a: 'Associations', kpi_e: 'Établissements', kpi_p: 'Provinces', kpi_f: 'Filles',
  sexSplit: 'Répartition par sexe', girls: 'Filles', boys: 'Garçons', total: 'total',
  byProvince: 'Par province', byEtablissement: 'Par établissement', ageBands: 'Tranches d’âge', ageYears: 'ans', byCategory: 'Établissements par catégorie', latest: 'Dernières fiches', moreProvinces: 'autres provinces non affichées',
  searchPh: 'Rechercher un nom (FR ou AR)', allProvinces: 'Toutes les provinces', allAssociations: 'Toutes les associations', allSexes: 'Tous les sexes', allCategories: 'Toutes les catégories',
  colName: 'Nom et prénom', colDob: 'Naissance', colSex: 'Sexe', colAssociation: 'Association', colProvince: 'Province', colCategories: 'Catégories', colBeneficiaries: 'Bénéficiaires',
  F: 'Fille', M: 'Garçon', results: 'résultats', prev: 'Précédent', next: 'Suivant', empty: 'Aucun résultat.', exportCsv: 'Exporter (CSV)',
  listSub: 'Recherchez, filtrez et consultez les fiches de votre périmètre.',
  formNew: 'Ajouter un bénéficiaire', formEdit: 'Modifier la fiche', formSub: 'Le contrôle des doublons se fait pendant la saisie.',
  secAssociation: 'Rattachement', secIdentity: 'Identité', secMore: 'Autres informations',
  fAssociation: 'Association', fNomFr: 'Nom (FR)', fPrenomFr: 'Prénom (FR)', fNomAr: 'Nom (AR)', fPrenomAr: 'Prénom (AR)', fDob: 'Date de naissance', fSex: 'Sexe', fCin: 'CIN (facultatif)',
  choose: 'Choisir…',
  fEtablissement: 'Établissement', chooseAssociation: 'Rechercher une association…', chooseEtablissement: 'Choisir un établissement…', pickAssocFirst: 'Choisissez d’abord une association.', noEtab: 'Aucun établissement actif pour cette association.', ssNoResult: 'Aucun résultat', ssMore: 'Affinez la recherche pour voir plus de résultats.',
  etabTitle: 'Établissements', etabSub: 'Établissements rattachés aux associations.', etabAdd: 'Ajouter un établissement', etabEdit: 'Modifier l’établissement', etabName: 'Nom de l’établissement (AR)', etabType: 'Type d’établissement', etabLicense: 'N° de licence',
  colEtablissement: 'Établissement', colEtabs: 'Établissements', colType: 'Type', colLicense: 'Licence',
  etabDeleteTitle: 'Supprimer cet établissement ?', etabDeleteOk: 'Il n’a aucun bénéficiaire. La suppression est définitive.', etabDeleteBlocked: 'Suppression impossible : {n} bénéficiaires sont rattachés. Archivez l’établissement à la place.',
  etabSaved: 'Établissement enregistré', etabDeleted: 'Établissement supprimé', etabArchived: 'Établissement archivé', etabUnarchived: 'Établissement réactivé', etabArchivedTag: 'Archivé', etabShowArchived: 'Afficher les archivés',
  assocDeleteHasEtabs: 'Suppression impossible : {n} établissements sont rattachés à cette association.', readOnlyHint: 'Consultation seule : seul l’administrateur modifie les associations et les établissements.',
  dupTitle: 'Contrôle des doublons', dupIdle: 'Renseignez le nom, le prénom (FR) et la date de naissance.', dupChecking: 'Vérification…', dupOk: 'Aucun doublon détecté', dupExact: 'Cet enfant existe déjà',
  dupConfirm: 'Je confirme que c’est une autre personne (homonyme, jumeau).', dupSimilar: 'Fiches qui ressemblent', dupHidden: 'Existe dans une autre province : détails masqués pour votre rôle.', dupKey: 'Clé calculée',
  SWAPPED_DATE: 'jour et mois inversés ?', NEIGHBOUR_YEAR: 'année voisine', CLOSE_NAME: 'nom très proche',
  saved: 'Fiche enregistrée', archived: 'Fiche archivée', archiveTitle: 'Archiver cette fiche ?', archiveText: 'La fiche disparaît des listes et des statistiques. Elle reste conservée dans la base.',
  assocTitle: 'Associations', assocSub: 'Associations ayant un établissement dans votre périmètre.', assocAdd: 'Ajouter une association', assocEdit: 'Modifier l’association', assocName: 'Nom (AR)',
  assocCategories: 'Catégories', assocCategoriesHint: 'choix unique', assocShowArchived: 'Afficher les archivées', assocArchivedTag: 'Archivée',
  assocSaved: 'Association enregistrée', assocArchived: 'Association archivée', assocUnarchived: 'Association réactivée', unarchive: 'Réactiver', assocDeleted: 'Association supprimée', assocDeleteTitle: 'Supprimer cette association ?', assocDeleteOk: 'Elle n’a aucun bénéficiaire. La suppression est définitive.',
  assocDeleteBlocked: 'Suppression impossible : {n} bénéficiaires sont rattachés. Archivez l’association à la place.',
  usersTitle: 'Utilisateurs', usersSub: 'Comptes créés au premier démarrage. Seul l’administrateur peut les gérer.', colRole: 'Rôle', colScope: 'Périmètre', colState: 'État', colLastLogin: 'Dernière connexion',
  stActive: 'Actif', stDisabled: 'Désactivé', stMustChange: 'Mot de passe à changer', neverLogged: 'Jamais', resetPwd: 'Réinitialiser le mot de passe', disable: 'Désactiver', enable: 'Activer',
  resetTitle: 'Réinitialiser le mot de passe ?', resetText: 'Le compte reviendra au mot de passe par défaut et devra le changer à la prochaine connexion.', resetDone: 'Mot de passe réinitialisé', userUpdated: 'Compte mis à jour',
  auditTitle: 'Journal d’audit', auditSub: 'Connexions, saisies, modifications et exports.', colDate: 'Date', colUser: 'Utilisateur', colAction: 'Action', colDetail: 'Détail',
  errors: 'Erreur',
  e_INVALID_CREDENTIALS: 'E-mail ou mot de passe incorrect, ou compte verrouillé (réessayez dans 15 minutes).',
  e_WRONG_CURRENT_PASSWORD: 'Le mot de passe actuel est incorrect.', e_WEAK_PASSWORD: 'Mot de passe trop faible.', e_PASSWORD_REUSED: 'Choisissez un mot de passe différent de l’actuel et du mot de passe par défaut.',
  e_CIN_EXISTS: 'Ce CIN est déjà enregistré.', e_INVALID_CIN: 'CIN invalide (ex. : AB123456).', e_OUT_OF_SCOPE: 'Cet établissement est hors de votre périmètre.', e_READ_ONLY: 'Votre compte est en lecture seule.',
  e_ASSOCIATION_EXISTS: 'Cette association existe déjà.', e_HAS_BENEFICIARIES: 'Des bénéficiaires sont rattachés à cette association.', e_INVALID_CATEGORIES: 'Choisissez une catégorie.',
  e_INVALID_BIRTH_DATE: 'Date de naissance invalide.', e_INVALID_FR_NAME: 'Le nom et le prénom français doivent contenir des lettres.', e_VALIDATION: 'Vérifiez les champs du formulaire.',
  e_ETABLISSEMENT_ARCHIVED: 'Cet établissement est archivé.', e_ETABLISSEMENT_EXISTS: 'Cet établissement existe déjà pour cette association et cette province.', e_ETABLISSEMENT_NOT_FOUND: 'Établissement introuvable.', e_ASSOCIATION_NOT_FOUND: 'Association introuvable.', e_PROVINCE_NOT_FOUND: 'Province introuvable.', e_ADMIN_ONLY: 'Seul l’administrateur peut modifier les associations et les établissements.', e_HAS_ETABLISSEMENTS: 'Des établissements sont rattachés à cette association.', e_NOT_FOUND: 'Introuvable.', e_FORBIDDEN: 'Action non autorisée.', e_CONFLICT: 'Conflit avec une donnée existante.', e_INTERNAL: 'Erreur du serveur. Réessayez.', e_CANNOT_RESET_ADMIN: 'Le compte administrateur ne peut pas être réinitialisé ici.', e_CANNOT_DISABLE_ADMIN: 'Le compte administrateur ne peut pas être désactivé.',
} as const;

type Key = keyof typeof fr;

const ar: Record<Key, string> = {
  app: 'تتبع المستفيدين',
  login: 'تسجيل الدخول', email: 'البريد الإلكتروني', password: 'كلمة المرور', signin: 'دخول', logout: 'تسجيل الخروج',
  loginHint: 'استعمل حساب إقليمك أو جهتك.',
  changePwdTitle: 'اختر كلمة المرور الخاصة بك', changePwdIntro: 'لأمنك، يجب استبدال كلمة المرور الافتراضية قبل المتابعة.',
  currentPwd: 'كلمة المرور الحالية', newPwd: 'كلمة المرور الجديدة', confirmPwd: 'تأكيد كلمة المرور الجديدة', save: 'حفظ', cancel: 'إلغاء',
  pwdRules: 'عشرة أحرف على الأقل، مع حرف كبير وحرف صغير ورقم.', pwdMismatch: 'كلمتا المرور غير متطابقتين.',
  nav_dashboard: 'لوحة القيادة', nav_beneficiaries: 'المستفيدون', nav_new: 'إضافة مستفيد', nav_associations: 'الجمعيات', nav_etablissements: 'المؤسسات', nav_users: 'المستخدمون', nav_audit: 'السجل',
  role_ADMIN: 'مسؤول وطني', role_PROVINCE: 'مسؤول الإقليم', role_REGIONAL: 'اطلاع جهوي',
  readonly: 'قراءة فقط', national: 'وطني (12 جهة)', loading: 'جارٍ التحميل…', yes: 'نعم', no: 'لا', all: 'الكل', actions: 'إجراءات', edit: 'تعديل', delete: 'حذف', archive: 'أرشفة', close: 'إغلاق',
  hello: 'مرحبا', helloSub: 'هذا نشاط نطاقك.', cta: 'إضافة مستفيد',
  kpi_b: 'المستفيدون', kpi_a: 'الجمعيات', kpi_e: 'المؤسسات', kpi_p: 'الأقاليم', kpi_f: 'الفتيات',
  sexSplit: 'التوزيع حسب الجنس', girls: 'فتيات', boys: 'ذكور', total: 'المجموع',
  byProvince: 'حسب الإقليم', byEtablissement: 'حسب المؤسسة', ageBands: 'الفئات العمرية', ageYears: 'سنة', byCategory: 'المؤسسات حسب الفئة', latest: 'آخر البطاقات', moreProvinces: 'أقاليم أخرى غير معروضة',
  searchPh: 'بحث عن اسم (عربي أو فرنسي)', allProvinces: 'كل الأقاليم', allAssociations: 'كل الجمعيات', allSexes: 'كل الأجناس', allCategories: 'كل الفئات',
  colName: 'الاسم الكامل', colDob: 'تاريخ الازدياد', colSex: 'الجنس', colAssociation: 'الجمعية', colProvince: 'الإقليم', colCategories: 'الفئات', colBeneficiaries: 'المستفيدون',
  F: 'أنثى', M: 'ذكر', results: 'نتيجة', prev: 'السابق', next: 'التالي', empty: 'لا توجد نتائج.', exportCsv: 'تصدير (CSV)',
  listSub: 'ابحث وصفِّ واطلع على بطاقات نطاقك.',
  formNew: 'إضافة مستفيد', formEdit: 'تعديل البطاقة', formSub: 'تتم مراقبة التكرار أثناء الإدخال.',
  secAssociation: 'الانتماء', secIdentity: 'الهوية', secMore: 'معلومات أخرى',
  fAssociation: 'الجمعية', fNomFr: 'الاسم العائلي (فرنسية)', fPrenomFr: 'الاسم الشخصي (فرنسية)', fNomAr: 'الاسم العائلي (عربية)', fPrenomAr: 'الاسم الشخصي (عربية)', fDob: 'تاريخ الازدياد', fSex: 'الجنس', fCin: 'رقم البطاقة الوطنية (اختياري)',
  choose: 'اختر…',
  fEtablissement: 'المؤسسة', chooseAssociation: 'ابحث عن جمعية…', chooseEtablissement: 'اختر مؤسسة…', pickAssocFirst: 'اختر جمعية أولا.', noEtab: 'لا توجد مؤسسة نشطة لهذه الجمعية.', ssNoResult: 'لا توجد نتائج', ssMore: 'دقّق البحث لرؤية المزيد من النتائج.',
  etabTitle: 'المؤسسات', etabSub: 'المؤسسات التابعة للجمعيات.', etabAdd: 'إضافة مؤسسة', etabEdit: 'تعديل المؤسسة', etabName: 'اسم المؤسسة (عربية)', etabType: 'نوع المؤسسة', etabLicense: 'رقم الرخصة',
  colEtablissement: 'المؤسسة', colEtabs: 'المؤسسات', colType: 'النوع', colLicense: 'الرخصة',
  etabDeleteTitle: 'حذف هذه المؤسسة؟', etabDeleteOk: 'لا يوجد بها أي مستفيد. الحذف نهائي.', etabDeleteBlocked: 'تعذر الحذف: يوجد {n} مستفيدا مرتبطا بها. قم بأرشفة المؤسسة بدل حذفها.',
  etabSaved: 'تم حفظ المؤسسة', etabDeleted: 'تم حذف المؤسسة', etabArchived: 'تمت أرشفة المؤسسة', etabUnarchived: 'تمت إعادة تفعيل المؤسسة', etabArchivedTag: 'مؤرشفة', etabShowArchived: 'إظهار المؤرشفة',
  assocDeleteHasEtabs: 'تعذر الحذف: توجد {n} مؤسسة مرتبطة بهذه الجمعية.', readOnlyHint: 'للاطلاع فقط: المسؤول الوطني وحده يعدّل الجمعيات والمؤسسات.',
  dupTitle: 'مراقبة التكرار', dupIdle: 'أدخل الاسم العائلي والشخصي (بالفرنسية) وتاريخ الازدياد.', dupChecking: 'جارٍ التحقق…', dupOk: 'لا يوجد تكرار', dupExact: 'هذا الشخص مسجل مسبقا',
  dupConfirm: 'أؤكد أنه شخص آخر (تشابه أسماء أو توأم).', dupSimilar: 'بطاقات مشابهة', dupHidden: 'مسجل في إقليم آخر: التفاصيل مخفية بالنسبة لدورك.', dupKey: 'المفتاح المحسوب',
  SWAPPED_DATE: 'هل اليوم والشهر مقلوبان؟', NEIGHBOUR_YEAR: 'سنة مجاورة', CLOSE_NAME: 'اسم متقارب جدا',
  saved: 'تم حفظ البطاقة', archived: 'تمت أرشفة البطاقة', archiveTitle: 'أرشفة هذه البطاقة؟', archiveText: 'ستختفي البطاقة من اللوائح والإحصائيات، لكنها تبقى محفوظة في قاعدة البيانات.',
  assocTitle: 'الجمعيات', assocSub: 'الجمعيات التي لها مؤسسة في نطاقك.', assocAdd: 'إضافة جمعية', assocEdit: 'تعديل الجمعية', assocName: 'الاسم (عربية)',
  assocCategories: 'الفئات', assocCategoriesHint: 'اختيار واحد', assocShowArchived: 'إظهار المؤرشفة', assocArchivedTag: 'مؤرشفة',
  assocSaved: 'تم حفظ الجمعية', assocArchived: 'تمت أرشفة الجمعية', assocUnarchived: 'تمت إعادة تفعيل الجمعية', unarchive: 'إعادة التفعيل', assocDeleted: 'تم حذف الجمعية', assocDeleteTitle: 'حذف هذه الجمعية؟', assocDeleteOk: 'لا يوجد بها أي مستفيد. الحذف نهائي.',
  assocDeleteBlocked: 'تعذر الحذف: يوجد {n} مستفيدا مرتبطا بها. قم بأرشفتها بدل حذفها.',
  usersTitle: 'المستخدمون', usersSub: 'حسابات أنشئت عند أول تشغيل. المسؤول الوطني وحده يديرها.', colRole: 'الدور', colScope: 'النطاق', colState: 'الحالة', colLastLogin: 'آخر دخول',
  stActive: 'نشط', stDisabled: 'معطل', stMustChange: 'يجب تغيير كلمة المرور', neverLogged: 'أبدا', resetPwd: 'إعادة تعيين كلمة المرور', disable: 'تعطيل', enable: 'تفعيل',
  resetTitle: 'إعادة تعيين كلمة المرور؟', resetText: 'سيعود الحساب إلى كلمة المرور الافتراضية وعليه تغييرها عند الدخول التالي.', resetDone: 'تمت إعادة تعيين كلمة المرور', userUpdated: 'تم تحديث الحساب',
  auditTitle: 'سجل التدقيق', auditSub: 'الدخول والإدخال والتعديل والتصدير.', colDate: 'التاريخ', colUser: 'المستخدم', colAction: 'الإجراء', colDetail: 'التفاصيل',
  errors: 'خطأ',
  e_INVALID_CREDENTIALS: 'البريد أو كلمة المرور غير صحيحة، أو الحساب مقفل (أعد المحاولة بعد 15 دقيقة).',
  e_WRONG_CURRENT_PASSWORD: 'كلمة المرور الحالية غير صحيحة.', e_WEAK_PASSWORD: 'كلمة المرور ضعيفة.', e_PASSWORD_REUSED: 'اختر كلمة مرور مختلفة عن الحالية والافتراضية.',
  e_CIN_EXISTS: 'رقم البطاقة الوطنية مسجل مسبقا.', e_INVALID_CIN: 'رقم البطاقة الوطنية غير صالح (مثال: AB123456).', e_OUT_OF_SCOPE: 'هذه المؤسسة خارج نطاقك.', e_READ_ONLY: 'حسابك للقراءة فقط.',
  e_ASSOCIATION_EXISTS: 'هذه الجمعية موجودة مسبقا.', e_HAS_BENEFICIARIES: 'يوجد مستفيدون مرتبطون بهذه الجمعية.', e_INVALID_CATEGORIES: 'اختر فئة.',
  e_INVALID_BIRTH_DATE: 'تاريخ الازدياد غير صالح.', e_INVALID_FR_NAME: 'يجب أن يحتوي الاسم الفرنسي على حروف.', e_VALIDATION: 'تحقق من حقول النموذج.',
  e_ETABLISSEMENT_ARCHIVED: 'هذه المؤسسة مؤرشفة.', e_ETABLISSEMENT_EXISTS: 'هذه المؤسسة موجودة مسبقا لهذه الجمعية وهذا الإقليم.', e_ETABLISSEMENT_NOT_FOUND: 'المؤسسة غير موجودة.', e_ASSOCIATION_NOT_FOUND: 'الجمعية غير موجودة.', e_PROVINCE_NOT_FOUND: 'الإقليم غير موجود.', e_ADMIN_ONLY: 'المسؤول الوطني وحده يعدّل الجمعيات والمؤسسات.', e_HAS_ETABLISSEMENTS: 'توجد مؤسسات مرتبطة بهذه الجمعية.', e_NOT_FOUND: 'غير موجود.', e_FORBIDDEN: 'إجراء غير مسموح.', e_CONFLICT: 'تعارض مع معطيات موجودة.', e_INTERNAL: 'خطأ في الخادم. أعد المحاولة.', e_CANNOT_RESET_ADMIN: 'لا يمكن إعادة تعيين حساب المسؤول من هنا.', e_CANNOT_DISABLE_ADMIN: 'لا يمكن تعطيل حساب المسؤول.',
};

const dict: Record<Lang, Record<string, string>> = { fr, ar };

interface Ctx { lang: Lang; setLang: (l: Lang) => void; t: (k: string, vars?: Record<string, string | number>) => string; err: (code: string) => string; dir: 'ltr' | 'rtl' }
const I18n = createContext<Ctx>(null as any);

export function I18nProvider({ children }: { children: ReactNode }) {
  const [lang, setLangState] = useState<Lang>('fr');
  useEffect(() => { try { const s = localStorage.getItem('lang'); if (s === 'ar' || s === 'fr') setLangState(s); } catch {} }, []);
  useEffect(() => { document.documentElement.lang = lang; document.documentElement.dir = lang === 'ar' ? 'rtl' : 'ltr'; }, [lang]);
  const setLang = useCallback((l: Lang) => { setLangState(l); try { localStorage.setItem('lang', l); } catch {} }, []);
  const value = useMemo<Ctx>(() => ({
    lang, setLang, dir: lang === 'ar' ? 'rtl' : 'ltr',
    t: (k, vars) => { let s = dict[lang][k] ?? dict.fr[k] ?? k; if (vars) for (const [a, b] of Object.entries(vars)) s = s.replace(`{${a}}`, String(b)); return s; },
    err: (code) => dict[lang]['e_' + code] ?? dict[lang].e_INTERNAL,
  }), [lang, setLang]);
  return <I18n.Provider value={value}>{children}</I18n.Provider>;
}
export const useI18n = () => useContext(I18n);
