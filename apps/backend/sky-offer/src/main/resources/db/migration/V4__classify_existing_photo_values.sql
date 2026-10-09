UPDATE offer
SET photo_object_key = external_photo_url
WHERE external_photo_url LIKE 'offers/' || id::text || '/%';

UPDATE offer
SET external_photo_url = NULL
WHERE external_photo_url IS NOT NULL
  AND external_photo_url NOT LIKE 'http://%'
  AND external_photo_url NOT LIKE 'https://%';
