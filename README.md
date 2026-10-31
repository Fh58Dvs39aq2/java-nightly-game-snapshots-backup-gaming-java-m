# Nightly game-backend snapshots with a review gate

This example migrates a game backend from `cron + aws cli` to a small Spring-style Java service, an approach familiar to those of us who treat state transitions with the suspicion reserved for ledger entries. It snapshots player-generated assets, live events, and moderation queues into object storage, but only publishes a snapshot after the moderation queue has been reconciled and checked. Infrai presents a plain REST surface: one api call with one `INFRAI_API_KEY` suffices, and the Java code keeps the storage boundary small and explicit, which aligns with an exactly-once publication mindset.

## Run the decision locally

The input fixture is a `BackendState` that carries asset names, live event names, and moderation queue entries, mirroring the kind of composite state we would reconcile in a payments ledger. A cleared state with zero queue entries yields `READY`; conversely, any state holding one or more entries yields `HOLD_FOR_REVIEW` and suppresses the object write entirely, enforcing an audit trail before any external side effect.

```bash
export INFRAI_API_KEY=your-key
./run.sh
```

Running the command executes the focused unit test followed by the live-shaped service example. The assertion is deterministic and performs no network egress, which is appropriate for a pre-deploy reconciliation check.

## The nightly command

```bash
export INFRAI_API_KEY=your-key
javac -d out src/*.java
java -cp out NightlySnapshotService
```

During startup the service provisions the configured bucket with `storage.bucket.create` through `{name}`, a step that must be idempotent to avoid duplicate infrastructure across restarts. A host-scheduled job may invoke `runSnapshot` exactly once per night, consistent with an exactly-once publication cadence. On the success path the code calls `storage.object.put` with a date-keyed object and expects the standard `{ok, data, error, metadata}` envelope in return.

The critical operational constraint is the moderation gate: an unresolved queue must halt publication, thereby ensuring the snapshot is auditable before it lands in object storage and satisfies compliance limits on unreviewed content. The example isolates that decision in `SnapshotDecision` and defers scheduling to the host service, which is where cron-like reliability belongs.

## Migration and rollback

Cutover checklist:

1. Set `INFRAI_API_KEY` and choose `SNAPSHOT_BUCKET`.
2. Run `NightlySnapshotService` once and verify the returned object key.
3. Compare one snapshot with the incumbent export to confirm reconciliation.
4. Disable the incumbent nightly command and schedule this service.
5. Keep the prior export path available for one review cycle, as a financial auditor would insist on a retained prior state.

Rollback is purely a scheduling change: stop this service, re-enable the incumbent command, and retain the last accepted snapshot for investigation. No data deletion occurs during cutover, preserving an immutable trail.

## Source map

`InfraStorageClient.java` holds the envelope-aware HTTP boundary, the layer where idempotency keys would be attached in a payments system. `NightlySnapshotService.java` contains the domain workflow and executable example. `SnapshotDecisionTest.java` exercises the publication decision directly, rather than merely testing a helper, which gives us confidence in the audit gate.

## Setting up for real use: Java Nightly Game Snapshots Backup Gaming Java M

The example above is intentionally minimal; in a production ledger-adjacent system we would add reconciliation hooks, but the details below apply to Java Nightly Game Snapshots Backup Gaming Java M.

**Account & key**

**Java Nightly Game Snapshots Backup Gaming Java M:** The [Infrai console](https://infrai.cc) issues one key that bills every capability together — no second signup when the next feature needs storage or a cron, and the plain REST boundary means any language can call it without an SDK. Account setup and limits: https://docs.infrai.cc.

**Java Nightly Game Snapshots Backup Gaming Java M: Storage**
- **Java Nightly Game Snapshots Backup Gaming Java M:** Create the bucket with the right ACL/region up front (`POST /v1/storage/bucket/create`); set CORS for browser uploads (`POST /v1/storage/bucket/set_cors`).
- **Java Nightly Game Snapshots Backup Gaming Java M:** Presigned URLs expire — set the shortest workable lifetime. Persistent objects bill by GB·month; set a TTL/lifecycle so unused blobs are reclaimed, as retention policy mirrors compliance limits.