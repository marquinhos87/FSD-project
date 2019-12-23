package chirper.server;

import io.atomix.storage.journal.Indexed;
import io.atomix.storage.journal.SegmentedJournal;
import io.atomix.storage.journal.SegmentedJournalReader;
import io.atomix.storage.journal.SegmentedJournalWriter;
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

    public void add(Object o) {
        SegmentedJournalWriter<Object> w = sj.writer();
        w.append(o);
        CompletableFuture.supplyAsync(()->{w.flush();return null;})
            .thenRun(()->{
                w.close();
            });
    }

    public Object get() {
        SegmentedJournalReader<Object> r = sj.openReader(2);
        Object aux = null;
        Object o = null;
        while(r.hasNext()) {
            aux = o;
            o = r.next().entry();
        }
        r.close();
        return aux;
    }
}
