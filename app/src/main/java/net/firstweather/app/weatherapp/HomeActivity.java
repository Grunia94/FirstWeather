package net.firstweather.app.weatherapp;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.icu.text.DateFormat;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import net.firstweather.app.weatherapp.data.Channel;
import net.firstweather.app.weatherapp.data.Condition;
import net.firstweather.app.weatherapp.data.LocationResult;
import net.firstweather.app.weatherapp.data.Units;
import net.firstweather.app.weatherapp.fragments.WeatherConditionFragment;
import net.firstweather.app.weatherapp.listener.GeocodingServiceListener;
import net.firstweather.app.weatherapp.listener.WeatherServiceListener;
import net.firstweather.app.weatherapp.service.WeatherCacheService;
import net.firstweather.app.weatherapp.service.GoogleMapsGeocodingService;
import net.firstweather.app.weatherapp.service.YahooWeatherService;

import java.util.Objects;

public class HomeActivity extends AppCompatActivity implements WeatherServiceListener, GeocodingServiceListener, LocationListener {

    public static int GET_WEATHER_FROM_CURRENT_LOCATION = 0x00001;

    private ImageView weatherIconImageView;
    private TextView temperatureTextView;
    private TextView conditionTextView;
    private TextView locationTextView;
    private TextView dataTextView;

    private YahooWeatherService weatherService;
    private GoogleMapsGeocodingService geocodingService;
    private WeatherCacheService cacheService;

    private ProgressDialog loadingDialog;

    // weather service fail flag
    private boolean weatherServicesHasFailed = false;

