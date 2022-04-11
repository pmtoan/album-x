package com.hcmus.albumx.RecycleBin;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.CompositePageTransformer;
import androidx.viewpager2.widget.MarginPageTransformer;
import androidx.viewpager2.widget.ViewPager2;

import com.hcmus.albumx.AlbumList.AlbumDatabase;
import com.hcmus.albumx.AllPhotos.FullScreenImageAdapter;
import com.hcmus.albumx.AllPhotos.ImageDatabase;
import com.hcmus.albumx.AllPhotos.ImageInfo;
import com.hcmus.albumx.MainActivity;
import com.hcmus.albumx.R;

import java.io.File;
import java.util.ArrayList;

public class ImageViewingRecycleBin extends Fragment {
    public static String TAG = "Image Viewing Recycle Bin";

    private static final String IMAGE_PATH_ARG = "imagePath";
    private static final String IMAGE_POSITION_ARG = "position";

    private MainActivity main;
    private Context context;

    private ViewPager2 viewPager;
    FullScreenImageAdapter adapter;

    private int pos = 0;

    private ArrayList<ImageInfo> imageInfoArrayList;

    public static ImageViewingRecycleBin newInstance(String imagePath, int pos) {
        ImageViewingRecycleBin fragment = new ImageViewingRecycleBin();
        Bundle bundle = new Bundle();
        bundle.putString(IMAGE_PATH_ARG, imagePath);
        bundle.putInt(IMAGE_POSITION_ARG, pos);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            context = getActivity();
            main = (MainActivity) getActivity();

            imageInfoArrayList = ImageDatabase.getInstance(context).getImagesInRecycleBin();

            if (getArguments() != null) {
                pos = getArguments().getInt(IMAGE_POSITION_ARG);
            }
        } catch (IllegalStateException ignored) {
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = (View) inflater.inflate(R.layout.image_viewing_recycle_bin, container, false);

        adapter = new FullScreenImageAdapter(context, imageInfoArrayList);

        viewPager = view.findViewById(R.id.imageViewPager);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(pos, false);
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                pos = position;
            }
        });

        CompositePageTransformer transformer = new CompositePageTransformer();
        transformer.addTransformer(new MarginPageTransformer(40));
        transformer.addTransformer(new ViewPager2.PageTransformer() {
            @Override
            public void transformPage(@NonNull View page, float position) {
                float v = 1 - Math.abs(position);

                page.setScaleY(0.8f + v * 0.2f);
            }
        });
        viewPager.setPageTransformer(transformer);

        Button delete = view.findViewById(R.id.buttonDelete);
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Dialog dialog = new Dialog(context);
                dialog.getWindow().setContentView(R.layout.layout_custom_dialog_remove_image_recycle_bin);
                dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_window);

                Button removeGallery = dialog.findViewById(R.id.remove_out_gallery);
                removeGallery.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ImageDatabase.getInstance(getContext())
                                .deleteImage(imageInfoArrayList.get(pos).name,imageInfoArrayList.get(pos).path);
                        deleteImageInStorage(imageInfoArrayList.get(pos).name);
                        imageInfoArrayList.remove(pos);

                        if(imageInfoArrayList.size() > 0){
                            adapter.notifyDataSetChanged();
                            viewPager.setCurrentItem(pos, false);
                        } else {
                            main.getSupportFragmentManager().popBackStack();
                        }
                        dialog.dismiss();
                    }
                });
                Button cancel = dialog.findViewById(R.id.cancel);
                cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss();
                    }
                });
                dialog.show();
            }
        });
        Button recover = view.findViewById(R.id.buttonRecover);
        recover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ImageDatabase.getInstance(getContext())
                        .recoverImageFromRecycleBin(imageInfoArrayList.get(pos).name, imageInfoArrayList.get(pos).path);
                AlbumDatabase.getInstance(getContext())
                        .recoverImage(imageInfoArrayList.get(pos).name, imageInfoArrayList.get(pos).path);

                imageInfoArrayList.remove(pos);

                if (imageInfoArrayList.size() > 0) {
                    adapter.notifyDataSetChanged();
                    viewPager.setCurrentItem(pos, false);
                } else {
                    main.getSupportFragmentManager().popBackStack();
                }
            }
        });

        Button back = (Button) view.findViewById(R.id.backButton);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                main.getSupportFragmentManager().popBackStack();
                onDetach();
            }
        });

        return view;
    }

    public boolean isStoragePermissionGranted() {
        boolean checkWritePermission = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;

        if (checkWritePermission) {
            return  true;
        } else{
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            return false;
        }
    }

    public String deleteImageInStorage(String image_name) {
        String root = Environment.getExternalStorageDirectory().toString();
        if (isStoragePermissionGranted()) { // check or ask permission
            File myDir = new File(root, "/saved_images");
            if (!myDir.exists()) {
                myDir.mkdirs();
            }
            File file = new File(myDir, image_name);
            if (file.exists()) {
                file.delete();
            }
        }
        return null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        ((MainActivity)getActivity()).setBottomNavigationVisibility(View.VISIBLE);

        RecycleBinPhotos fragment = (RecycleBinPhotos) main.getSupportFragmentManager()
                .findFragmentByTag(RecycleBinPhotos.TAG);

        if (fragment != null) {
            fragment.notifyChangedListImageOnDelete(imageInfoArrayList);
        }
    }

}