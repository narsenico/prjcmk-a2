package it.amonshore.comikkua.data.release;

import android.app.Application;
import android.os.Bundle;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Size;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import it.amonshore.comikkua.ICallback;
import it.amonshore.comikkua.LogHelper;
import it.amonshore.comikkua.Utility;

public class ReleaseViewModel extends AndroidViewModel {

    private ReleaseRepository mRepository;
    private LiveData<List<IReleaseViewModelItem>> mReleaseViewModelItems;

    // lo uso per salvare gli stati delle viste (ad esempio la posizione dello scroll di una lista in un fragment)
    public final Bundle states;
    private ReleaseViewModelGroupHelper mGroupHelper;

    public ReleaseViewModel(Application application) {
        super(application);
        mRepository = new ReleaseRepository(application);
        mGroupHelper = new ReleaseViewModelGroupHelper();
        states = new Bundle();
    }

    public LiveData<List<ComicsRelease>> getAllReleases(@NonNull @Size(8) String refDate,
                                                        @NonNull @Size(8) String refNextDate,
                                                        @NonNull @Size(8) String refOtherDate,
                                                        long retainStart) {
        return mRepository.getComicsReleases(refDate, refNextDate, refOtherDate, retainStart);
    }

    public LiveData<List<ComicsRelease>> getAllReleases(long comicsId) {
        return mRepository.getComicsReleasesByComicsId(comicsId);
    }

    public LiveData<List<IReleaseViewModelItem>> getReleaseViewModelItems() {
        if (mReleaseViewModelItems == null) {
            mReleaseViewModelItems = createReleaseViewModelItems();
        }
        return mReleaseViewModelItems;
    }

    private LiveData<List<IReleaseViewModelItem>> createReleaseViewModelItems() {
        final Calendar calendar = Calendar.getInstance(Locale.getDefault());
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        LogHelper.d("createReleaseViewModelItems today=%s", dateFormat.format(calendar.getTime()));

        // il giorno di riferimento è il primo giorno della settimana in corso
        Utility.gotoFirstDayOfWeek(calendar);
        final String refDate = dateFormat.format(calendar.getTime());
        // la settimana dopo
        calendar.add(Calendar.DAY_OF_MONTH, 7);
        final String refNextDate = dateFormat.format(calendar.getTime());
        // la settimana dopo
        calendar.add(Calendar.DAY_OF_MONTH, 7);
        final String refOtherDate = dateFormat.format(calendar.getTime());
        // per quanto riguarda le release precedenti estraggo anche quelle aquistate dal giorno prima (rispetto al corrente)
        //  (quelle successive verrebbero cmq estratte in quanto fanno parte del "periodo corrente")
        final long retainStart = System.currentTimeMillis() - 86400000;

        LogHelper.d("prepare notable releases refDate=%s, refNextDate=%s, refOtherDate=%s retainStart=%s",
                refDate, refNextDate, refOtherDate, retainStart);

        // ogni volta che i dati sottostanti cambiano li riorganizzo aggiungendo gli header di gruppo e raggruppando le release quando necessario
        // quindi aggiorno mediatorLiveData
        final MediatorLiveData<List<IReleaseViewModelItem>> mediatorLiveData = new MediatorLiveData<>();
        mediatorLiveData.addSource(getAllReleases(refDate, refNextDate, refOtherDate, retainStart), comicsReleases -> {
            // e raggruppando le release senza data
            mediatorLiveData.setValue(mGroupHelper.createViewModelItems(comicsReleases, MissingRelease.TYPE));
        });
        return mediatorLiveData;
    }

    public LiveData<List<IReleaseViewModelItem>> getReleaseViewModelItems(long comicsId) {

        LogHelper.d("prepare releases for detail comicsId=%s",
                comicsId);

        // ogni volta che i dati sottostanti cambiano li riorganizzo aggiungendo gli header di gruppo e raggruppando le release quando necessario
        // quindi aggiorno mediatorLiveData
        final MediatorLiveData<List<IReleaseViewModelItem>> mediatorLiveData = new MediatorLiveData<>();
        mediatorLiveData.addSource(getAllReleases(comicsId), comicsReleases -> {
            // TODO: si potrebbero unire tutti i purchased, ma poi al click si devono espandere
            mediatorLiveData.setValue(mGroupHelper.createViewModelItems(comicsReleases, 0));
        });
        return mediatorLiveData;
    }

    public LiveData<Release> getRelease(long id) {
        return mRepository.getRelease(id);
    }

    public LiveData<List<ComicsRelease>> getOneTimeComicsReleases(Iterable<Long> ids) {
        return mRepository.getOneTimeComicsReleases(Utility.toArray(ids));
    }

    public void insert(Release release) {
        mRepository.insert(release);
    }

    public long insertSync(Release release) {
        return mRepository.insertSync(release);
    }

    public Long[] insertSync(Release... release) {
        return mRepository.insertSync(release);
    }

    public void update(Release release) {
        mRepository.update(release);
    }

    public void updatePurchased(boolean purchased, Iterable<Long> ids) {
        updatePurchased(purchased, Utility.toArray(ids));
    }

    public void updatePurchased(boolean purchased, Long... ids) {
        mRepository.updatePurchased(purchased, System.currentTimeMillis(), ids);
    }

    public void togglePurchased(Iterable<Long> ids) {
        togglePurchased(Utility.toArray(ids));
    }

    public void togglePurchased(Long... ids) {
        mRepository.togglePurchased(System.currentTimeMillis(), ids);
    }

    public void updateOrdered(boolean ordered, Iterable<Long> ids) {
        updateOrdered(ordered, Utility.toArray(ids));
    }

    public void updateOrdered(boolean ordered, Long... ids) {
        mRepository.updateOrdered(ordered, System.currentTimeMillis(), ids);
    }

    public void toggleOrdered(Iterable<Long> ids) {
        toggleOrdered(Utility.toArray(ids));
    }

    public void toggleOrdered(Long... ids) {
        mRepository.toggleOrdered(System.currentTimeMillis(), ids);
    }

    public void delete(Iterable<Long> ids) {
        mRepository.delete(Utility.toArray(ids));
    }

    public void delete(Long... ids) {
        mRepository.delete(ids);
    }

    public int deleteByNumberSync(long comicsId, int... number) {
        return mRepository.deleteByNumberSync(comicsId, number);
    }

    public void remove(Iterable<Long> ids, ICallback<Integer> callback) {
        remove(Utility.toArray(ids), callback);
    }

    public void remove(Long... ids) {
        mRepository.remove(ids);
    }

    public void remove(Long id, ICallback<Integer> callback) {
        remove(new Long[] { id }, callback);
    }

    public void remove(Long[] ids, ICallback<Integer> callback) {
        mRepository.remove(ids, callback);
    }

    public void undoRemoved() {
        mRepository.undoRemoved();
    }

    public void deleteRemoved() {
        mRepository.deleteRemoved();
    }
}
