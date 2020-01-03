/* -------------------------------------------------------------------------- */

package chirper.server.broadcast;

import chirper.server.network.ServerId;
import io.atomix.storage.journal.SegmentedJournal;
import io.atomix.storage.journal.SegmentedJournalReader;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;

import java.util.Set;

/* -------------------------------------------------------------------------- */

public class Log<T>
{
    private SegmentedJournal<T> journal;

    public Log(String name, int id, Class<T> entryType)
    {
        final var serializer =
            Serializer
                .builder()
                .withTypes(
                    entryType, Prepared.class, Commit.class, ServerId.class,
                    Address.class, Set.class
                ).build();

        this.journal =
            SegmentedJournal
                .<T>builder()
                .withName(name + id)
                .withSerializer(serializer)
                .build();
    }

    public SegmentedJournalReader<T> getReader()
    {
        return this.journal.openReader(0);
    }

    public void appendEntry(T entry)
    {
        journal.writer().append(entry);
        journal.writer().flush();
    }

//    public T getEntry()
//    {
//        SegmentedJournalReader<T> r = journal.openReader(0);
//        T aux = null;
//        T o = null;
//        while(r.hasNext()) {
//            aux = o;
//            o = r.next().entry();
//        }
//        r.close();
//        return aux;
//    }
//
//    public void keepLast(int count)
//    {
//        journal.writer().truncate(journal.writer().getLastIndex() - count);
//        journal.writer().flush();
//    }
}

/* -------------------------------------------------------------------------- */
