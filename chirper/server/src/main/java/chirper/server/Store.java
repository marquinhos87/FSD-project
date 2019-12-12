package chirper.server;

import io.atomix.storage.journal.SegmentedJournal;
import io.atomix.storage.journal.SegmentedJournalReader;
import io.atomix.storage.journal.SegmentedJournalWriter;

import java.nio.file.Path;

public class Store
{
    public Store(Path stateDirectory)
    {

    }

    public CoordinatorHandle prepareAsCoordinator(String chirp)
    {

    }

    public void commitAsCoordinator(CoordinatorHandle handle)
    {

    }

    public ParticipantHandle prepareAsParticipant(String chirp)
    {

    }

    public void commitAsParticipant(ParticipantHandle handle)
    {

    }



    private final SegmentedJournal< String > journal;
    private final SegmentedJournalWriter< String > writer;

    public void addChirp(String chirp)
    {
        final var entry = writer.append(chirp);

        writer.flush();
    }
}
