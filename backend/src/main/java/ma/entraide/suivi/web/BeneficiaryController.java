package ma.entraide.suivi.web;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import ma.entraide.suivi.security.AppUserPrincipal;
import ma.entraide.suivi.service.AuditService;
import ma.entraide.suivi.service.BeneficiaryService;
import ma.entraide.suivi.service.DedupService;
import ma.entraide.suivi.web.Dtos.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/beneficiaries")
public class BeneficiaryController {
    private final BeneficiaryService service;
    private final AuditService audit;

    public BeneficiaryController(BeneficiaryService service, AuditService audit) { this.service = service; this.audit = audit; }

    @GetMapping
    public PageDto<BeneficiaryDto> list(@AuthenticationPrincipal AppUserPrincipal me, @RequestParam(required = false) String q,
                                        @RequestParam(required = false) Long etablissementId, @RequestParam(required = false) Long categoryId,
                                        @RequestParam(required = false) Long associationId,
                                        @RequestParam(required = false) Long provinceId, @RequestParam(required = false) String sexe,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        return service.search(me, q, etablissementId, categoryId, associationId, provinceId, sexe, page, size);
    }

    @GetMapping("/{id}")
    public BeneficiaryDto get(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long id) { return service.get(me, id); }

    @PostMapping("/check")
    public DedupService.Result check(@AuthenticationPrincipal AppUserPrincipal me, @Valid @RequestBody CheckRequest r) {
        return service.check(me, r);
    }

    @PostMapping
    public BeneficiaryDto create(@AuthenticationPrincipal AppUserPrincipal me, @Valid @RequestBody BeneficiaryRequest r) {
        return service.create(me, r);
    }

    @PutMapping("/{id}")
    public BeneficiaryDto update(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long id, @Valid @RequestBody BeneficiaryRequest r) {
        return service.update(me, id, r);
    }

    @DeleteMapping("/{id}")
    public void archive(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long id) { service.archive(me, id); }

    /** CSV export (UTF-8 with BOM so Excel opens Arabic correctly), restricted to the caller's perimeter. */
    @GetMapping("/export.csv")
    public void export(@AuthenticationPrincipal AppUserPrincipal me, @RequestParam(required = false) String q,
                       @RequestParam(required = false) Long etablissementId, @RequestParam(required = false) Long categoryId,
                       @RequestParam(required = false) Long associationId,
                       @RequestParam(required = false) Long provinceId, @RequestParam(required = false) String sexe, HttpServletResponse res) throws IOException {
        var rows = service.exportAll(me, q, etablissementId, categoryId, associationId, provinceId, sexe);
        audit.log(me.email(), "EXPORT_CSV", "Beneficiary", null, rows.size() + " lignes");
        res.setContentType("text/csv;charset=UTF-8");
        res.setHeader("Content-Disposition", "attachment; filename=\"beneficiaires.csv\"");
        PrintWriter w = new PrintWriter(new java.io.OutputStreamWriter(res.getOutputStream(), StandardCharsets.UTF_8));
        w.write('﻿');
        w.write("Province,Association,Établissement,Nom (FR),Prénom (FR),Nom (AR),Prénom (AR),Date de naissance,Sexe,CIN\r\n");
        for (var b : rows) {
            w.write(String.join(",", csv(b.provinceNomFr()), csv(b.associationNom()), csv(b.etablissementNom()), csv(b.nomFr()), csv(b.prenomFr()),
                    csv(b.nomAr()), csv(b.prenomAr()), String.valueOf(b.dateNaissance()), b.sexe(), csv(b.cin())) + "\r\n");
        }
        w.flush();
    }

    /** Quotes the value and neutralises spreadsheet formula injection. */
    private static String csv(String v) {
        if (v == null) return "";
        String s = v;
        if (!s.isEmpty() && "=+-@\t\r".indexOf(s.charAt(0)) >= 0) s = "'" + s;
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}
