CREATE TABLE regions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(60) NOT NULL UNIQUE,
  nom_fr VARCHAR(150) NOT NULL,
  nom_ar VARCHAR(150)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE provinces (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(80) NOT NULL UNIQUE,
  region_id BIGINT NOT NULL,
  nom_fr VARCHAR(150) NOT NULL,
  nom_ar VARCHAR(150),
  CONSTRAINT fk_province_region FOREIGN KEY (region_id) REFERENCES regions(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE categories (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(60) NOT NULL UNIQUE,
  nom_fr VARCHAR(200) NOT NULL,
  nom_ar VARCHAR(200) NOT NULL,
  actif BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE associations (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  province_id BIGINT NOT NULL,
  nom_ar VARCHAR(255) NOT NULL,
  actif BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(3) NOT NULL,
  updated_at DATETIME(3) NOT NULL,
  CONSTRAINT fk_asso_province FOREIGN KEY (province_id) REFERENCES provinces(id),
  CONSTRAINT uq_asso_province_nom UNIQUE (province_id, nom_ar)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE association_categories (
  association_id BIGINT NOT NULL,
  category_id BIGINT NOT NULL,
  PRIMARY KEY (association_id, category_id),
  CONSTRAINT fk_ac_asso FOREIGN KEY (association_id) REFERENCES associations(id) ON DELETE CASCADE,
  CONSTRAINT fk_ac_cat FOREIGN KEY (category_id) REFERENCES categories(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE beneficiaires (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  association_id BIGINT NOT NULL,
  nom_ar VARCHAR(120) NOT NULL,
  prenom_ar VARCHAR(120) NOT NULL,
  nom_fr VARCHAR(120) NOT NULL,
  prenom_fr VARCHAR(120) NOT NULL,
  date_naissance DATE NOT NULL,
  sexe VARCHAR(1) NOT NULL,
  cin VARCHAR(20) NULL,
  dedup_key VARCHAR(300) NOT NULL,
  search_text VARCHAR(600) NOT NULL,
  statut VARCHAR(10) NOT NULL DEFAULT 'ACTIF',
  created_at DATETIME(3) NOT NULL,
  updated_at DATETIME(3) NOT NULL,
  created_by VARCHAR(120),
  updated_by VARCHAR(120),
  CONSTRAINT fk_benef_asso FOREIGN KEY (association_id) REFERENCES associations(id),
  CONSTRAINT uq_benef_cin UNIQUE (cin),
  CONSTRAINT ck_benef_sexe CHECK (sexe IN ('F','M'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_benef_dedup ON beneficiaires (dedup_key);
CREATE INDEX idx_benef_dob ON beneficiaires (date_naissance);
CREATE INDEX idx_benef_asso ON beneficiaires (association_id, statut);
CREATE INDEX idx_benef_created ON beneficiaires (created_at);

CREATE TABLE users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(190) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  role VARCHAR(10) NOT NULL,
  province_id BIGINT NULL,
  region_id BIGINT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  must_change_password BOOLEAN NOT NULL DEFAULT TRUE,
  failed_attempts INT NOT NULL DEFAULT 0,
  locked_until DATETIME(3) NULL,
  last_login DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL,
  CONSTRAINT fk_user_province FOREIGN KEY (province_id) REFERENCES provinces(id),
  CONSTRAINT fk_user_region FOREIGN KEY (region_id) REFERENCES regions(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE audit_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  at DATETIME(3) NOT NULL,
  user_email VARCHAR(190),
  action VARCHAR(60) NOT NULL,
  entity VARCHAR(40),
  entity_id BIGINT,
  details TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_audit_at ON audit_log (at);
