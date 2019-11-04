package it.amonshore.comikkua.data;

import android.annotation.SuppressLint;
import android.app.Application;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Size;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.paging.PagedList;
import it.amonshore.comikkua.BuildConfig;
import it.amonshore.comikkua.LogHelper;
import it.amonshore.comikkua.Utility;

public class ReleasesViewModel extends AndroidViewModel {

    private ReleaseRepository mRepository;

    public ReleasesViewModel(Application application) {
        super(application);
        mRepository = new ReleaseRepository(application);
    }

    LiveData<List<Release>> getReleases(int comicsId) {
        return mRepository.getReleases(comicsId);
    }

    @SuppressLint("Assert")
    public LiveData<List<ComicsRelease>> getAllReleases(@NonNull @Size(6) String refDate, long retainStart) {
        return mRepository.getAllReleases(refDate, retainStart);
    }

    public LiveData<List<ComicsRelease>> getNotableReleases() {
        final Calendar calendar = Calendar.getInstance(Locale.getDefault());
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        LogHelper.d("today=%s", dateFormat.format(calendar.getTime()));

        // il giorno di riferimento è il primo giorno della settimana in corso
        Utility.gotoFirstDayOfWeek(calendar);
        final String refDate = dateFormat.format(calendar.getTime());
        // per quanto riguarda le release precedenti estraggo anche quelle aquistate dal giorno prima (rispetto al corrente)
        //  (quelle successive verrebbero cmq estratte in quanto fanno parte del "periodo corrente")
        final long retainStart = System.currentTimeMillis() - 86400000;

        LogHelper.d("prepare notable release refDate=%s, retainStart=%s", refDate, retainStart);

        // ogni volta che i dati sottostanti cambiano li riorganizzo aggiungendo gli header di gruppo e raggruppando le release quando necessario
        // quindi aggiorno mediatorLiveData
        final MediatorLiveData<List<ComicsRelease>> mediatorLiveData = new MediatorLiveData<>();
        mediatorLiveData.addSource(getAllReleases(refDate, retainStart), comicsReleases -> {
            // TODO: è qua che posso manipolare il risultato aggiungendo gli header
            // e raggruppando le release senza data
            mediatorLiveData.setValue(comicsReleases);
        });
        return mediatorLiveData;
    }

    public void insert(Release release) {
        mRepository.insert(release);
    }

    public void update(Release release) {
        mRepository.update(release);
    }

    public void delete(Iterable<Long> ids) {
        mRepository.delete(Utility.toArray(ids));
    }

}
