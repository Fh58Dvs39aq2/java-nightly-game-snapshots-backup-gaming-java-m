import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class NightlySnapshotService {
    public record BackendState(List<String> playerAssets, List<String> liveEvents, List<String> moderationQueue) {}
    public enum SnapshotDecision { READY, HOLD_FOR_REVIEW }
    public record SnapshotResult(SnapshotDecision decision, String objectKey) {}

    private final InfraStorageClient storage;
    private final String bucket;

    public NightlySnapshotService(InfraStorageClient storage, String bucket) {
        this.storage = Objects.requireNonNull(storage);
        this.bucket = Objects.requireNonNull(bucket);
    }

    public SnapshotResult runSnapshot(BackendState state, LocalDate date) throws Exception {
        SnapshotDecision decision = decide(state);
        String key = "nightly/" + date + ".json";
        if (decision == SnapshotDecision.HOLD_FOR_REVIEW) {
            return new SnapshotResult(decision, key);
        }
        String payload = "{\"player_assets\":[" + strings(state.playerAssets()) + "],\"live_events\":[" + strings(state.liveEvents()) + "],\"moderation_queue\":[]}";
        storage.putSnapshot(bucket, key, payload);
        return new SnapshotResult(decision, key);
    }

    static SnapshotDecision decide(BackendState state) {
        return state.moderationQueue().isEmpty() ? SnapshotDecision.READY : SnapshotDecision.HOLD_FOR_REVIEW;
    }

    public static void main(String[] args) throws Exception {
        String key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) throw new IllegalStateException("INFRAI_API_KEY is required");
        String bucket = System.getenv().getOrDefault("SNAPSHOT_BUCKET", "game-nightly-snapshots");
        InfraStorageClient storage = new InfraStorageClient(key);
        storage.createBucket(bucket);
        NightlySnapshotService service = new NightlySnapshotService(storage, bucket);
        BackendState state = new BackendState(List.of("skin-42"), List.of("winter-arena"), List.of());
        System.out.println(service.runSnapshot(state, LocalDate.now()));
    }

    private static String strings(List<String> values) {
        return values.stream().map(value -> "\"" + value.replace("\"", "\\\"") + "\"").collect(Collectors.joining(","));
    }
}
