package com.example.aluno.olay;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.example.aluno.olay.Constants.PICK_IMAGE_REQUEST;
import static com.example.aluno.olay.Constants.READ_WRITE_EXTERNAL;

public class CreateActivity extends AppCompatActivity {
    GPSTracker gps;

    static final int REQUEST_IMAGE_CAPTURE = 1;

    EditText txtdata;
    EditText txthora;
    EditText txtnome;
    EditText txtdescricao;
    EditText txtendereco;

    String link;

    int pYear;
    int pMonth;
    int pDay;

    int pHora;
    int pMin;

    //atribui id para dialogo de data/hora
    static final int DATE_DIALOG_ID = 0;
    static final int TIME_DIALOG_ID = 1;


    private static final int MAX_WIDTH = 1024;
    private static final int MAX_HEIGHT = 768;
    private File chosenFile;
    private Uri returnUri;

    String localizacaoEvento, linkImageEvento, dataEvento, nomeEvento, descricaoEvento, horaEvento;

    StringBuilder dataBanco, horaBanco;

    //DATA
    private DatePickerDialog.OnDateSetListener pDateSetListener =
            new DatePickerDialog.OnDateSetListener() {
                public void onDateSet(DatePicker view, int year,
                                      int monthOfYear, int dayOfMonth) {
                    pYear = year;
                    pMonth = monthOfYear;
                    pDay = dayOfMonth;
                    updateDisplay();
                }
            };

    private void updateDisplay() {
        txtdata.setText(
                new StringBuilder()
                        // Month is 0 based so add 1
                        .append(pDay).append("-")
                        .append(pMonth + 1).append("-")
                        .append(pYear).append(" "));

        //formata o campo data pra mandar para o banco
        dataBanco = new StringBuilder()
                .append(pYear).append("-")
                .append(pMonth + 1).append("-")
                .append(pDay).append("");

    }

