package ma.entraide.suivi.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.Chunk;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * "Les bénéficiaires" PDF: logo, bilingual title, beneficiaries grouped by association then établissement
 * (and by province when the export covers several provinces), signature area at the bottom of the last page.
 * Fonts are embedded (Manrope for Latin, Noto Sans Arabic for Arabic) so the file looks the same on every computer.
 */
public final class BeneficiaryPdf {

    // ---- input model
    public record Person(String nomFr, String nomAr, String cin, LocalDate dateNaissance, String sexe) {}
    public record Etab(String nomAr, List<Person> people) {}
    public record Asso(String nomAr, List<Etab> etabs) {}
    public record Prov(String nomFr, String nomAr, List<Asso> assos) {}
    public record Data(String titleFr, String titleAr, String regionFr, String regionAr, String placeFr, String placeAr,
                       LocalDate date, String by, boolean provinceBands, List<Prov> provinces) {}

    // ---- palette (same as the web application)
    private static final Color GREEN = new Color(0x2F, 0x8A, 0x0C);
    private static final Color LIME = new Color(0x6B, 0xD5, 0x2D);
    private static final Color RED = new Color(0xD7, 0x00, 0x0F);
    private static final Color INK = new Color(0x1A, 0x22, 0x18);
    private static final Color MUTED = new Color(0x66, 0x70, 0x61);
    private static final Color LINE = new Color(0xD9, 0xE0, 0xD3);
    private static final Color HEAD_BG = new Color(0xEE, 0xF4, 0xE8);
    private static final Color HEAD_FG = new Color(0x2D, 0x4A, 0x1C);
    private static final Color ETAB_BG = new Color(0xE4, 0xF4, 0xD8);
    private static final Color ETAB_FG = new Color(0x1F, 0x4D, 0x0B);
    private static final Color PROV_BG = new Color(0x1A, 0x3D, 0x10);

    /** Relative widths: N°, name (FR), name (AR), CIN, birth date, sex. */
    private static final float[] W = {4.5f, 25f, 21f, 13f, 17.5f, 19f};
    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final class Fonts {
        static final BaseFont LAT = load("Manrope_400Regular.ttf");
        static final BaseFont LAT_B = load("Manrope_700Bold.ttf");
        static final BaseFont LAT_XB = load("Manrope_800ExtraBold.ttf");
        static final BaseFont AR = load("NotoSansArabic_400Regular.ttf");
        static final BaseFont AR_B = load("NotoSansArabic_700Bold.ttf");

        private static BaseFont load(String name) {
            try (InputStream in = BeneficiaryPdf.class.getResourceAsStream("/pdf/" + name)) {
                if (in == null) throw new IllegalStateException("Police introuvable : " + name);
                return BaseFont.createFont(name, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, in.readAllBytes(), null);
            } catch (IOException | DocumentException e) {
                throw new IllegalStateException("Police illisible : " + name, e);
            }
        }
    }

