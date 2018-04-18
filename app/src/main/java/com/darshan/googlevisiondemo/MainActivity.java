package com.darshan.googlevisiondemo;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.AnnotateImageResponse;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.ColorInfo;
import com.google.api.services.vision.v1.model.DominantColorsAnnotation;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;
import com.google.api.services.vision.v1.model.ImageProperties;
import com.google.api.services.vision.v1.model.SafeSearchAnnotation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Darshan Bhatta
 */

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST_CODE = 420;

    private static final String VISION_API_KEY = "AIzaSyByPX6YJOmWdn86wp4_u1FgiRDBPvV2VB8";


    ImageButton takePicture;
    ProgressDialog dialog;
    Dialog myDia;
    ImageView imageView;

    private Feature feature;
    private Bitmap bitmap;

    // list of all the possible APIs, just change the  api string to one of the following
    // "LANDMARK_DETECTION" - detects the landmark and prints out the name
    // "SAFE_SEARCH_DETECTION" - detects any "adult or violent and ranks the %
    // "LOGO_DETECTION" - detects a company logo, E.g. take a picture of Google logo and return google
    // "IMAGE_PROPERTIES" - returns the RGB values at specific coordinates

    private String api = "LABEL_DETECTION";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        takePicture = findViewById(R.id.takePicture);
        imageView = findViewById(R.id.imageView);


        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (myDia != null) {
                    myDia.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    myDia.show();
                }
            }
        });

        feature = new Feature();
        feature.setType(api); //selects api
        feature.setMaxResults(8); //max number of outputs returned


        takePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePictureFromCamera();
            }
        });
    }


    //opens camera and takes a picture
    public void takePictureFromCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, CAMERA_REQUEST_CODE);

    }

    //after taking a picture, it runs this method
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            bitmap = (Bitmap) data.getExtras().get("data");
            imageView.setImageBitmap(bitmap);
            callCloudVision(bitmap, feature);
        }
    }


    //calls google vision API
    private void callCloudVision(final Bitmap bitmap, final Feature feature) {
        final ArrayList<Feature> featureList = new ArrayList<>();
        featureList.add(feature);

        final ArrayList<AnnotateImageRequest> annotateImageRequests = new ArrayList<>();

        AnnotateImageRequest annotateImageReq = new AnnotateImageRequest();
        annotateImageReq.setFeatures(featureList);
        annotateImageReq.setImage(bitmap2JPEG(bitmap));
        annotateImageRequests.add(annotateImageReq);


        new AsyncTask<Object, Void, String>() {
            @Override
            protected String doInBackground(Object... params) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        dialog = new ProgressDialog(MainActivity.this);
                        dialog.setMessage("Processing Data...");
                        dialog.show();
                    }
                });

                try {


                    HttpTransport http = AndroidHttp.newCompatibleTransport();
                    JsonFactory json = GsonFactory.getDefaultInstance();

                    VisionRequestInitializer request = new VisionRequestInitializer(VISION_API_KEY);

                    Vision.Builder build = new Vision.Builder(http, json, null);
                    build.setVisionRequestInitializer(request);

                    Vision vision = build.build();

                    BatchAnnotateImagesRequest batchRequest = new BatchAnnotateImagesRequest();
                    batchRequest.setRequests(annotateImageRequests);

                    Vision.Images.Annotate annotateRequest = vision.images().annotate(batchRequest);
                    annotateRequest.setDisableGZipContent(true);
                    BatchAnnotateImagesResponse response = annotateRequest.execute();
                    return convertResponseToString(response);
                } catch (GoogleJsonResponseException e) {
                    Toast.makeText(getApplicationContext(), "failed to reach API: " + e.getContent(), Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    Toast.makeText(getApplicationContext(), "failed to reach API: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                return "Google Vision API request failed. Check logs for details.";
            }

            protected void onPostExecute(String result) {

                dialog.dismiss();
                myDia = new Dialog(MainActivity.this);


                myDia.setContentView(R.layout.returnpopup);
                Button close = myDia.findViewById(R.id.close);
                Button help = myDia.findViewById(R.id.help);
                TextView text = myDia.findViewById(R.id.dataviewr);

                text.setText(result);

                close.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        myDia.dismiss();

                    }
                });
                help.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AlertDialog alert = null;
                        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setTitle("Help")
                                .setMessage("This application is a demo of the Google Vision API for Android. \n\nThe default API in the app is \"LABEL_DETECTION\", which tries to detect objects in the image. You can change this in the app inside the API string. \n\nThe source code of this application is available on Github @darshanbhatta. \n\nIf you have any questions email me @ bhatta.darshan26@gmail.com.");


                        final AlertDialog finalAlert = alert;
                        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                if (finalAlert != null)
                                    finalAlert.dismiss();
                            }
                        });
                        alert = builder.create();


                        alert.show();


                    }
                });


                myDia.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                myDia.show();
            }


        }.execute();
    }

    // Convert the bitmap to a JPEG
    private Image bitmap2JPEG(Bitmap bit) {

        Image baseImage = new Image();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bit.compress(Bitmap.CompressFormat.JPEG, 90, stream);
        byte[] imageBytes = stream.toByteArray();
        baseImage.encodeContent(imageBytes);

        return baseImage;
    }

    //gets the response from Google Vision and converts it into a readable format
    private String convertResponseToString(BatchAnnotateImagesResponse response) {

        AnnotateImageResponse imageResponses = response.getResponses().get(0);

        List<EntityAnnotation> entity;

        String message = "";
        switch (api) {
            case "LANDMARK_DETECTION":
                entity = imageResponses.getLandmarkAnnotations();
                message = formatText(entity);
                break;

            case "LOGO_DETECTION":
                entity = imageResponses.getLogoAnnotations();
                message = formatText(entity);
                break;

            case "SAFE_SEARCH_DETECTION":
                SafeSearchAnnotation annotation = imageResponses.getSafeSearchAnnotation();
                message = formatImageText(annotation);
                break;

            case "IMAGE_PROPERTIES":
                ImageProperties imageProperties = imageResponses.getImagePropertiesAnnotation();
                message = formatImageProp(imageProperties);
                break;

            case "LABEL_DETECTION":
                entity = imageResponses.getLabelAnnotations();
                message = formatText(entity);
                break;

        }

        return message;
    }

    // prints out safe search in a formatted way
    private String formatImageText(SafeSearchAnnotation annotation) {
        return String.format("adult: %s\nmedical: %s\nspoofed: %s\nviolence: %s\n",
                annotation.getAdult(),
                annotation.getMedical(),
                annotation.getSpoof(),
                annotation.getViolence());
    }


    //prints out the image properties in a formatted way
    private String formatImageProp(ImageProperties imageProperties) {
        String message = "";

        DominantColorsAnnotation colors = imageProperties.getDominantColors();

        for (ColorInfo color : colors.getColors()) {
            message = message + "" + color.getPixelFraction() + " - " + color.getColor().getRed() + " - " + color.getColor().getGreen() + " - " + color.getColor().getBlue();
            message = message + "\n";
        }

        return message;
    }

    //prints the message in a formatted way
    private String formatText(List<EntityAnnotation> entityAnnotation) {
        String message = "";

        if (entityAnnotation != null) {
            int count = 0;
            for (EntityAnnotation entity : entityAnnotation) {
                count++;
                if (count <= 8) {
                    int num = (int) (entity.getScore() * 100);
                    message = message + "    " + num + "% - " + entity.getDescription().substring(0, 1).toUpperCase() + entity.getDescription().substring(1);
                    message += "\n";
                }
            }
        } else {
            message = "Nothing Found";
        }

        return message;
    }


}
