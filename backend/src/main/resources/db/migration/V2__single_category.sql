-- Une association n'a plus qu'une seule catégorie (choix unique).
-- Les associations chargées depuis l'Excel n'en ont pas : la colonne reste nullable, mais l'API exige une catégorie à la saisie.
ALTER TABLE associations ADD COLUMN category_id BIGINT NULL;
ALTER TABLE associations ADD CONSTRAINT fk_asso_category FOREIGN KEY (category_id) REFERENCES categories(id);

-- Reprise des données éventuelles : on conserve la première catégorie déjà choisie.
UPDATE associations a
   SET category_id = (SELECT MIN(ac.category_id) FROM association_categories ac WHERE ac.association_id = a.id);

DROP TABLE association_categories;
CREATE INDEX idx_asso_category ON associations (category_id);
