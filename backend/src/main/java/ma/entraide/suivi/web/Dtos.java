package ma.entraide.suivi.web;

import jakarta.validation.constraints.*;
import ma.entraide.suivi.domain.Role;
import ma.entraide.suivi.service.DedupService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class Dtos {
    private Dtos() {}

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}
    public record ChangePasswordRequest(@NotBlank String currentPassword, @NotBlank String newPassword) {}
    public record Me(Long id, String email, Role role, boolean mustChangePassword, Long provinceId, String provinceNomFr,
                     String provinceNomAr, Long regionId, String regionNomFr, String regionNomAr) {}

    public record RegionDto(Long id, String code, String nomFr, String nomAr) {}
    public record ProvinceDto(Long id, String code, Long regionId, String nomFr, String nomAr) {}
    public record CategoryDto(Long id, String code, String nomFr, String nomAr) {}
    public record RefData(List<RegionDto> regions, List<ProvinceDto> provinces, List<CategoryDto> categories) {}

    public record AssociationRequest(@NotNull Long provinceId, @NotBlank @Size(max = 255) String nomAr,
                                     @NotEmpty List<Long> categoryIds, Boolean actif) {}
    public record AssociationDto(Long id, Long provinceId, String provinceNomFr, String provinceNomAr, String nomAr,
                                 boolean actif, List<Long> categoryIds, long beneficiaryCount) {}

    public record BeneficiaryRequest(
            @NotNull Long associationId,
            @NotBlank @Size(max = 120) String nomFr, @NotBlank @Size(max = 120) String prenomFr,
            @NotBlank @Size(max = 120) String nomAr, @NotBlank @Size(max = 120) String prenomAr,
            @NotNull @PastOrPresent LocalDate dateNaissance,
            @NotNull @Pattern(regexp = "[FM]") String sexe,
            @Size(max = 20) String cin, boolean confirmDuplicate) {}
    public record BeneficiaryDto(Long id, Long associationId, String associationNom, Long provinceId, String provinceNomFr,
                                 String provinceNomAr, String nomFr, String prenomFr, String nomAr, String prenomAr,
                                 LocalDate dateNaissance, String sexe, String cin) {}
    public record PageDto<T>(List<T> items, long total, int page, int size) {}
    public record CheckRequest(@NotBlank String nomFr, @NotBlank String prenomFr, @NotNull LocalDate dateNaissance, Long excludeId) {}

    public record UserDto(Long id, String email, Role role, String provinceNomFr, String regionNomFr, boolean enabled,
                          boolean mustChangePassword, LocalDateTime lastLogin) {}
    public record EnabledRequest(boolean enabled) {}
    public record AuditDto(Long id, LocalDateTime at, String userEmail, String action, String entity, Long entityId, String details) {}

    public record Count(String label, String labelAr, long count) {}
    public record Month(String month, long count) {}
    public record LatestDto(Long id, String nomFr, String prenomFr, String sexe, String associationNom) {}
    public record Stats(long total, long girls, long boys, long associations, long provinces, List<Count> byProvince,
                        List<Count> byAssociation, List<Count> ageBands, List<Month> monthly,
                        List<Count> associationsByCategory, List<LatestDto> latest) {}

    public record DuplicateResponse(String code, DedupService.Result result) {}
}
