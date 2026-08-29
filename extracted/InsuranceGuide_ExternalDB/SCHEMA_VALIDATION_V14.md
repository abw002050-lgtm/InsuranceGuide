# Schema validation — V14

The bundled `Domw` SQLite database was checked directly. Every column referenced by the current repositories exists in the actual database tables:

- EMP: OK
- Pension: OK
- BANKS: OK
- BRANCHES: OK
- GOVS: OK
- POST: OK
- PHONE: OK
- LAWS: OK
- LAWP: OK

The law/procedure paths were also verified to match the copied local `assets/content/proc` structure.
