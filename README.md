# Device status simulator

The simulator sends one complete digital-door snapshot per device to `POST /status`.
The endpoint accepts only a device credential in the `Authorization: Bearer <token>`
header. The device and its tenant are determined from the credential; the snapshot
does not supply either ID. User sessions cannot send device status.

The sample data creates two simulator devices, one in each test tenant, with
credential hashes for this local setup. The matching tokens are in the
Git-ignored `simulator-credentials.local.ps1` file. In PowerShell, load them and
start the simulator:

```powershell
. .\simulator-credentials.local.ps1
node simulator.js
```

The local credential file is not shared with other clones. To provision a new
setup, generate a unique 32-byte random token per device. For example, in
PowerShell generate a token for device A and print its SHA-256 hash:

```powershell
$env:SIMULATOR_TOKEN_A = node -e "console.log(require('node:crypto').randomBytes(32).toString('base64url'))"
node -e "console.log(require('node:crypto').createHash('sha256').update(process.env.SIMULATOR_TOKEN_A).digest('hex'))"
```

In the database, set `devices.credential_hash` to that printed hash for device
`d06c72ee-56f1-4b6a-9a7a-18b58e36a111`. Repeat with `SIMULATOR_TOKEN_B` and
device `d06c72ee-56f1-4b6a-9a7a-18b58e36a222` to enable the other device.
For example, execute this SQL in your database client, substituting the hash:

```sql
UPDATE devices SET credential_hash = '<printed SHA-256 hash>'
WHERE id = 'd06c72ee-56f1-4b6a-9a7a-18b58e36a111';
```

Set the hash to `NULL` to revoke a credential, or replace it to rotate the
credential. Never commit tokens or use predictable credentials. Database hashes
should also be treated as sensitive.

Run `node simulator.js` in the same shell; only devices with a token set are
enabled. Set `BACKEND_URL` if the backend is not at `http://localhost:8080` and
`SIMULATOR_PORT` to change the listener port (default 3000). Use HTTPS outside
local development to protect bearer credentials. The current development
configuration uses `ddl-auto=create-drop`, so database-only credential changes
must be reapplied after a backend restart, or the seed hashes updated.
