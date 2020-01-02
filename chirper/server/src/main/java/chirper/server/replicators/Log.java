package chirper.server.replicators;

import io.atomix.storage.journal.SegmentedJournal;
import io.atomix.storage.journal.SegmentedJournalReader;
import io.atomix.storage.journal.SegmentedJournalWriter;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class Log<T> {

    private SegmentedJournal<Object> sj;

    private Serializer s;

    private SegmentedJournalWriter<Object> w;

    public Log(String name, int id,Class<T> type)
    {
        this.s = Serializer.builder().withTypes(type, Prepared.class, Commit.class, ServerId.class,
            Address.class, Set.class).build();

        this.sj =  SegmentedJournal.<Object>builder()
            .withName(name+id)
            .withSerializer(s)
            .build();

        this.w = sj.writer();
    }

    public void add(Object o)
    {
        w.append(o);
        CompletableFuture.supplyAsync(()->{
            w.flush();
            return null;
        });
    }

    public Object get()
    {
        SegmentedJournalReader<Object> r = sj.openReader(0);
        Object aux = null;
        Object o = null;
        while(r.hasNext()) {
            aux = o;
            o = r.next().entry();
        }
        r.close();
        return aux;
    }

    public void remove(int i)
    {
        w.truncate(w.getLastIndex()-i);
        CompletableFuture.supplyAsync(()->{
            w.flush();
            return null;
        });
    }

}
