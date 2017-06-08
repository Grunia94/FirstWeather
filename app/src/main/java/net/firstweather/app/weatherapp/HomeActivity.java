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
        dataTextView.setText(condition.getDate());

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
        String polskaNazwa = new String();
        switch (angielskaNazwa){
            case("tornado"):
                polskaNazwa = "Tornado";
                break;
            case("tropical storm"):
                polskaNazwa = "Burza tropikalna";
                break;
            case("hurricane"):
                polskaNazwa = "Huragan";
                break;
            case("severe thunderstorms"):
                polskaNazwa = "Gwałtowne burze";
                break;
            case("thunderstorms"):
                polskaNazwa = "Burze";
                break;
            case("mixed rain and snow"):
                polskaNazwa = "Deszcz ze śniegiem";
                break;
            case("mixed rain and sleet"):
                polskaNazwa = "Deszcz ze śniegiem";
                break;
            case("mixed snow and sleet"):
                polskaNazwa = "Deszcz ze śniegiem";
                break;
            case("freezing drizzle"):
                polskaNazwa = "Marznąca mżawka";
                break;
            case("drizzle"):
                polskaNazwa = "Mżawka";
                break;
            case("freezing rain"):
                polskaNazwa = "Marznący deszcz";
                break;
            case("showers"):
                polskaNazwa = "Przelotny deszcz";
                break;
            case("snow flurries"):
                polskaNazwa = "Śnieżyca";
                break;
            case("light snow showers"):
                polskaNazwa = "Przelotne opady śniegu";
                break;
            case("blowing snow"):
                polskaNazwa = "Śnieg z wiatrem";
                break;
            case("snow"):
                polskaNazwa = "Śnieg";
                break;
            case("hail"):
                polskaNazwa = "Grad";
                break;
            case("sleet"):
                polskaNazwa = "Śnieg z deszczem";
                break;
            case("dust"):
                polskaNazwa = "Zawierucha";
                break;
            case("foggy"):
                polskaNazwa = "Mgła";
                break;
            case("haze"):
                polskaNazwa = "Mgła";
                break;
            case("smoky"):
                polskaNazwa = "Mgliście";
                break;
            case("blustery"):
                polskaNazwa = "Wietrznie";
                break;
            case("windy"):
                polskaNazwa = "Wietrznie";
                break;
            case("Cold"):
                polskaNazwa = "Zimno";
                break;
            case("Cloudy"):
                polskaNazwa = "Pochmurnie";
                break;
            case("mostly cloudy (night)"):
                polskaNazwa = "Pochmurnie nocą";
                break;
            case("mostly cloudy (day)"):
                polskaNazwa = "Pochmurnie w dzień";
                break;
            case("partly cloudy (night)"):
                polskaNazwa = "Częściowe zachmurzenie";
                break;
            case("partly cloudy (day)"):
                polskaNazwa = "Częściowe zachmurzenie";
                break;
            case("clear (night)"):
                polskaNazwa = "Bezchmurna noc";
                break;
            case("sunny"):
                polskaNazwa = "Słonecznie";
                break;
            case("fair (night)"):
                polskaNazwa = "Bezchmurna noc";
                break;
            case("fair (day)"):
                polskaNazwa = "Bezchmurny dzień";
                break;
            case("mixed rain and hail"):
                polskaNazwa = "";
                break;
            case("Deszcz z gradem"):
                polskaNazwa = "";
                break;
            case("hot"):
                polskaNazwa = "Gorąco";
                break;
            case("isolated thunderstorms"):
                polskaNazwa = "Możliwe burze";
                break;
            case("scattered thunderstorms"):
                polskaNazwa = "Przelotne burze";
                break;
            case("scattered showers"):
                polskaNazwa = "Przelotne opady";
                break;
            case("scattered snow showers"):
                polskaNazwa = "Przelotne opady śniegu";
                break;
            case("heavy snow"):
                polskaNazwa = "Mocne opady śniegu";
                break;
            case("partly cloudy"):
                polskaNazwa = "Częściowe zachmurzenie";
                break;
            case("thundershowers"):
                polskaNazwa = "Wieczorne burze";
                break;
            case("snow showers"):
                polskaNazwa = "Opady śniegu";
                break;
            case("isolated thundershowers"):
                polskaNazwa = "Możliwe opady śniegu";
                break;
            case("not available"):
                polskaNazwa = "Brak danych";
                break;
        }
        return polskaNazwa;
    }

    public String SpolszczenieDniTygodnia(String angielskaNazwa){
        String polskaNazwa = new String();
        switch (angielskaNazwa){
            case("Mon"):
                polskaNazwa = "Pon.";
                break;
            case("Tue"):
                polskaNazwa = "Wt.";
                break;
            case("Wed"):
                polskaNazwa = "Śr";
                break;
            case("Thu"):
                polskaNazwa = "Czw.";
                break;
            case("Fri"):
                polskaNazwa = "Pt.";
                break;
            case("Sat"):
                polskaNazwa = "Sob.";
                break;
            case("Sun"):
                polskaNazwa = "Nd.";
                break;
        }
        return polskaNazwa;
    }
}
