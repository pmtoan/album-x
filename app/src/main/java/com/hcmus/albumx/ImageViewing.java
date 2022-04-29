package com.hcmus.albumx;


import android.app.Dialog;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.CompositePageTransformer;
import androidx.viewpager2.widget.MarginPageTransformer;
import androidx.viewpager2.widget.ViewPager2;

import com.hcmus.albumx.AlbumList.AlbumDatabase;
import com.hcmus.albumx.AlbumList.AlbumInfo;
import com.hcmus.albumx.AlbumList.AlbumPhotos;
import com.hcmus.albumx.AllPhotos.AllPhotos;
import com.hcmus.albumx.AllPhotos.FullScreenImageAdapter;
import com.hcmus.albumx.AllPhotos.ImageDatabase;
import com.hcmus.albumx.AllPhotos.ImageInfo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;


public class ImageViewing extends Fragment {
    public static String TAG = "Image Viewing";

    private static final String IMAGE_PATH_ARG = "imagePath";
    private static final String IMAGE_ARRAY_ARG = "imageArray";
    private static final String IMAGE_POSITION_ARG = "position";
    private static final String IMAGE_FROM_ALBUM_ARG = "fromAlbum";

    private MainActivity main;
    private Context context;

    private ViewPager2 viewPager;
    FullScreenImageAdapter adapter;

    public static int GALLERY_RESULT = 2;
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;

    private ArrayList<ImageInfo> imageInfoArrayList = new ArrayList<>();
    private int pos = 0;

    private ArrayList<AlbumInfo> albumInfoArrayList;
    private int fromAlbum;

    public static ImageViewing newInstance(String imagePath, ArrayList<ImageInfo> imageInfoArrayList, int pos, int fromAlbum) {
        ImageViewing fragment = new ImageViewing();
        Bundle bundle = new Bundle();
        bundle.putString(IMAGE_PATH_ARG, imagePath);
        bundle.putInt(IMAGE_POSITION_ARG, pos);
        bundle.putInt(IMAGE_FROM_ALBUM_ARG, fromAlbum);
        bundle.putSerializable(IMAGE_ARRAY_ARG, imageInfoArrayList);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            context = getActivity();
            main = (MainActivity) getActivity();

            if(albumInfoArrayList == null){
                albumInfoArrayList = new ArrayList<>();

                Cursor cursor = AlbumDatabase.getInstance(context).getAlbums();
                while (cursor.moveToNext()){
                    int id = cursor.getInt(0);
                    String name = cursor.getString(1);
                    int type = cursor.getInt(2);

                    if(name.equals(AlbumDatabase.albumSet.ALBUM_RECENT)){
                        albumInfoArrayList.add(new AlbumInfo(id, name, type, R.drawable.ic_recent));
                    } else if (name.equals(AlbumDatabase.albumSet.ALBUM_FAVORITE)){
                        albumInfoArrayList.add(new AlbumInfo(id, name, type, R.drawable.ic_favorite));
                    } else if (name.equals(AlbumDatabase.albumSet.ALBUM_EDITOR)){
                        albumInfoArrayList.add(new AlbumInfo(id, name, type, R.drawable.ic_edit));
                    } else {
                        albumInfoArrayList.add(new AlbumInfo(id, name, type, R.drawable.ic_photo));
                    }
                }
            }

            if (getArguments() != null) {
                pos = getArguments().getInt(IMAGE_POSITION_ARG);

                fromAlbum = getArguments().getInt(IMAGE_FROM_ALBUM_ARG);

                imageInfoArrayList = (ArrayList<ImageInfo>) getArguments().getSerializable(IMAGE_ARRAY_ARG);
            }
        } catch (IllegalStateException ignored) {
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = (View) inflater.inflate(R.layout.image_viewing, container, false);

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

        Button back = (Button) view.findViewById(R.id.backButton);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                main.getSupportFragmentManager().popBackStack();
                //notify();
            }
        });

        Button like = (Button) view.findViewById(R.id.buttonLike);
        like.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(AlbumDatabase.getInstance(context)
                        .isImageExistsInAlbum(imageInfoArrayList.get(pos).name,
                                imageInfoArrayList.get(pos).path,
                                albumInfoArrayList.get(1).id)){
                    Toast.makeText(context, "Image exists in Favorite", Toast.LENGTH_SHORT).show();
                } else{
                    AlbumDatabase.getInstance(context)
                            .insertImageToAlbum(imageInfoArrayList.get(pos).name, imageInfoArrayList.get(pos).path, albumInfoArrayList.get(1).id);

                    Toast.makeText(context, "Added to Favorite", Toast.LENGTH_SHORT).show();
                }
            }
        });

        Button edit = (Button) view.findViewById(R.id.buttonEdit);
        edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String path = imageInfoArrayList.get(pos).path;
                Log.d("pathne", path);
