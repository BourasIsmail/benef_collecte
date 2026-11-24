package ma.entraide.suivi.seed;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ma.entraide.suivi.domain.*;
import ma.entraide.suivi.repo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Loads regions, provinces, associations and categories (from the original Excel) on first start only. */
@Component @Order(1)
public class ReferenceDataSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(ReferenceDataSeeder.class);

    record RegionSeed(String code, String nomFr, String nomAr) {}
    record ProvinceSeed(String code, String nomFr, String nomAr, String region) {}
    record AssociationSeed(String province, String nomAr) {}

    private final RegionRepository regions;
    private final ProvinceRepository provinces;
    private final CategoryRepository categories;
    private final AssociationRepository associations;
    private final ObjectMapper mapper;
    private final TransactionTemplate tx;

    public ReferenceDataSeeder(RegionRepository regions, ProvinceRepository provinces, CategoryRepository categories,
                               AssociationRepository associations, ObjectMapper mapper, TransactionTemplate tx) {
        this.regions = regions; this.provinces = provinces; this.categories = categories;
        this.associations = associations; this.mapper = mapper; this.tx = tx;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (regions.count() > 0) return;
        tx.executeWithoutResult(s -> {
            Map<String, Region> regionByCode = new HashMap<>();
            for (RegionSeed r : read("seed/regions.json", new TypeReference<List<RegionSeed>>() {})) {
                Region e = new Region(); e.setCode(r.code()); e.setNomFr(r.nomFr()); e.setNomAr(r.nomAr());
                regionByCode.put(r.code(), regions.save(e));
            }
            Map<String, Province> provinceByCode = new HashMap<>();
            for (ProvinceSeed p : read("seed/provinces.json", new TypeReference<List<ProvinceSeed>>() {})) {
                Province e = new Province(); e.setCode(p.code()); e.setNomFr(p.nomFr()); e.setNomAr(p.nomAr());
                e.setRegion(regionByCode.get(p.region()));
                provinceByCode.put(p.code(), provinces.save(e));
            }
            for (RegionSeed c : read("seed/categories.json", new TypeReference<List<RegionSeed>>() {})) {
                Category e = new Category(); e.setCode(c.code()); e.setNomFr(c.nomFr()); e.setNomAr(c.nomAr());
                categories.save(e);
            }
            int n = 0;
            for (AssociationSeed a : read("seed/associations.json", new TypeReference<List<AssociationSeed>>() {})) {
                Association e = new Association(); e.setProvince(provinceByCode.get(a.province())); e.setNomAr(a.nomAr());
                associations.save(e); n++;
            }
            log.info("Données de référence chargées : {} régions, {} provinces, {} associations", regionByCode.size(), provinceByCode.size(), n);
        });
    }

    private <T> T read(String path, TypeReference<T> type) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) { return mapper.readValue(in, type); }
        catch (Exception e) { throw new IllegalStateException("Lecture impossible : " + path, e); }
    }
}