    private SharedPreferences preferences = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.homescreen);

        weatherIconImageView = (ImageView) findViewById(R.id.weatherIconImageView);
        temperatureTextView = (TextView) findViewById(R.id.temperatureTextView);
        conditionTextView = (TextView) findViewById(R.id.conditionTextView);
        locationTextView = (TextView) findViewById(R.id.locationTextView);
        dataTextView = (TextView) findViewById(R.id.data);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        weatherService = new YahooWeatherService(this);
        weatherService.setTemperatureUnit(preferences.getString(getString(R.string.pref_temperature_unit), null));

        geocodingService = new GoogleMapsGeocodingService(this);
        cacheService = new WeatherCacheService(this);

        if (preferences.getBoolean(getString(R.string.pref_needs_setup), true)) {
            startSettingsActivity();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        loadingDialog = new ProgressDialog(this);
        loadingDialog.setMessage(getString(R.string.loading));
        loadingDialog.setCancelable(false);
        loadingDialog.show();

        String location = null;

        if (preferences.getBoolean(getString(R.string.pref_geolocation_enabled), true)) {
            String locationCache = preferences.getString(getString(R.string.pref_cached_location), null);

            if (locationCache == null) {
                getWeatherFromCurrentLocation();
            } else {
                location = locationCache;
            }
        } else {
            location = preferences.getString(getString(R.string.pref_manual_location), null);
        }

        if (location != null) {
            weatherService.refreshWeather(location);
        }
    }

    private void getWeatherFromCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
            }, GET_WEATHER_FROM_CURRENT_LOCATION);

            return;
        }

        // system's LocationManager
        final LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        Criteria locationCriteria = new Criteria();

        if (isNetworkEnabled) {
            locationCriteria.setAccuracy(Criteria.ACCURACY_COARSE);
        } else if (isGPSEnabled) {
            locationCriteria.setAccuracy(Criteria.ACCURACY_FINE);
        }

        locationManager.requestSingleUpdate(locationCriteria, this, null);
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == HomeActivity.GET_WEATHER_FROM_CURRENT_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getWeatherFromCurrentLocation();
            } else {
                loadingDialog.hide();

                AlertDialog messageDialog = new AlertDialog.Builder(this)
                        .setMessage(getString(R.string.location_permission_needed))
                        .setPositiveButton(getString(R.string.disable_geolocation), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                startSettingsActivity();
                            }
                        })
                        .create();

                messageDialog.show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    private void startSettingsActivity() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.currentLocation:
                loadingDialog.show();
                getWeatherFromCurrentLocation();
                return true;
            case R.id.settings:
                startSettingsActivity();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void serviceSuccess(Channel channel) {
        loadingDialog.hide();

        Condition condition = channel.getItem().getCondition();
        Units units = channel.getUnits();

        int weatherIconImageResource = getResources().getIdentifier("icon_" + condition.getCode(), "drawable", getPackageName());

        weatherIconImageView.setImageResource(weatherIconImageResource);
        temperatureTextView.setText(getString(R.string.temperature_output, condition.getTemperature(), units.getTemperature()));
        conditionTextView.setText(SpolszczenieWarunkowAtmosferycznych(condition.getDescription()));
        locationTextView.setText(channel.getLocation());
        dataTextView.setText(plMiesiac(ZmianaDaty(condition.getDate())));

        cacheService.save(channel);
    }

    @Override
    public void serviceFailure(Exception exception) {
        // display error if this is the second failure
        if (weatherServicesHasFailed) {
            loadingDialog.hide();
            Toast.makeText(this, exception.getMessage(), Toast.LENGTH_LONG).show();
        } else {
            // error doing reverse geocoding, load weather data from cache
            weatherServicesHasFailed = true;
            // OPTIONAL: let the user know an error has occurred then fallback to the cached data
            Toast.makeText(this, exception.getMessage(), Toast.LENGTH_SHORT).show();

            cacheService.load(this);
        }
    }

    @Override
    public void geocodeSuccess(LocationResult location) {
        // completed geocoding successfully
        weatherService.refreshWeather(location.getAddress());

        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(getString(R.string.pref_cached_location), location.getAddress());
        editor.apply();
    }

    @Override
    public void geocodeFailure(Exception exception) {
        // GeoCoding failed, try loading weather data from the cache
        cacheService.load(this);
    }

    @Override
    public void onLocationChanged(Location location) {
        geocodingService.refreshLocation(location);
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
        // OPTIONAL: implement your custom logic here
    }

    @Override
    public void onProviderEnabled(String s) {
        // OPTIONAL: implement your custom logic here
    }

    @Override
    public void onProviderDisabled(String s) {
        // OPTIONAL: implement your custom logic here
    }

    public void buttonOnClick(View view) {
        Intent intent;
        intent = new Intent(HomeActivity.this, Week.class);
        startActivity(intent);
    }

    public String SpolszczenieWarunkowAtmosferycznych(String angielskaNazwa){
        switch (angielskaNazwa){
            case("Tornado"):
                return "Tornado";

            case("Tropical Storm"):
                return "Burza tropikalna";

            case("Hurricane"):
                return "Huragan";

            case("Severe Thunderstorms"):
                return "Gwałtowne burze";

            case("Thunderstorms"):
                return "Burze";

            case("Mixed Rain And Snow"):
                return "Deszcz ze śniegiem";

            case("Mixed Rain And Sleet"):
                return "Deszcz ze śniegiem";

            case("Mixed Snow And Sleet"):
                return "Deszcz ze śniegiem";

            case("Freezing Drizzle"):
                return "Marznąca mżawka";

            case("Drizzle"):
                return "Mżawka";

            case("Freezing Rain"):
                return "Marznący deszcz";

            case("Showers"):
                return "Przelotny deszcz";

            case("Snow Flurries"):
                return "Śnieżyca";

            case("Light Snow Showers"):
                return "Przelotne opady śniegu";

            case("Blowing Snow"):
                return "Śnieg z wiatrem";

            case("Snow"):
                return "Śnieg";

            case("Rain"):
                return "Deszcz";

            case("Hail"):
                return "Grad";

            case("Sleet"):
                return "Śnieg z deszczem";

            case("Dust"):
                return "Zawierucha";

            case("Foggy"):
                return "Mgła";

            case("Haze"):
                return "Mgła";

            case("Smoky"):
                return "Mgliście";

            case("Blustery"):
                return "Wietrznie";

            case("Windy"):
                return "Wietrznie";

            case("Cold"):
                return "Zimno";

            case("Cloudy"):
                return "Pochmurnie";

            case("Mostly Cloudy"):
                return "Pochmurnie";

            case("Partly Cloudy"):
                return "Częściowe zachmurzenie";

            case("Clear"):
                return "Bezchmurnie";

            case("Sunny"):
                return "Słonecznie";

            case("Fair"):
                return "Bezchmurnie";

            case("Mixed Rain And Hail"):
                return "Deszcz z gradem";

            case("Hot"):
                return "Gorąco";

            case("Isolated Thunderstorms"):
                return "Możliwe burze";

            case("Scattered Thunderstorms"):
                return "Przelotne burze";

            case("Scattered Showers"):
                return "Przelotne opady";

            case("Scattered Snow Showers"):
                return "Przelotne opady śniegu";

            case("Heavy Snow"):
                return "Mocne opady śniegu";

            case("Thundershowers"):
                return "Wieczorne burze";

            case("Snow Showers"):
                return "Opady śniegu";

            case("Isolated Thundershowers"):
                return "Możliwe opady śniegu";

            case("Mostly Sunny"):
                return "Słonecznie";


            case("Not Available"):
                return "Brak danych";

        }

        return angielskaNazwa;
    }

    public String SpolszczenieDniTygodnia(String angielskaNazwa){
        switch (angielskaNazwa){
            case("Mon"):
                return "Pon.";

            case("Tue"):
                return "Wt.";

            case("Wed"):
                return "Śr";

            case("Thu"):
                return "Czw.";

            case("Fri"):
                return "Pt.";

            case("Sat"):
                return "Sob.";

            case("Sun"):
                return "Nd.";
        }

        return angielskaNazwa;
    }

    public String plMiesiac(String dane) { //02 Jun 2017
        String dzien = "";
        String miesiac = "";
        String rok = "";

        for (int i = 0; i<dane.length(); i++) {
            if(i<2) dzien += dane.charAt(i);
            if(i>2 && i<6) miesiac += dane.charAt(i);
            if(i>6 && i<dane.length()) rok += dane.charAt(i);
        }

        if (Objects.equals(miesiac, "Jun")){
            miesiac = "Czerwiec";
        }

        return dzien + " " + miesiac + " " +rok;

    }


    public String ZmianaDaty(String angielskaNazwa) {
        String tmp ="";
        for (int i = 5; i < 16; i++ ) {
          tmp += angielskaNazwa.charAt(i) ;
        }

        return tmp;
    }

}
