package ma.entraide.suivi.pdf;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import ma.entraide.suivi.pdf.BeneficiaryPdf.*;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BeneficiaryPdfTest {

    private static List<Person> people(int n, int seed) {
        String[] fr = {"BAHALI SALMA", "TAZI KHADIJA", "EL FASSI HAMZA", "ANTRA MOHAMED", "IDRISSI KARIM", "OUALI AYA"};
        String[] ar = {"بهالي سلمى", "التازي خديجة", "الفاسي حمزة", "عنترا محمد", "الإدريسي كريم", "والي آية"};
        List<Person> l = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            int k = (i + seed) % fr.length;
            l.add(new Person(fr[k], ar[k], (i % 3 == 0) ? "JB" + (100000 + i * 7) : null, LocalDate.of(2008 + i % 10, 1 + i % 12, 1 + i % 27), k % 2 == 0 ? "F" : "M"));
        }
        return l;
    }

    private static Data single(int etabs, int perEtab) {
        List<Asso> assos = new ArrayList<>();
        for (int a = 0; a < 7; a++) {
            List<Etab> es = new ArrayList<>();
            for (int e = 0; e < etabs; e++) es.add(new Etab("دار الطالب والطالبة " + a + "-" + e, people(perEtab, a + e)));
            assos.add(new Asso("الجمعية الخيرية الإسلامية " + a, es));
        }
        return new Data("Les bénéficiaires de la province : Taza", "المستفيدون بإقليم : تازة", "Fès-Meknès", "فاس مكناس",
                "Taza", "تازة", LocalDate.of(2026, 11, 27), "taza@entraide.ma", false, List.of(new Prov("Taza", "تازة", assos)));
    }

    private static String text(byte[] pdf, int page) throws Exception {
        PdfReader r = new PdfReader(pdf);
        try { return new PdfTextExtractor(r).getTextFromPage(page); } finally { r.close(); }
    }

    @Test
    void producesAValidMultiPagePdfWithTheFrenchLabels() throws Exception {
        byte[] pdf = BeneficiaryPdf.render(single(2, 6));
        assertEquals("%PDF", new String(pdf, 0, 4));
        PdfReader r = new PdfReader(pdf);
        int pages = r.getNumberOfPages();
        r.close();
        assertTrue(pages >= 2, "le tableau doit tenir sur plusieurs pages, pages=" + pages);
        String p1 = text(pdf, 1);
        assertTrue(p1.contains("Les bénéficiaires de la province : Taza"), p1);
        assertTrue(p1.contains("Aucun"), "un bénéficiaire sans CIN doit afficher « Aucun »");
        assertTrue(p1.contains("JB100000"), "un CIN existant doit être affiché");
        assertTrue(p1.contains("Page 1 /"), "pied de page avec le numéro de page");
        assertTrue(text(pdf, pages).contains("Le responsable de la province"), "la signature est sur la dernière page");
        Path out = Path.of("target", "sample-beneficiaires.pdf");
        Files.write(out, pdf);
    }

    @Test
    void emptyExportStillProducesADocument() throws Exception {
        Data d = new Data("Les bénéficiaires — toutes les provinces", "المستفيدون — جميع الأقاليم", "Toutes les régions", "جميع الجهات",
                null, null, LocalDate.of(2026, 11, 27), "admin@entraide.ma", true, List.of());
        byte[] pdf = BeneficiaryPdf.render(d);
        assertTrue(text(pdf, 1).contains("Aucun bénéficiaire ne correspond"));
    }

    @Test
    void severalProvincesGetAProvinceBand() throws Exception {
        Data one = single(1, 2);
        List<Prov> provs = List.of(new Prov("Fès", "فاس", one.provinces().get(0).assos().subList(0, 2)),
                new Prov("Taza", "تازة", one.provinces().get(0).assos().subList(2, 4)));
        Data d = new Data("Les bénéficiaires — toutes les provinces", "المستفيدون — جميع الأقاليم", "Toutes les régions", "جميع الجهات",
                null, null, LocalDate.of(2026, 11, 27), "admin@entraide.ma", true, provs);
        byte[] pdf = BeneficiaryPdf.render(d);
        String t = text(pdf, 1);
        assertTrue(t.contains("PROVINCE"), t);
        Files.write(Path.of("target", "sample-beneficiaires-multi.pdf"), pdf);
    }

    @Test
    void thousandsOfRowsRenderInReasonableTime() {
        long t0 = System.currentTimeMillis();
        byte[] pdf = BeneficiaryPdf.render(single(8, 100));   // 5 600 beneficiaries
        long ms = System.currentTimeMillis() - t0;
        assertTrue(pdf.length > 10_000);
        assertTrue(ms < 20_000, "trop lent : " + ms + " ms");
        System.out.println("5600 lignes : " + ms + " ms, " + pdf.length / 1024 + " Ko");
    }
}
