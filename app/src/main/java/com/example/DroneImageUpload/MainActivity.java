
//Copyright (c) 2021 Ville Helin
//
//        Permission is hereby granted, free of charge, to any person obtaining
//        a copy of this software and associated documentation files (the
//        "Software"), to deal in the Software without restriction, including
//        without limitation the rights to use, copy, modify, merge, publish,
//        distribute, sublicense, and/or sell copies of the Software, and to
//        permit persons to whom the Software is furnished to do so, subject to
//        the following conditions:
//
//        The above copyright notice and this permission notice shall be included
//        in all copies or substantial portions of the Software.
//
//        THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
//        EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
//        MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
//        IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
//        CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
//        TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
//        SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

package com.example.DroneImageUpload;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.os.FileObserver;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

// Luodaan interface apiService, jolla hallitaan minkä tyyppisiä ja millaisia arvoja lähetetään tai saadaa.
interface ApiService {

    // Luodaan multipart post postImage, jolla dronella otettu kuva on tarkoitus lähettää backendille
    @Multipart
    @POST("/upload") // Retrofitclientin alustuksessa luotu base url + /upload tässä tapauksessa
    Call<ResponseBody> postImage(
            // Muuttujat, joita lähetykseen tarvitaan
            @Part MultipartBody.Part image,
            @Part("lat") RequestBody lat,
            @Part("lon") RequestBody lon,
            @Part("drone_id") RequestBody drone_id,
            @Part("token") RequestBody token
    );
}


public class MainActivity extends AppCompatActivity {

    private final int IMG_REQUEST = 200;
    ApiService apiService;
    Bitmap mBitmap;
    TextView responseTextView;
    TextView observerTextView;
    EditText droneIdEditText;
    String fileName;
    String fileUri;
    String drone_id;
    ArrayList<String> uploadedFiles = new ArrayList<>();

    public static FileObserver observer;

    String[] permissions = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        responseTextView = findViewById(R.id.responseTextView);
        observerTextView = findViewById(R.id.observerStatusTextView);
        droneIdEditText = findViewById(R.id.droneIdEditText);

