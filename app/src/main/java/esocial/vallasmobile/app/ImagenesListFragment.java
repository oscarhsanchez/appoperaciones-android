package esocial.vallasmobile.app;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;

import esocial.vallasmobile.R;
import esocial.vallasmobile.adapter.ImagenesListAdapter;
import esocial.vallasmobile.components.SpacesItemDecoration;
import esocial.vallasmobile.obj.Imagen;
import esocial.vallasmobile.utils.Constants;
import esocial.vallasmobile.utils.Dialogs;
import esocial.vallasmobile.utils.ImageUtils;
import esocial.vallasmobile.utils.Utils;

/**
 * Created by jesus.martinez on 28/03/2016.
 */
public abstract class ImagenesListFragment extends BaseFragment {


    public RecyclerView list;
    public TextView emptyText;
    public ProgressBar progressBar;
    private FloatingActionButton fabAddImage;
    public ProgressDialog progressDialog;

    public ImagenesListAdapter adapter;
    public ArrayList<Imagen> images;
    LinearLayoutManager mLayoutManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.list_imagenes, container, false);
        list = (RecyclerView) v.findViewById(R.id.imagenes_list);
        progressBar = (ProgressBar) v.findViewById(R.id.progressBar);
        fabAddImage = (FloatingActionButton) v.findViewById(R.id.fab_add_image);
        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.spacing);
        list.addItemDecoration(new SpacesItemDecoration(spacingInPixels));
        emptyText = (TextView) v.findViewById(R.id.empty_images);
        list.setLayoutManager(new LinearLayoutManager(getActivity()));
        list.setItemAnimator(new DefaultItemAnimator());
        return v;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mLayoutManager = new LinearLayoutManager(getActivity());
        list.setLayoutManager(mLayoutManager);

        setListeners();
        getImages();
    }


    private void setListeners() {
        fabAddImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Check permission from location
                if (Build.VERSION.SDK_INT >= 23 &&
                        ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                    ActivityCompat.requestPermissions(getActivity(),
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            Constants.PERMISSION_READ_STORAGE);
                } else {
                    openSelectDialog();
                }
            }
        });
    }

    private void openSelectDialog() {
        AlertDialog.Builder getImageFrom = new AlertDialog.Builder(getActivity());
        final CharSequence[] opsChars = {getResources().getString(R.string.take_pic), getResources().getString(R.string.open_gallery)};
        getImageFrom.setItems(opsChars, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Images.Media.TITLE, "Image_"+System.currentTimeMillis());
                    mCapturedImageURI = getContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                    takePicture.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI);
                    getActivity().startActivityForResult(takePicture, Constants.REQUEST_CAMERA);

                } else if (which == 1) {
                    Intent pickPhoto = new Intent(Intent.ACTION_PICK,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    getActivity().startActivityForResult(pickPhoto, Constants.REQUEST_GALLERY);
                }
                dialog.dismiss();
            }
        });
        getImageFrom.show();
    }

    Uri mCapturedImageURI;

    public abstract void getImages();

    public abstract void postImage(String time, byte[] bitmap);

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            String imgDecodableString = null;
            if (requestCode == Constants.REQUEST_GALLERY) {
                Uri selectedImage = data.getData();
                String[] filePathColumn = {MediaStore.Images.Media.DATA};

                // Get the cursor
                Cursor cursor = getActivity().getContentResolver().query(selectedImage,
                        filePathColumn, null, null, null);
                // Move to first row
                cursor.moveToFirst();

                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                imgDecodableString = cursor.getString(columnIndex);
                cursor.close();
            } else if (requestCode == Constants.REQUEST_CAMERA) {
                imgDecodableString = ImageUtils.getRealPathFromUri(getActivity(), mCapturedImageURI);
            }

            progressDialog = Dialogs.newProgressDialog(getActivity(), getString(R.string.saving_image), false);
            progressDialog.show();


            byte [] byteArray = ImageUtils.compressImage(imgDecodableString);

           /* Bitmap bitmap = ImageUtils.decodeSampledBitmapFromFile(imgDecodableString, 500, 500);
            bitmap = ImageUtils.getImageRotated(imgDecodableString, bitmap);*/

            Calendar now = Calendar.getInstance();
            String year = String.format("%02d",now.get(Calendar.YEAR));
            String month = String.format("%02d",now.get(Calendar.MONTH) + 1); // Note: zero based!
            String day = String.format("%02d",now.get(Calendar.DAY_OF_MONTH));
            String hour = String.format("%02d",now.get(Calendar.HOUR_OF_DAY));
            String minute = String.format("%02d",now.get(Calendar.MINUTE));
            String second = String.format("%02d",now.get(Calendar.SECOND));

            postImage(year+month+day+hour+minute+second, byteArray);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case Constants.PERMISSION_READ_STORAGE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openSelectDialog();
                }
                break;
        }
    }

    public String photoFileName = "photo.jpg";

    // Returns the Uri for a photo stored on disk given the fileName
    public Uri getPhotoFileUri(String fileName) {
        // Only continue if the SD Card is mounted
        if (isExternalStorageAvailable()) {
            // Get safe storage directory for photos
            // Use `getExternalFilesDir` on Context to access package-specific directories.
            // This way, we don't need to request external read/write runtime permissions.
            File mediaStorageDir = new File(
                    Environment.getExternalStorageDirectory().toString(), "VallasApp");

            // Create the storage directory if it does not exist
            if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()){
                Log.d("VallasApp", "failed to create directory");
            }

            // Return the file target for the photo based on filename
            return Uri.fromFile(new File(mediaStorageDir.getPath() + File.separator + fileName));
        }
        return null;
    }

    // Returns true if external storage for photos is available
    private boolean isExternalStorageAvailable() {
        String state = Environment.getExternalStorageState();
        return state.equals(Environment.MEDIA_MOUNTED);
    }
}