//                Uri uri = getImageContentUri(context, path);

                Uri selectedUri = Uri.fromFile(new File(path));
                EditImage editImage = new EditImage(getActivity());
                editImage.openEditor(selectedUri);
            }
        }); //edit onClickListener

        Button share = (Button) view.findViewById(R.id.buttonShare);
        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Create Img from bitmap and share with text
//                Bitmap bitmap =  MediaStore.Images.Media.getBitmap( , Uri.parse(imageInfoArrayList.get(pos).path));
//                shareImageandText( MediaStore.Images.Media.getBitmap(c.getContentResolver() , Uri.parse(paths)););
                shareImageandText(BitmapFactory.decodeFile(imageInfoArrayList.get(pos).path));
            }
        });

        Button delete = (Button) view.findViewById(R.id.buttonDelete);
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(fromAlbum == 0){
                    Dialog dialog = new Dialog(context);
                    dialog.setContentView(R.layout.layout_custom_dialog_remove_image_gallery);
                    dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_window);

                    Button removeGallery = dialog.findViewById(R.id.remove_out_gallery);
                    removeGallery.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            removeImageFrom(true);
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
                else {
                    Dialog dialog = new Dialog(context);
                    dialog.setContentView(R.layout.layout_custom_dialog_remove_image_album);
                    dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_window);

                    Button removeAlbum = dialog.findViewById(R.id.remove_out_album);
                    removeAlbum.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            removeImageFrom(false);
                            dialog.dismiss();
                        }
                    });
                    Button removeGallery = dialog.findViewById(R.id.remove_out_gallery);
                    removeGallery.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            removeImageFrom(true);
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
            }
        });

        Button more = (Button) view.findViewById(R.id.buttonMore);
        more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popup = new PopupMenu(getActivity().getApplicationContext(), v);
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        switch (menuItem.getItemId()) {

                            case R.id.menu_wallpaper:
                                WallpaperManager wallpaperManager = WallpaperManager.getInstance(v.getContext());
                                try {
                                    //viewPager.getDrawableState();
                                    // set the wallpaper by calling the setResource function
                                    wallpaperManager.setBitmap(BitmapFactory.decodeFile(imageInfoArrayList.get(pos).path));
                                    Toast.makeText(context, "Set wallpaper successfully", Toast.LENGTH_SHORT).show();
                                } catch (IOException e) {
                                    // here the errors can be logged instead of printStackTrace
                                    e.printStackTrace();
                                }
                                return true;
                            case R.id.menu_image_info:

                                showExif(imageInfoArrayList.get(pos).path);
                                //                                  chua lam
                                return true;
                            default:
                                return false;
                        } //Switch
                    }
                }); //setOnMenuItemClickListener
                popup.inflate(R.menu.menu_image_more);
                popup.show();
            }
        }); //more onClickListener

        return view;
    }   //View

    private void removeImageFrom(boolean isRemoveOutOfGallery){
        if(isRemoveOutOfGallery){
            ImageDatabase.getInstance(getContext())
                    .moveImageToRecycleBin(imageInfoArrayList.get(pos).name,imageInfoArrayList.get(pos).path);
            AlbumDatabase.getInstance(getContext())
                    .softDeleteImage(imageInfoArrayList.get(pos).name,imageInfoArrayList.get(pos).path);
        } else{
            AlbumDatabase.getInstance(getContext())
                    .hardDeleteImage(imageInfoArrayList.get(pos).name,imageInfoArrayList.get(pos).path);
        }

        imageInfoArrayList.remove(pos);

        if(imageInfoArrayList.size() > 0){
            adapter.notifyDataSetChanged();
            viewPager.setCurrentItem(pos, false);
        } else {
            main.getSupportFragmentManager().popBackStack();
            onDetach();
        }
    }

    private void shareImageandText(Bitmap bitmap) {
        Uri uri = getImageToShare(bitmap);
        Intent intent = new Intent(Intent.ACTION_SEND);

        // putting uri of image to be shared
        intent.putExtra(Intent.EXTRA_STREAM, uri);

        // adding text to share
        intent.putExtra(Intent.EXTRA_TEXT, "Sharing Image");

        // Add subject Here
        intent.putExtra(Intent.EXTRA_SUBJECT, "Subject Here");

        // setting type to image
        intent.setType("image/*");

        // calling startactivity() to share
        startActivity(Intent.createChooser(intent, "Share Via"));
    }   //ShareImageandText

    // Retrieving the url to share
    private Uri getImageToShare(Bitmap bitmap) {
        File imagefolder = new File(getActivity().getCacheDir(), "images");
        Uri uri = null;
        try {
            imagefolder.mkdirs();
            File file = new File(imagefolder, "shared_image.jpeg");
            FileOutputStream outputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
            outputStream.flush();
            outputStream.close();
            uri = FileProvider.getUriForFile(context,
                    BuildConfig.APPLICATION_ID + ".provider", file);

        } catch (Exception e) {
            Log.d("err", e.getMessage());
            Toast.makeText(context, "" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        return uri;
    }   //getImageToShare

    @Override
    public void onDetach() {
        super.onDetach();
        ((MainActivity)getActivity()).setBottomNavigationVisibility(View.VISIBLE);

        if(fromAlbum == 0){
            AllPhotos fragment = (AllPhotos) main.getSupportFragmentManager()
                    .findFragmentByTag(AllPhotos.TAG);

            if (fragment != null) {
                fragment.notifyChangedListImageOnDelete(imageInfoArrayList);
            }
        } else {
            AlbumPhotos fragment2 = (AlbumPhotos) main.getSupportFragmentManager()
                    .findFragmentByTag(AlbumPhotos.TAG);

            if (fragment2 != null) {
                fragment2.notifyChangedListImageOnDelete(imageInfoArrayList);
            }
        }
    }

    void showExif(String path){
        if(path != null){

            ParcelFileDescriptor parcelFileDescriptor = null;

            /*
            How to convert the Uri to FileDescriptor, refer to the example in the document:
            https://developer.android.com/guide/topics/providers/document-provider.html
             */
            try {
//                parcelFileDescriptor = main.getContentResolver().openFileDescriptor(photoUri, "r");
//                FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();

                /*
                ExifInterface (FileDescriptor fileDescriptor) added in API level 24
                 */
                ExifInterface exifInterface = new ExifInterface(path);
                String exif="";
                exif += "\nIMAGE_LENGTH: " +
                        exifInterface.getAttribute(ExifInterface.TAG_IMAGE_LENGTH);
                exif += "\nIMAGE_WIDTH: " +
                        exifInterface.getAttribute(ExifInterface.TAG_IMAGE_WIDTH);
                exif += "\n DATETIME: " +
                        exifInterface.getAttribute(ExifInterface.TAG_DATETIME);
                exif += "\n TAG_MAKE: " +
                        exifInterface.getAttribute(ExifInterface.TAG_MAKE);
                exif += "\n TAG_MODEL: " +
                        exifInterface.getAttribute(ExifInterface.TAG_MODEL);
                exif += "\n TAG_ORIENTATION: " +
                        exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION);
                exif += "\n TAG_WHITE_BALANCE: " +
                        exifInterface.getAttribute(ExifInterface.TAG_WHITE_BALANCE);
                exif += "\n TAG_FOCAL_LENGTH: " +
                        exifInterface.getAttribute(ExifInterface.TAG_FOCAL_LENGTH);
                exif += "\n TAG_FLASH: " +
                        exifInterface.getAttribute(ExifInterface.TAG_FLASH);
                exif += "\nGPS related:";
                exif += "\n TAG_GPS_DATESTAMP: " +
                        exifInterface.getAttribute(ExifInterface.TAG_GPS_DATESTAMP);
                exif += "\n TAG_GPS_TIMESTAMP: " +
                        exifInterface.getAttribute(ExifInterface.TAG_GPS_TIMESTAMP);
                exif += "\n TAG_GPS_LATITUDE: " +
                        exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
                exif += "\n TAG_GPS_LATITUDE_REF: " +
                        exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
                exif += "\n TAG_GPS_LONGITUDE: " +
                        exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
                exif += "\n TAG_GPS_LONGITUDE_REF: " +
                        exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);
                exif += "\n TAG_GPS_PROCESSING_METHOD: " +
                        exifInterface.getAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD);

//                parcelFileDescriptor.close();

                Toast.makeText(main.getApplicationContext(),
                        exif,
                        Toast.LENGTH_LONG).show();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Toast.makeText(main.getApplicationContext(),
                        "Something wrong:\n" + e.toString(),
                        Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(main.getApplicationContext(),
                        "Something wrong:\n" + e.toString(),
                        Toast.LENGTH_LONG).show();
            }

//            String strPhotoPath = photoUri.getPath();

        }else{
            Toast.makeText(main.getApplicationContext(),
                    "photoUri == null",
                    Toast.LENGTH_LONG).show();
        }
    };

}