-- Repoint assets from duplicate folder rows to the oldest (lowest folder_id) row per path,
-- then remove the duplicates and enforce uniqueness so this can't recur.
-- assets.folder_id has ON DELETE CASCADE, so duplicates must be repointed before deletion.

UPDATE assets a
SET folder_id = canonical.folder_id
FROM (
    SELECT path, MIN(folder_id) AS folder_id
    FROM folders
    GROUP BY path
) canonical
JOIN folders dup ON dup.path = canonical.path
WHERE a.folder_id = dup.folder_id
  AND dup.folder_id <> canonical.folder_id;

DELETE FROM folders dup
USING (
    SELECT path, MIN(folder_id) AS folder_id
    FROM folders
    GROUP BY path
) canonical
WHERE dup.path = canonical.path
  AND dup.folder_id <> canonical.folder_id;

ALTER TABLE folders ADD CONSTRAINT uq_folders_path UNIQUE (path);
