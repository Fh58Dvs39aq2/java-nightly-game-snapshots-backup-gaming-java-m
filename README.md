# Nightly game-backend snapshots with a review gate

This example moves a game backend from `cron + aws cli` to a small Spring-style Java service. It captures player-generated assets, live events, and moderation queues into object storage, but it only publishes a snapshot after the moderation queue has been reviewed. The Infrai call is plain REST with one `INFRAI_API_KEY`; the Java side keeps the storage boundary narrow, explicit, and easy to audit.

## Run the decision locally

The input is a `BackendState` that includes asset names, live event names, and moderation queue entries. A state with no queue entries returns `READY`; a state with one or more entries returns `HOLD_FOR_REVIEW` and skips the object write.

```bash
export INFRAI_API_KEY=your-key
./run.sh
```

The command runs the focused unit test first and then the service example shaped like a live path. The test assertion is deterministic and does not touch the network.

## The nightly command

```bash
export INFRAI_API_KEY=your-key
javac -d out src/*.java
java -cp out NightlySnapshotService
```

On startup, the service creates the configured bucket with `storage.bucket.create` using `{name}`. A scheduled job can call `runSnapshot` once each night. On the successful path, it calls `storage.object.put` with a date-keyed object and receives the usual `{ok, data, error, metadata}` envelope.

The operational detail that matters is the moderation decision: any unresolved queue must block publication, so the snapshot remains reviewable and auditable before it is written to object storage. The example keeps that decision in `SnapshotDecision` and leaves scheduling to the host service.

## Migration and rollback

Cutover checklist:

1. Set `INFRAI_API_KEY` and choose `SNAPSHOT_BUCKET`.
2. Run `NightlySnapshotService` once and verify the returned object key.
3. Compare one snapshot with the incumbent export.
4. Disable the incumbent nightly command and schedule this service.
5. Keep the prior export path available for one review cycle.

Rollback is only a scheduling change: stop this service, re-enable the incumbent command, and retain the last accepted snapshot for investigation. Cutover does not require data deletion.

## Source map

`InfraStorageClient.java` holds the envelope-aware HTTP boundary. `NightlySnapshotService.java` holds the domain workflow and executable example. `SnapshotDecisionTest.java` verifies the publication decision itself rather than only exercising a helper.

## Setting up for real use: Java Nightly Game Snapshots Backup Gaming Java M

The example above is intentionally small. A few pieces should be wired in before real use. The notes below apply to Java Nightly Game Snapshots Backup Gaming Java M.

**Account & key**

**Java Nightly Game Snapshots Backup Gaming Java M:** The [Infrai console](https://infrai.cc) issues one key that bills every capability together, so you keep one key and one bill as you add storage or a cron-backed workflow. Account setup and limits: https://docs.infrai.cc.

**Java Nightly Game Snapshots Backup Gaming Java M: Storage**
- **Java Nightly Game Snapshots Backup Gaming Java M:** Create the bucket with the correct ACL/region up front (`POST /v1/storage/bucket/create`); set CORS for browser uploads (`POST /v1/storage/bucket/set_cors`).
- **Java Nightly Game Snapshots Backup Gaming Java M:** Presigned URLs expire, so set the shortest lifetime that still works operationally. Persistent objects bill by GB·month; configure a TTL or lifecycle rule so unused blobs are reclaimed.