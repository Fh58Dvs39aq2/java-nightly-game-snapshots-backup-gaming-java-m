import java.time.LocalDate;
import java.util.List;

public final class SnapshotDecisionTest {
    public static void main(String[] args) throws Exception {
        NightlySnapshotService.BackendState clear = new NightlySnapshotService.BackendState(List.of("asset"), List.of("event"), List.of());
        NightlySnapshotService.BackendState queued = new NightlySnapshotService.BackendState(List.of(), List.of(), List.of("case-7"));
        assert NightlySnapshotService.decide(clear) == NightlySnapshotService.SnapshotDecision.READY;
        assert NightlySnapshotService.decide(queued) == NightlySnapshotService.SnapshotDecision.HOLD_FOR_REVIEW;
        System.out.println("snapshot decision test passed");
    }
}