        // Sovelluksen käynnistyessä kutsutaan näitä funktioita
        checkPermissions(); // Tarkistetaan, että sovelluksella on tarvittavat oikeudet
        initRetrofitClient(); // Alustetaan retrofit toiminta valmiiksi
    }

    // Sovellus tarvitsee laitekohtaisia oikeuksia, joiden alustus ja androidiin liittyvät kyselyt tehdään
    private boolean checkPermissions() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : permissions) {
            result = ContextCompat.checkSelfPermission(this, p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), 100);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == 100) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // do something
            }
            return;
        }
    }

    // Alustetaan retrofitClient ja liitettään se alussa luotuun interfaceen.
    private void initRetrofitClient() {
        // OkHttpClient client = new OkHttpClient.Builder().protocols(Arrays.asList(Protocol.HTTP_1_1)).connectTimeout(5, TimeUnit.MINUTES).readTimeout(5, TimeUnit.MINUTES).build();
        OkHttpClient client = new OkHttpClient.Builder().protocols(Arrays.asList(Protocol.HTTP_1_1)).build();

        // TODO: Add apiService here
        // apiService = new Retrofit.Builder().baseUrl("http://192.168.8.104:3000/").client(client).build().create(ApiService.class); // Localhostissa käytetty apiService
    }

    // Valitse kuva nappulan toiminta, jossa avataan galleria intent. Galleria intentillä valitaan kuva galleriasta ja avataan se imageviewiin.
    public void selectImage(View view) {

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, IMG_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {

            ImageView imageView = findViewById(R.id.imageView); // imageViewin alustus

            if (requestCode == IMG_REQUEST) {
                fileUri = getPathFromURI(data.getData()); // Tallennetaan kuva fileUri muuttujaan ja haetaan sen sijainti ensin

                if (fileUri != null) {
                    // Otetaan kuvasta sen nimi talteen ja laitetaan se muuttujaan fileName
                    String[] name = fileUri.split("/");
                    fileName = name[name.length - 1];
                    Toast.makeText(getApplicationContext(), fileName, Toast.LENGTH_SHORT).show(); // Näytettään toastissa fileName

                    // Näytettään kuva imageViewissä
                    mBitmap = BitmapFactory.decodeFile(fileUri);
                    imageView.setImageBitmap(mBitmap);
                }
                else
                    Toast.makeText(this, "Virhe kuvan valinnassa. Yritä uudelleen!", Toast.LENGTH_SHORT).show(); // Kuva virheilmoitus
            }
        }
    }

    // Kuvan sijainti puhelimessa sen urin perusteella
    private String getPathFromURI(Uri contentUri) {
        String[] proj = {MediaStore.Audio.Media.DATA};
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    private void multipartImageUpload() {
        Toast.makeText(this, "Kuvaa lähetetään..", Toast.LENGTH_SHORT).show(); // Annetaan toast ilmoitus kun kuvaa yritetään lähettää

        drone_id = droneIdEditText.getText().toString(); // Ottaa drone_idlle arvo edit text kentästä

        File file = new File(fileUri);

//        if (droneIdEditText.getText().toString().isEmpty()) {
//            drone_id = "1";
//        }

        // Alustetaan backendille lähetettävät arvot
        RequestBody reqFile = RequestBody.create(MediaType.parse("image/*"), file);
        MultipartBody.Part imageBody = MultipartBody.Part.createFormData("image", file.getName(), reqFile);
        RequestBody latBody = RequestBody.create(MediaType.parse("text/plain"), "61.50353");
        RequestBody lonBody = RequestBody.create(MediaType.parse("text/plain"), "23.80842");
        RequestBody drone_idBody = RequestBody.create(MediaType.parse("text/plain"), drone_id);
        RequestBody tokenBody = RequestBody.create(MediaType.parse("text/plain"), "**TOKEN_HERE**"); // TODO: Add token for backend here

        // Kutsutaan lähetystä ja reagoidaan backendin vastaukselle
        Call<ResponseBody> req = apiService.postImage(imageBody, latBody, lonBody, drone_idBody, tokenBody);
        req.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                if (response.code() == 200) {
                    // Näytetään backendin vastaus sen onnistuessa sovelluksen teksti kentässä
                    responseTextView.setText("Lähetys onnistui: " + response.code());
                    responseTextView.setTextColor(Color.BLUE);
                } else
                    // Näytetään backendin response.code tekstikentässä, jos se ei ole 200, mutta lähetys onnistui.
                    responseTextView.setText(response.code());

                // Toast.makeText(getApplicationContext(), response.code() + " ", Toast.LENGTH_SHORT).show(); // Toast message response.codelle helppottamaan virheenkorjausta
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                // Näytetään lähetys ei onnistunut tekstikentässä, jos jokin menee pieleen.
                responseTextView.setText("Lähetys epäonnistui");
                responseTextView.setTextColor(Color.RED);
                t.printStackTrace(); // Virheen tulostus??
            }
        });
    }

    // Lähetä kuva nappulan toiminta
    public void uploadImage(View view) {
        if (!droneIdEditText.getText().toString().isEmpty()) {
            if (mBitmap != null) {
                multipartImageUpload(); // Kutsutaan lähetä funktiota
            } else {
                Toast.makeText(getApplicationContext(), "Kuvaa ei löydy. Yritä uudeleen", Toast.LENGTH_SHORT).show(); // Annetaan virhe, jos kuvaa ei löydy imageviewistä
            }
        }
        else
            Toast.makeText(this, "Syötä drone_id ensin!", Toast.LENGTH_SHORT).show(); // drone_id on vaadittu kenttä ennen tätä toimintoa, joten näytetään virheilmoitus
    }

    // Kuuntelijan käynnistys funktio
    private void startWatching() {

        // final String pathToWatch = android.os.Environment.getExternalStorageDirectory().toString() + "/DCIM/Camera/"; // Kameralla otettujen kuvien sijainti pää asiassa testaamisen helpottamiseksi

        final String pathToWatch = android.os.Environment.getExternalStorageDirectory().toString() + "/DJI/dji.go.v4/DJI Album/"; // Dronella tallennettujen kuvien  sijainti
        Toast.makeText(this, "Kuuntelija käynnistetty. Kuunneellaan " + pathToWatch, Toast.LENGTH_LONG).show(); // Näytetään toast ilmoitus kun kuuntelija käynnistetään

        // Käynnistetään fileobserver tyyppinen kuuntelija ja laitetaan se kuuntelemaan moved_to tyyppisiä tapahtumia aiemmin alustettuun kansioon
        observer = new FileObserver(pathToWatch, FileObserver.MOVED_TO) { //ALL_EVENTS) {
            @Override
            public void onEvent(int event, final String file) { // Kuuntelija huomaa tapahtuman
                if (file != null) {
                    if (!(uploadedFiles.contains(file))) {
                        uploadedFiles.add(file); // Lisätään kuva aiempien lähettetyjen juokkoon, jotta kuvia ei lähettetäisi montaa kertaa
                        fileUri = pathToWatch + file; // Laitetaan kuvan sijainti fileUri muuttujaan, jotta se osataan myöhemmässä vaiheessa lähettää backendille
                        multipartImageUpload(); // Kutsutaan lähetettäävää funktiota
                    }
//                    if (!(pathToWatch + file).equals(fileUri)) {
//                        // System.out.println("File created [" + pathToWatch + file + "]");
//                        fileUri = pathToWatch + file;
//                        multipartImageUpload();
//                    }
                }
            }
        };
        observer.startWatching(); // Kuuntelija käynnistys
        // Näytettään tekstikentässä, kun kuutelija on käynnissä
        observerTextView.setTextColor(Color.BLUE);
        observerTextView.setVisibility(View.VISIBLE);
    }

    // Käynnistä kuuntelija nappulan toiminta
    public void startObserverButton(View view) {
        if (!droneIdEditText.getText().toString().isEmpty()) {
            startWatching(); // Kutsutaan kuuntelijan käynnistämistä
            droneIdEditText.setEnabled(false); // Suljetaan drone_id teksti kenttän muokkaus, jotta sitä ei voida muuttaa kun kuuntelija on päällä
        }
        else {
            Toast.makeText(this, "Syötä drone_id ensin!", Toast.LENGTH_SHORT).show(); // drone_id pitää olla syötetty ennen kuuntelijan käynnistämstä, joten annettaan siitä virheilmoitus
        }
    }

    // Sammuta kuuntelija nappulan toiminta
    public void stopObserver(View view) {
        if (observerTextView.getVisibility() == view.VISIBLE) {
            Toast.makeText(this, "Kuuntelija sammutettu", Toast.LENGTH_SHORT).show(); // Annetaan toast ilmoitus, kun kuuntelija on sammutettu
            observer.stopWatching(); // Sammutetaan kuuntelija
            observerTextView.setVisibility(View.INVISIBLE); // Piiloitetaan kuunlija päällä tekstikenttä
            droneIdEditText.setEnabled(true); // Muutetaan drone_id teksti kenttä taas muokattavaksi
        }
        else {
            Toast.makeText(this, "Käynnistä kuuntelija ensin", Toast.LENGTH_SHORT).show(); // Virheilmoitus, kun kuuntelija ei ole päällä ja nappullaa painetaan
        }
    }
}