    //HORA
    private TimePickerDialog.OnTimeSetListener pTimeSetListener =
            new TimePickerDialog.OnTimeSetListener() {
                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                    pHora = hourOfDay;
                    pMin = minute;
                    updateDisplay1();
                }
            };

    private void updateDisplay1() {
        txthora.setText(
                new StringBuilder()
                        .append(pHora).append(":")
                        .append(pMin));

        horaBanco = new StringBuilder()
                .append(pHora).append(":")
                .append(pMin).append(":")
                .append("00");
    }

    //cria um novo dialog para o picker
    @SuppressWarnings("deprecation")
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DATE_DIALOG_ID:
                return new DatePickerDialog(this,
                        pDateSetListener,
                        pYear, pMonth, pDay);
            case TIME_DIALOG_ID:
                return new TimePickerDialog(this,
                        pTimeSetListener,
                        pHora, pMin, false);
        }
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create);

        txtnome = (EditText) findViewById(R.id.txtNome);
        txtdescricao = (EditText) findViewById(R.id.txtDescricao);
        txtendereco = (EditText) findViewById(R.id.txtEndereco);
        txtendereco.setInputType(InputType.TYPE_NULL);
        txtdata = (EditText) findViewById(R.id.txtData);
        txthora = (EditText) findViewById(R.id.txtHora);

        txtdata.setOnClickListener(new View.OnClickListener() {
            @SuppressWarnings("deprecation")
            public void onClick(View v) {
                showDialog(DATE_DIALOG_ID);
            }
        });

        txthora.setOnClickListener(new View.OnClickListener() {
            @SuppressWarnings("deprecation")
            public void onClick(View v) {
                showDialog(TIME_DIALOG_ID);
            }
        });

        final Calendar cal = Calendar.getInstance();
        pYear = cal.get(Calendar.YEAR);
        pMonth = cal.get(Calendar.MONTH);
        pDay = cal.get(Calendar.DAY_OF_MONTH);

        updateDisplay();

        final Calendar hor = Calendar.getInstance();
        pHora = hor.get(Calendar.HOUR_OF_DAY);
        pMin = hor.get(Calendar.MINUTE);
        updateDisplay1();

        //BOTÕES CANCELAR E OK
        //botões do actionbar, pos isso estou utlizando o findviewbyid
        final LayoutInflater inflater = (LayoutInflater) getSupportActionBar().getThemedContext()
                .getSystemService(LAYOUT_INFLATER_SERVICE);
        final View customActionBarView = inflater.inflate(
                R.layout.actionbar_custom_view_done_cancel, null);
        customActionBarView.findViewById(R.id.actionbar_done).setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View v) {
                        //"Enviar"
                        if(!isOnline()){
                            mostrarAviso(v,"Não foi possível acessar a internet.");
                        }
                        else{
                            if(txtnome.getText().toString().equals("Posição atual")){
                                mostrarAviso(v, "Por favor, selecione outro nome para o evento!");
                            }
                            else if(txtnome.getText().toString().equals("")
                                    ||txtdescricao.getText().toString().equals("")
                                    ||txtendereco.getText().toString().equals("")){
                                mostrarAviso(v, "Por favor, preencha todos os campos!");
                            }
                            else{
                                onUpload(v);
                            }
                        }
                    }
                });
        customActionBarView.findViewById(R.id.actionbar_cancel).setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View v) {
                        // "Cancelar"
                        finish();
                    }
                });

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayOptions(
                ActionBar.DISPLAY_SHOW_CUSTOM,
                ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME
                        | ActionBar.DISPLAY_SHOW_TITLE);
        actionBar.setCustomView(customActionBarView,
                new ActionBar.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
        // END_INCLUDE (inflate_set_custom_view)

        txtendereco.setOnClickListener(new View.OnClickListener() {
            @SuppressWarnings("deprecation")
            public void onClick(View v) {
                mostrarAviso(v,"Voce pode atualizar a localização clicando no campo novamente.");
                //GPS
                // Create class object
                gps = new GPSTracker(CreateActivity.this);

                // Check if GPS enabled
                if(gps.canGetLocation()) {

                    double latitude = gps.getLatitude();
                    double longitude = gps.getLongitude();

                    // \n is for new line
                    //Toast.makeText(getApplicationContext(), "Your Location is - \nLat: " + latitude + "\nLong: " + longitude, Toast.LENGTH_LONG).show();
                    txtendereco.setText(latitude+","+longitude);
                } else {
                    // Can't get location.
                    // GPS or network is not enabled.
                    // Ask user to enable GPS/network in settings.
                    gps.showSettingsAlert();
                }
            }
        });
    }

    public void onChoose(View v) {

        final CharSequence[] items = {"Câmera", "Galeria"};

        AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
        builder.setIcon(R.drawable.foto);
        builder.setTitle("De onde você deseja escolher a imagem ?");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                //dependendo da escolha de endereço, abre um mapa diferente
                switch (item) {
                    case 0:
                        //CAMERA
                        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE);
                        break;

                    case 1:
                        //GALERIA
                        Intent intent = new Intent();
                        intent.setType("image/*");
                        intent.setAction(Intent.ACTION_GET_CONTENT);
                        startActivityForResult(Intent.createChooser(intent, "Selecione a imagem"), PICK_IMAGE_REQUEST);
                        break;
                }
            }
        });
        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();

    }

    public void onUpload(View v) {

        if (chosenFile == null) {
            mostrarAviso(v, "Escolha uma imagem antes de enviar");
            return;
        }
        mostrarAviso(v, "Cadastrando evento...");

        final NotificationHelper notificationHelper = new NotificationHelper(this.getApplicationContext());
        notificationHelper.createUploadingNotification();

        ImgurService imgurService = ImgurService.retrofit.create(ImgurService.class);

        final Call<ImageResponse> call =
                imgurService.postImage(
                        txtnome.getText().toString(),"", "", "",
                        MultipartBody.Part.createFormData(
                                "image",
                                chosenFile.getName(),
                                RequestBody.create(MediaType.parse("image/*"), chosenFile)
                        ));

        call.enqueue(new Callback<ImageResponse>() {
            @Override
            public void onResponse(Call<ImageResponse> call, Response<ImageResponse> response) {
                if (response == null) {
                    notificationHelper.createFailedUploadNotification();
                    return;
                }
                if (response.isSuccessful()) {

                    linkImageEvento = "http://imgur.com/" + response.body().data.id+".jpg";
                    localizacaoEvento = txtendereco.getText().toString();
                    dataEvento = dataBanco.toString();
                    nomeEvento = txtnome.getText().toString();
                    descricaoEvento = txtdescricao.getText().toString();
                    horaEvento = txthora.getText().toString();

                    Log.d("URL da imagem", "http://imgur.com/" + response.body().data.id+".jpg");
                    notificationHelper.createUploadedNotification(response.body());

                    new CadastrarEvento().execute((Void) null);
                }
            }

            @Override
            public void onFailure(Call<ImageResponse> call, Throwable t) {
                Toast.makeText(getApplicationContext(), "Erro ao enviar a imagem, tente novamente.", Toast.LENGTH_SHORT).show();
                notificationHelper.createFailedUploadNotification();
                t.printStackTrace();
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if ((requestCode == PICK_IMAGE_REQUEST || requestCode == REQUEST_IMAGE_CAPTURE) && resultCode == RESULT_OK && data != null && data.getData() != null) {

            returnUri = data.getData();

            try {
                ImageView imageView = (ImageView) findViewById(R.id.imvFoto);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

                //abaixando RES da imagem para gastar menos memória RAM
                int size = (int) Math.ceil(Math.sqrt(MAX_WIDTH * MAX_HEIGHT));

                // Loads given image
                Picasso.with(imageView.getContext())
                        .load(returnUri)
                        .transform(new BitmapTransform(MAX_WIDTH, MAX_HEIGHT))
                        .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
                        .resize(size, size)
                        .centerInside()
                        .into(imageView);

            } catch (Exception e) {
                e.printStackTrace();
            }

            super.onActivityResult(requestCode, resultCode, data);

            Log.d(this.getLocalClassName(), "Before check");


            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
                final List<String> permissionsList = new ArrayList<String>();
                addPermission(permissionsList, Manifest.permission.READ_EXTERNAL_STORAGE);
                addPermission(permissionsList, Manifest.permission.WRITE_EXTERNAL_STORAGE);

                if (!permissionsList.isEmpty())
                    ActivityCompat.requestPermissions(CreateActivity.this,
                            permissionsList.toArray(new String[permissionsList.size()]),
                            READ_WRITE_EXTERNAL);
                else
                    getFilePath();
            } else {
                getFilePath();
            }
        }
    }

    private void getFilePath() {
        String filePath = DocumentHelper.getPath(this, this.returnUri);
        //Safety check to prevent null pointer exception
        if (filePath == null || filePath.isEmpty()) return;
        chosenFile = new File(filePath);
        Log.d("FilePath", filePath);
    }

    private void addPermission(List<String> permissionsList, String permission) {
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(permission);
            shouldShowRequestPermissionRationale(permission);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case READ_WRITE_EXTERNAL:
            {
                Map<String, Integer> perms = new HashMap<String, Integer>();
                perms.put(Manifest.permission.READ_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
                for (int i = 0; i < permissions.length; i++)
                    perms.put(permissions[i], grantResults[i]);
                if (perms.get(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                        && perms.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(), "Todas as permissões foram concedidas.", Toast.LENGTH_SHORT).show();
                    getFilePath();
                } else {
                    Toast.makeText(getApplicationContext(), "Algumas permissões foram negadas.", Toast.LENGTH_SHORT).show();
                }
                return;
            }
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @SuppressWarnings("deprecation")
    class CadastrarEvento extends AsyncTask<Void, Void, Boolean> {

        private void postData(String localizacaoEvento, String linkImageEvento, String dataEvento, String nomeEvento, String descricaoEvento, String horaEvento) {
            try {
                HttpClient httpclient = new DefaultHttpClient();
                HttpPost httppost = new HttpPost("http://192.168.1.105/olay/cadEventos.php");

                ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                nameValuePairs.add(new BasicNameValuePair("localizacaoEvento", localizacaoEvento));
                nameValuePairs.add(new BasicNameValuePair("linkImageEvento", linkImageEvento));
                nameValuePairs.add(new BasicNameValuePair("dataEvento", dataEvento));
                nameValuePairs.add(new BasicNameValuePair("nomeEvento", nomeEvento));
                nameValuePairs.add(new BasicNameValuePair("descricaoEvento", descricaoEvento));
                nameValuePairs.add(new BasicNameValuePair("horaEvento", horaEvento));
                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                final HttpResponse resposta = httpclient.execute(httppost);

                runOnUiThread(new Runnable(){
                    public void run(){
                        try {
                            //mostrará a mensagem que o php retornar, sendo de sucesso ou não
                            Toast.makeText(getApplicationContext(), EntityUtils.toString(resposta.getEntity()), Toast.LENGTH_SHORT).show();

                        } catch (ParseException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try{
                postData(localizacaoEvento,linkImageEvento,dataEvento,nomeEvento,descricaoEvento,horaEvento);
            }
            catch(Exception e){
                CreateActivity.this.runOnUiThread(new Runnable(){
                    public void run(){
                        Toast.makeText(getApplicationContext(), "Não foi possivel conectar ao servidor", Toast.LENGTH_SHORT).show();
                    }
                });
                return null;
            }
            return null;
        }
    }
    void mostrarAviso(View v, String texto){
        if (Build.VERSION.SDK_INT >= 8) {
            Snackbar.make(v,texto, Snackbar.LENGTH_LONG).setAction("Action", null).show();;
        }
        else
            Toast.makeText(getApplicationContext(), texto, Toast.LENGTH_SHORT).show();
    }

    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null &&
                cm.getActiveNetworkInfo().isConnectedOrConnecting();
    }
}