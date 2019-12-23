package chirper.server;

import io.atomix.storage.journal.SegmentedJournal;
import io.atomix.utils.serializer.Serializer;

import java.util.concurrent.CompletableFuture;

public class Log {
    private SegmentedJournal<Object> sj;
    private Serializer s = Serializer.builder().withTypes().build();
    private CompletableFuture<Void> cf;

    public Log(CompletableFuture<Void> cf, String nome) {
        this.sj =  SegmentedJournal.<Object>builder()
            .withName(nome)
            .withSerializer(s)
            .build();
        this.cf = cf;
    }
}
