package it.amonshore.comikkua;

import android.content.Context;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import it.amonshore.comikkua.data.Comics;
import it.amonshore.comikkua.data.ComicsDao;
import it.amonshore.comikkua.data.ComicsWithReleases;
import it.amonshore.comikkua.data.ComikkuDatabase;
import it.amonshore.comikkua.data.Release;
import it.amonshore.comikkua.data.ReleaseDao;

import static org.junit.Assert.*;

/**
 * NOTA BENE: i metodi devono essere pubblici altrimenti viene scatenato un errore in esecuzione
 */

@RunWith(AndroidJUnit4.class)
public class ComikkuDaoTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private ComikkuDatabase mDatabase;
    private ComicsDao mComicsDao;
    private ReleaseDao mReleaseDao;

    @Before
    public void init() {
        LogHelper.setTag("CMKTEST");

        final Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        mDatabase = Room.inMemoryDatabaseBuilder(appContext, ComikkuDatabase.class).build();

        mReleaseDao = mDatabase.releaseDao();
        mComicsDao = mDatabase.comicsDao();

//        mReleaseDao.deleteAll();
//        mComicsDao.deleteAll();
//        Comics[] list = new Comics[100];
//        for (int ii=0; ii<list.length; ii++) {
//            list[ii] = Comics.create(String.format("Comics #%s", ii+1));
//        }
//        mComicsDao.insert(list);
//
//        List<Comics> comics = mComicsDao.getRawComics();
//        Release[] releases = new Release[comics.size()];
//        for (int ii=0; ii<releases.length; ii++) {
//            releases[ii] = Release.create(comics.get(ii).id, (int)(Math.random() * 10));
//        }
//        mReleaseDao.insert(releases);
    }

    @After
    public void close() {
        mDatabase.close();
    }

    @Test
    public void testComicsWithReleases() throws InterruptedException {
        LiveData<List<ComicsWithReleases>> ld = mComicsDao.getComicsWithReleases();
        CountDownLatch latch = new CountDownLatch(1);

        ld.observeForever(new Observer<List<ComicsWithReleases>>() {
            @Override
            public void onChanged(List<ComicsWithReleases> comicsWithReleases) {
                latch.countDown();
                ld.removeObserver(this);

                assertTrue(comicsWithReleases.size() > 0);

                for (ComicsWithReleases cr : comicsWithReleases) {
                    assertTrue(cr.releases.size() > 0);
                }
            }
        });

        latch.await(2, TimeUnit.SECONDS);
    }

    @Test
    public void updateComicsWithReleases() throws InterruptedException {
        LiveData<List<ComicsWithReleases>> ld = mComicsDao.getComicsWithReleasesByName("Comics #1");
        CountDownLatch latch = new CountDownLatch(2);

        Observer<List<ComicsWithReleases>> observer;
        ld.observeForever(observer = new Observer<List<ComicsWithReleases>>() {
            @Override
            public void onChanged(List<ComicsWithReleases> comicsWithReleases) {
                latch.countDown();

                assertTrue(comicsWithReleases.size() == 1);

                if (latch.getCount() == 1) {
                    assertTrue(comicsWithReleases.get(0).releases.size() == 1);

                    Comics comics = comicsWithReleases.get(0).comics;

                    LiveData<List<Release>> lr = mReleaseDao.getReleases(comics.id);
                    lr.observeForever(new Observer<List<Release>>() {
                        @Override
                        public void onChanged(List<Release> releases) {
                            lr.removeObserver(this);

                            comics.publisher = "XXX";
                            mComicsDao.update(comics);
                        }
                    });

                    mReleaseDao.insert(Release.create(comics.id, 999));
                } else {
                    assertTrue(comicsWithReleases.get(0).comics.publisher.equals("XXX"));
                    assertTrue(String.format("Expected %s but found %s", 2, comicsWithReleases.get(0).releases.size()),
                            comicsWithReleases.get(0).releases.size() == 2);
                    assertTrue(comicsWithReleases.get(0).releases.get(1).number == 999);
                }
            }
        });

        latch.await(2, TimeUnit.SECONDS);
        ld.removeObserver(observer);
    }

    @Test
    public void testOrder() throws InterruptedException {
        LogHelper.d("start");
        LiveData<List<ComicsWithReleases>> ld = mComicsDao.getComicsWithReleases();
        CountDownLatch latch = new CountDownLatch(3);
        Observer<List<ComicsWithReleases>> observer;
        ld.observeForever(observer = new Observer<List<ComicsWithReleases>>() {
            @Override
            public void onChanged(List<ComicsWithReleases> comicsWithReleases) {
                for (ComicsWithReleases cwr : comicsWithReleases) {
                    LogHelper.d("%d) %s [id=%d]", latch.getCount(), cwr.comics.name, cwr.comics.id);
                }
                latch.countDown();
            }
        });
        mComicsDao.insert(Comics.create("b"));
        mComicsDao.insert(Comics.create("A"));
        mComicsDao.insert(Comics.create("C"));
        latch.await(4, TimeUnit.SECONDS);
        ld.removeObserver(observer);
        LogHelper.d("end");
    }
}
