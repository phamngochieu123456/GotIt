package com.aaa.gotit2;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.RequestQueue;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.VolleyError;
import com.googlecode.tesseract.android.TessBaseAPI;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private TextView tv, tv_res;
    private Button bt_rec, bt_insert;
    private ImageView iv_input;
    private TessBaseAPI tessBaseAPI;
    private String query;
    private RequestQueue requestQueue;
    private StringRequest stringRequest;
    private String BASE_URL = "http://api.wolframalpha.com/v2/query?";
    private String APP_ID = "PTYQRL-5HJ3E435KR";
    private Uri imageUri;

    private ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == RESULT_OK){
                Intent intent = result.getData();
                imageUri = intent.getData();
                iv_input.setImageURI(imageUri);
            }
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv = findViewById(R.id.tv);
        tv_res = findViewById(R.id.tv_res);
        bt_rec = findViewById(R.id.bt_rec);
        bt_insert = findViewById(R.id.bt_insert_image);
        iv_input = findViewById(R.id.iv_input);

        prepareModel();
        tessBaseAPI = new TessBaseAPI();
        tessBaseAPI.init(getFilesDir()+"","vie");

        bt_rec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tessBaseAPI.setImage(((BitmapDrawable)iv_input.getDrawable()).getBitmap());
                String result = tessBaseAPI.getUTF8Text();
                tv_res.setText(result);
                result = result.replace("+", " plus ").replace(" ", "+");

                Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024); // 1MB cap

                Network network = new BasicNetwork(new HurlStack());

                requestQueue = new RequestQueue(cache, network);
                query = BASE_URL+"appid="+APP_ID+"&input=Solve["+result+"]&podstate=Step-by-step%20solution&format=plaintext&output=json";
                requestQueue.start();

                stringRequest = new StringRequest(Request.Method.GET, query,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                JSONObject root = new JSONObject();
                                JSONArray pods = new JSONArray();
                                JSONObject podsroot = new JSONObject();
                                JSONArray subpods = new JSONArray();
                                JSONObject podsroot2 = new JSONObject();
                                String result = "";
                                try {
                                    root = new JSONObject(response).getJSONObject("queryresult");
                                    pods = root.getJSONArray("pods");
                                    podsroot = pods.getJSONObject(1);
                                    subpods = podsroot.getJSONArray("subpods");
                                    for (int i = 0;i<subpods.length();i++){
                                        podsroot2 = subpods.getJSONObject(i);
                                        result+=podsroot2.getString("plaintext")+"\n";
                                    }

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                tv.setText(result);
                                Log.d("DEBUG",subpods.toString());
                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        tv.setText(error.getMessage());
                    }
                });
                requestQueue.add(stringRequest);
            }
        });

        bt_insert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openGalerry();
            }
        });

    }

    private void openGalerry(){
        Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        activityResultLauncher.launch(gallery);
    }

    public void prepareModel()
    {
        File dir = new File(String.valueOf(getFilesDir()) + "/tessdata");
        if(!dir.exists())
        {
            dir.mkdir();
        }

        File trainedModel = new File(String.valueOf(getFilesDir()) + "/tessdata/vie.traineddata");

        if(!trainedModel.exists())
        {
            try
            {
                AssetManager assetManager = getAssets();
                InputStream is = assetManager.open("tessdata/vie.traineddata");
                OutputStream os = new FileOutputStream(String.valueOf(getFilesDir()) + "/tessdata/vie.traineddata");

                int read;
                byte[] buffer = new byte[1024];
                while((read=is.read(buffer))!=(-1))
                {
                    os.write(buffer,0,read);
                }

            }
            catch (Exception ex)
            {
                tv.setText(tv.getText() + ex.getMessage() +"\n");
            }
        }

    }

}