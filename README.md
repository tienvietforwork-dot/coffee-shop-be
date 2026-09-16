# Database backups

Automated full pg_dump snapshots of the shared Neon database (schema + data).
Produced weekly by .github/workflows/backup.yml on the main branch. Restore with:
```
psql "<connection-string>" -f backups/<file>.sql
```
