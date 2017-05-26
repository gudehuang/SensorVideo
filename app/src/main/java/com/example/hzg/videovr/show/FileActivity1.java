package com.example.hzg.videovr.show;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.example.hzg.videovr.MainActivityCv4;
import com.example.hzg.videovr.R;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by william on 2017/4/11.
 */
public class FileActivity1 extends AppCompatActivity {

    private static final String TAG = "File";
    private FileFragment panoramaFileFragment;
    private FileFragment horizontalFileFragment;
    private FileFragment verticalFileFragment;
    private ViewPager mViewPager;
    private ViewPagerAdapter viewpageradapter;
    private List<android.support.v4.app.Fragment> fragmentlist;
    private TabLayout mTablayout ;
    private TabLayout.Tab panorama ;
    private TabLayout.Tab horizontal ;
    private TabLayout.Tab vertical ;
    private  FileFragment[] fragments=new FileFragment[3];


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file);
        mViewPager = (ViewPager) findViewById(R.id.viewpager);
        mTablayout = (TabLayout) findViewById(R.id.tab);
        viewpageradapter = new ViewPagerAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(viewpageradapter);
        mTablayout.setupWithViewPager(mViewPager);
        panorama = mTablayout.getTabAt(0);
        horizontal = mTablayout.getTabAt(1);
        vertical = mTablayout.getTabAt(2);
        panorama.setIcon(R.drawable.tab_selector);
        horizontal.setIcon(R.drawable.tab_selector);
        vertical.setIcon(R.drawable.tab_selector);
    }

    private void getFragmentData() {
        panoramaFileFragment = FileFragment.create(MainActivityCv4.dataDirA);
        horizontalFileFragment = FileFragment.create(MainActivityCv4.dataDirH) ;
        verticalFileFragment = FileFragment.create(MainActivityCv4.dataDirV);
        fragmentlist.add(panoramaFileFragment);
        fragmentlist.add(horizontalFileFragment);
        fragmentlist.add(verticalFileFragment);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);

        }
    }

    private class ViewPagerAdapter extends FragmentPagerAdapter {
        public ViewPagerAdapter(FragmentManager fm) {
            super(fm);
        }
        private String[] mTitles = new String[]{"全景", "水平", "垂直"};
        @Override
        public android.support.v4.app.Fragment getItem(int position) {
            Fragment fragment=fragments[position];
            if (fragment==null)
            {
                switch (position)
                {
                    case 0:
                        fragments[position] = FileFragment.create(MainActivityCv4.dataDirA);
                        break;
                    case 1:
                        fragments[position]= FileFragment.create(MainActivityCv4.dataDirH) ;
                        break;
                    case 2:
                       fragments[position]= FileFragment.create(MainActivityCv4.dataDirV);
                        break;
                }
                fragment=fragments[position];
            }

            return fragment;
        }

        @Override
        public int getCount() {
            return fragments.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mTitles[position];
        }
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");

                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };
}