    // ---- entry point
    public static byte[] render(Data d) {
        Document doc = new Document(PageSize.A4.rotate(), mm(14), mm(14), mm(13), mm(17));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new Footer());
            doc.open();
            doc.addTitle(d.titleFr());
            doc.addCreator("Suivi des bénéficiaires");
            header(doc, d);
            doc.add(table(d));
            signature(doc, writer, d);
            doc.close();
        } catch (DocumentException | IOException e) {
            throw new IllegalStateException("Génération du PDF impossible", e);
        }
        return out.toByteArray();
    }

    private static float mm(float v) { return v * 72f / 25.4f; }

    // ---- page header (first page only)
    private static void header(Document doc, Data d) throws DocumentException, IOException {
        try (InputStream in = BeneficiaryPdf.class.getResourceAsStream("/pdf/logo.png")) {
            Image logo = Image.getInstance(in.readAllBytes());
            logo.scaleToFit(mm(120), mm(17));
            logo.setAlignment(Element.ALIGN_CENTER);
            logo.setSpacingAfter(4);
            doc.add(logo);
        }
        PdfPTable titles = new PdfPTable(1);
        titles.setWidthPercentage(100);
        titles.addCell(plain(phrase(d.titleFr(), 14.5f, true, INK), Element.ALIGN_CENTER, 0));
        if (d.titleAr() != null) titles.addCell(plain(phrase(d.titleAr(), 12.5f, true, INK), Element.ALIGN_CENTER, 2));
        doc.add(titles);

        PdfPTable rule = new PdfPTable(new float[]{55, 25, 20});
        rule.setWidthPercentage(100);
        rule.setSpacingBefore(5);
        rule.setSpacingAfter(5);
        for (Color c : new Color[]{GREEN, LIME, RED}) {
            PdfPCell cell = new PdfPCell(phrase(" ", 1, false, Color.WHITE));
            cell.setBackgroundColor(c);
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setFixedHeight(3.2f);
            rule.addCell(cell);
        }
        doc.add(rule);

        int people = d.provinces().stream().flatMap(p -> p.assos().stream()).flatMap(a -> a.etabs().stream()).mapToInt(e -> e.people().size()).sum();
        int etabs = d.provinces().stream().flatMap(p -> p.assos().stream()).mapToInt(a -> a.etabs().size()).sum();
        int assos = d.provinces().stream().mapToInt(p -> p.assos().size()).sum();
        PdfPTable meta = new PdfPTable(new float[]{1, 1.3f, 1.2f});
        meta.setWidthPercentage(100);
        meta.setSpacingAfter(6);
        meta.addCell(plain(phrase("Région : " + d.regionFr(), 8, false, MUTED), Element.ALIGN_LEFT, 0));
        meta.addCell(plain(phrase("Édité le " + d.date().format(DMY) + " par " + d.by(), 8, false, MUTED), Element.ALIGN_CENTER, 0));
        meta.addCell(plain(phrase(people + " bénéficiaires · " + etabs + " établissements · " + assos + " associations", 8, false, MUTED), Element.ALIGN_RIGHT, 0));
        meta.addCell(plain(phrase("الجهة : " + d.regionAr(), 8.5f, false, MUTED), Element.ALIGN_LEFT, 2));
        meta.addCell(plain(phrase("حرر بتاريخ " + d.date().format(DMY), 8.5f, false, MUTED), Element.ALIGN_CENTER, 2));
        meta.addCell(plain(phrase(people + " مستفيدا · " + etabs + " مؤسسات · " + assos + " جمعيات", 8.5f, false, MUTED), Element.ALIGN_RIGHT, 2));
        doc.add(meta);
    }

    // ---- main table
    private static PdfPTable table(Data d) throws DocumentException {
        PdfPTable t = new PdfPTable(W.length);
        t.setWidthPercentage(100);
        t.setWidths(W);
        t.setHeaderRows(2);       // column titles (French row + Arabic row) repeat on every page
        t.setSplitRows(false);    // a row never breaks across two pages
        String[][] heads = {{"N°", "#"}, {"NOM ET PRÉNOM", "الاسم الكامل (فرنسية)"}, {"NOM EN ARABE", "الاسم الكامل بالعربية"},
                {"CIN", "رقم البطاقة الوطنية"}, {"NAISSANCE", "تاريخ الازدياد"}, {"SEXE", "الجنس"}};
        int[] aligns = {Element.ALIGN_LEFT, Element.ALIGN_LEFT, Element.ALIGN_RIGHT, Element.ALIGN_LEFT, Element.ALIGN_LEFT, Element.ALIGN_LEFT};
        for (int i = 0; i < heads.length; i++) {
            PdfPCell c = plain(phrase(heads[i][0], 7.5f, true, HEAD_FG), aligns[i], 0);
            c.setBackgroundColor(HEAD_BG);
            c.setPaddingTop(5);
            c.setPaddingBottom(0);
            if (i == 2) c.setPaddingRight(20);
            t.addCell(c);
        }
        for (int i = 0; i < heads.length; i++) {
            PdfPCell c = plain(phrase(i == 0 ? " " : heads[i][1], 7.5f, false, new Color(0x5B, 0x7A, 0x49)), aligns[i], i == 0 ? 0 : 2);
            c.setBackgroundColor(HEAD_BG);
            c.setPaddingTop(0);
            c.setPaddingBottom(4);
            if (i == 2) c.setPaddingRight(20);
            c.setBorderWidthBottom(1.6f);
            c.setBorderColorBottom(GREEN);
            c.setBorder(Rectangle.BOTTOM);
            t.addCell(c);
        }

        int n = 0;
        for (Prov p : d.provinces()) {
            boolean firstOfProvince = d.provinceBands();
            for (Asso a : p.assos()) {
                boolean firstOfAsso = true;
                for (Etab e : a.etabs()) {
                    // "head block" = province band (if new) + association band (if new) + établissement band + first beneficiary.
                    // Kept in a single, unbreakable row so a band is never left alone at the bottom of a page.
                    PdfPTable head = new PdfPTable(W.length);
                    head.setWidthPercentage(100);
                    head.setWidths(W);
                    if (firstOfProvince) {
                        int pp = p.assos().stream().flatMap(x -> x.etabs().stream()).mapToInt(x -> x.people().size()).sum();
                        head.addCell(bandCell(band(PROV_BG, Color.WHITE, 0, "PROVINCE", "الإقليم", p.nomFr() + "  ·  " + p.nomAr(), null, null,
                                "Associations", "الجمعيات", p.assos().size(), "Bénéficiaires", "المستفيدون", pp, true)));
                        firstOfProvince = false;
                    }
                    if (firstOfAsso) {
                        int na = a.etabs().stream().mapToInt(x -> x.people().size()).sum();
                        head.addCell(bandCell(band(GREEN, Color.WHITE, 0, "ASSOCIATION", "الجمعية", a.nomAr(), null, null,
                                "Établissements", "المؤسسات", a.etabs().size(), "Bénéficiaires", "المستفيدون", na, false)));
                        firstOfAsso = false;
                    }
                    head.addCell(bandCell(band(ETAB_BG, ETAB_FG, 16, "ÉTABLISSEMENT", "المؤسسة", e.nomAr(), null, null,
                            "Bénéficiaires", "المستفيدون", e.people().size(), null, null, 0, false)));
                    List<Person> ppl = e.people();
                    if (!ppl.isEmpty()) person(head, ++n, ppl.get(0));
                    PdfPCell hc = new PdfPCell(head);
                    hc.setColspan(W.length);
                    hc.setPadding(0);
                    hc.setBorder(Rectangle.NO_BORDER);
                    t.addCell(hc);
                    for (int i = 1; i < ppl.size(); i++) person(t, ++n, ppl.get(i));
                }
            }
        }
        if (n == 0) {
            PdfPCell none = plain(phrase("Aucun bénéficiaire ne correspond aux filtres choisis.", 10, false, MUTED), Element.ALIGN_CENTER, 0);
            none.setColspan(W.length);
            none.setPadding(18);
            t.addCell(none);
        }
        return t;
    }

    private static PdfPCell bandCell(PdfPTable band) {
        PdfPCell c = new PdfPCell(band);
        c.setColspan(W.length);
        c.setPadding(0);
        c.setBorder(Rectangle.NO_BORDER);
        return c;
    }

    /** One coloured band: [LABEL fr] [label ar] [name] [counts]. Separate cells keep the bidirectional text in order. */
    private static PdfPTable band(Color bg, Color fg, float indent, String labFr, String labAr, String name, String x1, String x2,
                                  String c1Fr, String c1Ar, int c1, String c2Fr, String c2Ar, int c2, boolean strong) {
        PdfPTable t = new PdfPTable(new float[]{13, 8, 46, 33});
        t.setWidthPercentage(100);
        float vpad = strong ? 6 : 5;
        PdfPCell a = plain(phrase(labFr, 7, true, fg), Element.ALIGN_LEFT, 0);
        a.setPaddingLeft(indent + 6);
        PdfPCell b = plain(phrase(labAr, 7.5f, false, fg), Element.ALIGN_LEFT, 2);
        PdfPCell c = plain(phrase(name, strong ? 10.5f : 10, true, fg), Element.ALIGN_LEFT, containsLatin(name) ? 1 : 2);
        Phrase counts = new Phrase();
        counts.add(phrase(c1Fr + " · ", 7, false, fg));
        counts.add(phrase(c1Ar, 7.5f, false, fg));
        counts.add(phrase("  " + c1, 8, true, fg));
        if (c2Fr != null) {
            counts.add(phrase("      " + c2Fr + " · ", 7, false, fg));
            counts.add(phrase(c2Ar, 7.5f, false, fg));
            counts.add(phrase("  " + c2, 8, true, fg));
        }
        PdfPCell d = plain(counts, Element.ALIGN_RIGHT, 1);
        d.setPaddingRight(8);
        for (PdfPCell x : new PdfPCell[]{a, b, c, d}) {
            x.setBackgroundColor(bg);
            x.setPaddingTop(vpad);
            x.setPaddingBottom(vpad);
            t.addCell(x);
        }
        return t;
    }

    private static void person(PdfPTable t, int n, Person p) {
        t.addCell(row(phrase(String.valueOf(n), 8, false, MUTED), Element.ALIGN_LEFT, 0));
        t.addCell(row(phrase(p.nomFr(), 8.5f, true, INK), Element.ALIGN_LEFT, 0));
        PdfPCell arName = row(phrase(p.nomAr(), 9.5f, false, INK), Element.ALIGN_RIGHT, 2);
        arName.setPaddingRight(20);
        t.addCell(arName);
        boolean hasCin = p.cin() != null && !p.cin().isBlank();
        t.addCell(row(phrase(hasCin ? p.cin() : "Aucun", 8.5f, false, hasCin ? INK : new Color(0x9A, 0xA5, 0x94)), Element.ALIGN_LEFT, 0));
        t.addCell(row(phrase(p.dateNaissance().format(DMY), 8.5f, false, INK), Element.ALIGN_LEFT, 0));
        boolean girl = "F".equals(p.sexe());
        Phrase sex = new Phrase();
        sex.add(phrase((girl ? "Fille" : "Garçon") + " · ", 8.5f, false, INK));
        sex.add(phrase(girl ? "أنثى" : "ذكر", 9, false, INK));
        t.addCell(row(sex, Element.ALIGN_LEFT, 1));
    }

    // ---- signature (anchored to the bottom of the last page)
    private static void signature(Document doc, PdfWriter writer, Data d) throws DocumentException {
        PdfPTable s = new PdfPTable(new float[]{1f, 1.15f});
        s.setTotalWidth(doc.right() - doc.left());
        PdfPCell lc = new PdfPCell(phrase(" ", 8, false, INK));   // placeholder: the left column is drawn by hand below
        lc.setBorder(Rectangle.NO_BORDER);
        s.addCell(lc);

        PdfPTable right = new PdfPTable(1);
        right.setWidthPercentage(100);
        right.addCell(plain(phrase("Le responsable de la province · مسؤول الإقليم", 9, true, INK), Element.ALIGN_LEFT, 1));
        PdfPCell box = new PdfPCell(phrase(" ", 8, false, INK));
        box.setFixedHeight(mm(30));
        box.setBorderColor(new Color(0x9A, 0xA5, 0x94));
        box.setBorderWidth(0.8f);
        right.addCell(box);
        right.addCell(plain(phrase("Nom et prénom · الاسم الكامل : ................................................", 8, false, MUTED), Element.ALIGN_LEFT, 1));
        PdfPCell rc = new PdfPCell(right);
        rc.setBorder(Rectangle.NO_BORDER);
        s.addCell(rc);

        s.calculateHeights(true);
        float need = s.getTotalHeight() + 10;
        float free = writer.getVerticalPosition(false) - doc.bottom();
        if (free < need) doc.newPage();
        float top = doc.bottom() + s.getTotalHeight();
        PdfContentByte cb = writer.getDirectContent();
        s.writeSelectedRows(0, -1, doc.left(), top, cb);
        float x = doc.left() + 3;
        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, phrase("Fait à · حرر بـ", 9, true, INK), x, top - 13, 0, PdfWriter.RUN_DIRECTION_LTR, 0);
        String frPlace = d.placeFr() == null || d.placeFr().isBlank() ? "......................" : d.placeFr();
        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, phrase(frPlace + ", le ......../......../............", 9, false, MUTED), x, top - 29, 0, PdfWriter.RUN_DIRECTION_LTR, 0);
        // Arabic date line: dots (left-to-right) then the Arabic words on their right, so that it reads "... في ,تازة"
        String arPlace = d.placeAr() == null || d.placeAr().isBlank() ? "......................" : d.placeAr();
        Phrase dots = phrase("....../....../........", 9, false, MUTED);
        Phrase words = phrase(arPlace + "، في", 9, false, MUTED);
        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, dots, x, top - 45, 0, PdfWriter.RUN_DIRECTION_LTR, 0);
        float dotsW = ColumnText.getWidth(dots, PdfWriter.RUN_DIRECTION_LTR, 0);
        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, words, x + dotsW + 6, top - 45, 0, PdfWriter.RUN_DIRECTION_RTL, 0);
    }

    // ---- footer: application name (left) and "Page n / N" (right) on every page
    private static final class Footer extends PdfPageEventHelper {
        private PdfTemplate total;

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            total = writer.getDirectContent().createTemplate(40, 12);
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            float y = document.bottom() - 16;
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, phrase("Suivi des bénéficiaires — Entraide Nationale", 7.5f, false, MUTED), document.left(), y, 0);
            String page = "Page " + writer.getPageNumber() + " / ";
            float right = document.right();
            cb.saveState();
            cb.setColorFill(MUTED);
            cb.beginText();
            cb.setFontAndSize(Fonts.LAT, 7.5f);
            float tw = Fonts.LAT.getWidthPoint(page, 7.5f);
            cb.setTextMatrix(right - tw - 14, y);
            cb.showText(page);
            cb.endText();
            cb.restoreState();
            cb.addTemplate(total, right - 14, y);
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            total.beginText();
            total.setColorFill(MUTED);
            total.setFontAndSize(Fonts.LAT, 7.5f);
            total.setTextMatrix(0, 0);
            total.showText(String.valueOf(writer.getPageNumber() - 1));
            total.endText();
        }
    }

    // ---- text helpers
    private static Font font(BaseFont bf, float size, Color c) { return new Font(bf, size, Font.NORMAL, c); }

    private static boolean isArabic(char c) {
        return (c >= 0x0600 && c <= 0x06FF) || (c >= 0x0750 && c <= 0x077F) || (c >= 0xFB50 && c <= 0xFDFF) || (c >= 0xFE70 && c <= 0xFEFF);
    }

    private static boolean containsLatin(String s) {
        for (int i = 0; i < s.length(); i++) if (Character.isLetter(s.charAt(i)) && !isArabic(s.charAt(i))) return true;
        return false;
    }

    /** Splits the text in Arabic / non-Arabic runs and gives each run its own font. */
    private static Phrase phrase(String text, float size, boolean bold, Color color) {
        BaseFont lat = bold ? Fonts.LAT_B : Fonts.LAT;
        BaseFont ar = bold ? Fonts.AR_B : Fonts.AR;
        Phrase ph = new Phrase();
        if (text == null || text.isEmpty()) return ph;
        StringBuilder run = new StringBuilder();
        Boolean arabic = null;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            Boolean isAr = isArabic(c) ? Boolean.TRUE : (Character.isLetterOrDigit(c) ? Boolean.FALSE : null); // spaces / punctuation follow the current run
            if (isAr != null && arabic != null && isAr != arabic && run.length() > 0) {
                ph.add(new Chunk(run.toString(), font(arabic ? ar : lat, size, color)));
                run.setLength(0);
            }
            if (isAr != null) arabic = isAr;
            run.append(c);
        }
        if (run.length() > 0) ph.add(new Chunk(run.toString(), font(Boolean.TRUE.equals(arabic) ? ar : lat, size, color)));
        return ph;
    }

    /**
     * Borderless cell. bidi: 0 = Latin text; 1 = Latin and Arabic mixed (left-to-right paragraph, Arabic runs shaped and
     * reordered); 2 = Arabic only (right-to-left paragraph). `align` is always the VISUAL alignment: in a right-to-left cell
     * OpenPDF reads LEFT/RIGHT the other way round, so it is flipped here once.
     */
    private static PdfPCell plain(Phrase p, int align, int bidi) {
        PdfPCell c = new PdfPCell(p);
        c.setBorder(Rectangle.NO_BORDER);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(3);
        if (bidi == 1) c.setRunDirection(PdfWriter.RUN_DIRECTION_LTR);
        if (bidi == 2) {
            c.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
            if (align == Element.ALIGN_LEFT) align = Element.ALIGN_RIGHT;
            else if (align == Element.ALIGN_RIGHT) align = Element.ALIGN_LEFT;
        }
        c.setHorizontalAlignment(align);
        return c;
    }

    /** Table body cell with a thin bottom rule. */
    private static PdfPCell row(Phrase p, int align, int bidi) {
        PdfPCell c = plain(p, align, bidi);
        c.setPaddingTop(4.5f);
        c.setPaddingBottom(4.5f);
        c.setPaddingLeft(5);
        c.setPaddingRight(5);
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderWidthBottom(0.5f);
        c.setBorderColorBottom(LINE);
        return c;
    }

    private BeneficiaryPdf() {}
}
