package com.example.hzg.videovr.show;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.example.hzg.videovr.R;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.util.ArrayList;
import java.util.List;

public class FileActivity extends AppCompatActivity {

    private static final String TAG = "File";
    private PanoramaFragment panoramaFragment ;
    private HorizontalFragment horizontalFragment ;
    private VerticalFragment verticalFragment ;
    private ViewPager mViewPager;
    private ViewPagerAdapter viewpageradapter;
    private List<Fragment> fragmentlist;
    private TabLayout mTablayout ;
    private TabLayout.Tab panorama ;
    private TabLayout.Tab horizontal ;
    private TabLayout.Tab vertical ;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file);
        mViewPager = (ViewPager) findViewById(R.id.viewpager);
        mTablayout = (TabLayout) findViewById(R.id.tab);
        fragmentlist = new ArrayList<>();
    }

    private void getFragmentData() {
        panoramaFragment = new PanoramaFragment() ;
        horizontalFragment = new HorizontalFragment() ;
        verticalFragment = new VerticalFragment();
        fragmentlist.add(panoramaFragment);
        fragmentlist.add(horizontalFragment);
        fragmentlist.add(verticalFragment);
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
        public Fragment getItem(int position) {
            Fragment fragment = fragmentlist.get(position);
            return fragment;
        }

        @Override
        public int getCount() {
            return fragmentlist.size();
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
                    if (fragmentlist.size()==0) {
                        getFragmentData();
                        viewpageradapter = new ViewPagerAdapter(getSupportFragmentManager());
                        mViewPager.setAdapter(viewpageradapter);
                        mTablayout.setupWithViewPager(mViewPager);
                        panorama = mTablayout.getTabAt(0);
                        horizontal = mTablayout.getTabAt(1);
                        vertical = mTablayout.getTabAt(2);
                        panorama.setIcon(getResources().getDrawable(R.drawable.tab_select_icon));
                        horizontal.setIcon(getResources().getDrawable(R.drawable.tab_unselect_icon));
                        vertical.setIcon(getResources().getDrawable(R.drawable.tab_unselect_icon));
                        mTablayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                            @Override
                            public void onTabSelected(TabLayout.Tab tab) {
                                if (tab == panorama) {
                                    panorama.setIcon(getResources().getDrawable(R.drawable.tab_select_icon));
                                    mViewPager.setCurrentItem(0);
                                } else if (tab == horizontal) {
                                    horizontal.setIcon(getResources().getDrawable(R.drawable.tab_select_icon));
                                    mViewPager.setCurrentItem(1);
                                } else {
                                    vertical.setIcon(getResources().getDrawable(R.drawable.tab_select_icon));
                                    mViewPager.setCurrentItem(2);
                                }
                            }

                            @Override
                            public void onTabUnselected(TabLayout.Tab tab) {
                                if (tab == panorama) {
                                    panorama.setIcon(getResources().getDrawable(R.drawable.tab_unselect_icon));
                                } else if (tab == horizontal) {
                                    horizontal.setIcon(getResources().getDrawable(R.drawable.tab_unselect_icon));
                                } else {
                                    vertical.setIcon(getResources().getDrawable(R.drawable.tab_unselect_icon));
                                }
                            }

                            @Override
                            public void onTabReselected(TabLayout.Tab tab) {

                            }
                        });
                        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                            @Override
                            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

                            }

                            @Override
                            public void onPageSelected(int position) {
                                System.out.println(position);
                                switch (position) {
                                    case 0:
                                        panorama.select();
                                        break;
                                    case 1:
                                        horizontal.select();
                                        break;
                                    case 2:
                                        vertical.select();
                                        break;
                                    default:
                                        break;
                                }
                            }

                            @Override
                            public void onPageScrollStateChanged(int state) {

                            }
                        });
                    }
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
