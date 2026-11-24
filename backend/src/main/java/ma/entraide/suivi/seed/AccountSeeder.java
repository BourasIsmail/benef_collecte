package ma.entraide.suivi.seed;

import ma.entraide.suivi.config.AppProperties;
import ma.entraide.suivi.domain.AppUser;
import ma.entraide.suivi.domain.Role;
import ma.entraide.suivi.repo.ProvinceRepository;
import ma.entraide.suivi.repo.RegionRepository;
import ma.entraide.suivi.repo.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * First start only: one ADMIN account, one PROVINCE account per province ({code}@domain) and one REGIONAL
 * account per region (region.{code}@domain). Everybody gets the default password and must change it at first login.
 */
@Component @Order(2)
public class AccountSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AccountSeeder.class);

    private final UserRepository users;
    private final ProvinceRepository provinces;
    private final RegionRepository regions;
    private final PasswordEncoder encoder;
    private final AppProperties props;
    private final TransactionTemplate tx;

    public AccountSeeder(UserRepository users, ProvinceRepository provinces, RegionRepository regions, PasswordEncoder encoder,
                         AppProperties props, TransactionTemplate tx) {
        this.users = users; this.provinces = provinces; this.regions = regions; this.encoder = encoder; this.props = props; this.tx = tx;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users.count() > 0) return;
        if (props.defaultPassword() == null || props.defaultPassword().length() < 10)
            throw new IllegalStateException("APP_DEFAULT_PASSWORD doit contenir au moins 10 caractères");
        String defaultHash = encoder.encode(props.defaultPassword());
        String adminHash = props.adminPassword() == null || props.adminPassword().isBlank() ? defaultHash : encoder.encode(props.adminPassword());
        tx.executeWithoutResult(s -> {
            users.save(user(props.adminEmail(), adminHash, Role.ADMIN));
            for (var p : provinces.findAll()) {
                AppUser u = user(p.getCode() + "@" + props.emailDomain(), defaultHash, Role.PROVINCE);
                u.setProvince(p);
                users.save(u);
            }
            for (var r : regions.findAll()) {
                AppUser u = user("region." + r.getCode() + "@" + props.emailDomain(), defaultHash, Role.REGIONAL);
                u.setRegion(r);
                users.save(u);
            }
        });
        log.info("Comptes créés au premier démarrage : 1 admin ({}), {} provinces, {} régions. Changement de mot de passe obligatoire à la première connexion.",
                props.adminEmail(), provinces.count(), regions.count());
    }

    private AppUser user(String email, String hash, Role role) {
        AppUser u = new AppUser();
        u.setEmail(email.toLowerCase());
        u.setPasswordHash(hash);
        u.setRole(role);
        u.setEnabled(true);
        u.setMustChangePassword(true);
        return u;
    }
}
